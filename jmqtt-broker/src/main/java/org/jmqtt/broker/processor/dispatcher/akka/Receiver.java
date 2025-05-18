package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.ActorSelection;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.store.highperformance.OutflowMessageHandler;
import org.jmqtt.broker.store.local.LocalStore;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class Receiver extends AbstractBehavior<Letter> {

    private Map<Integer, Consumer<Letter>> responseHandlerMap = new ConcurrentHashMap<>();

    public Receiver(ActorContext<Letter> context) {
        super(context);
        responseHandlerMap.put(EventCode.BROKER_STATE.getCode(), this::brokerStatus);
        responseHandlerMap.put(EventCode.BROKER_STATE_REQUEST.getCode(), this::reportBrokerInfo);
        responseHandlerMap.put(EventCode.SESSION_STATE_RESPONSE.getCode(), this::onlineResponse);
        responseHandlerMap.put(EventCode.SUBSCRIPTION_RESPONSE.getCode(), this::subscriptionResponse);
        responseHandlerMap.put(EventCode.DISPATCHER_SHARE_SUBSCRIPTION_MSG.getCode(), this::dispatcherShareSubscriptionMsg);
        responseHandlerMap.put(EventCode.KICK_CONNECTION.getCode(), this::kickConnection);
    }

    public static Behavior<Letter> create() {
        return Behaviors.setup(Receiver::new);
    }

    @Override
    public Receive<Letter> createReceive() {
        return newReceiveBuilder()
                .onMessage(Letter.class, letter -> {
                    Event event = letter.getMessage();
                    Consumer<Letter> eventHandler = responseHandlerMap.get(event.getEventCode());
                    if (eventHandler != null) {
                        eventHandler.accept(letter);
                    } else {
                        log.warn("[responseHandler] consume event is not supported,event:{}", event);
                    }
                    return this;
                }).build();
    }

    private void reportBrokerInfo(Letter letter) {
        LocalStore localStore = BrokerContext.getLocalStore();
        BrokerDO brokerDO = localStore.getBroker(BrokerContext.getBrokerId());
        BrokerInfo brokerInfo = new BrokerInfo();
        if (brokerDO != null) {
            MixAll.copyProperties(brokerDO, brokerInfo);
            Boolean status = brokerInfo.getStatus();
            if (status != null && !status) {
                brokerDO.setStatus(true);
                brokerDO.setOnlineAt(System.currentTimeMillis());
                localStore.storeBroker(brokerDO);
            }
        } else {
            BrokerController controller = BrokerContext.getBrokerController();
            NettyConfig config = controller.getNettyConfig();
            brokerInfo.setBrokerId(BrokerContext.getBrokerId());
            brokerInfo.setIp(controller.getCurrentIp());
            brokerInfo.setTcpPort(config.getTcpPort());
            brokerInfo.setTcpPortSsl(config.getSslTcpPort());
            brokerInfo.setWsPort(config.getWebsocketPort());
            brokerInfo.setWsPortSsl(config.getSslWebsocketPort());
            brokerInfo.setStatus(true);
            brokerInfo.setOnlineAt(System.currentTimeMillis());
            localStore.storeBroker(new BrokerDO(brokerInfo));
        }
        String responsePath = letter.getResponsePath();
        if (StringUtils.isNotBlank(responsePath)) {
            response(new Letter(ClusterHelper.getEvent(EventCode.BROKER_STATE, brokerInfo)), responsePath);
        }
    }

    private void brokerStatus(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        log.info("receive broker status, {}", body);
        if (body instanceof BrokerInfo) {
            BrokerInfo brokerInfo = (BrokerInfo) body;
            BrokerDO broker = new BrokerDO(brokerInfo);
            BrokerContext.getLocalStore().storeBroker(broker);
        }
    }

    private void subscriptionResponse(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof SubscriptionRetainMessage) {
            SubscriptionRetainMessage sm = (SubscriptionRetainMessage) body;
            Subscription subscription = sm.getSubscription();
            String clientId = subscription.getClientId();
            if (!ClusterHelper.isKeeper()) {
                BrokerContext.getSessionStore().storeSubscription(clientId, subscription);
                BrokerContext.getSubscriptionMatcher().subscribe(subscription);
            }
            BrokerContext.getRetainMessageDispatcher().appendMessage(sm);
        }
    }

    private void onlineResponse(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof OfflineMessageResponse) {
            OfflineMessageResponse res = (OfflineMessageResponse) body;
            String clientId = res.getClientId();
            Collection<Message> offlineList = res.getOfflineList();
            ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
            if (clientSession != null) {
                offlineList.forEach(msg -> {
                    MqttMessage mqttMessage = MessageUtil.getPubMessage(msg, false);
                    clientSession.getCtx().writeAndFlush(mqttMessage);
                });
            } else {
                log.warn("The client offline again, put the message to the offline queue,clientId:{}", clientId);
            }

        }
    }

    private void dispatcherShareSubscriptionMsg(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof ShareSubscriptionMsg) {
            ShareSubscriptionMsg msg = (ShareSubscriptionMsg) body;
            List<Subscription> subscriptions = msg.getSubscriptions();
            Message message = msg.getMessage();
            subscriptions.forEach(subscription -> {
                String clientId = subscription.getClientId();
                ClientSession client = ConnectManager.getInstance().getClient(clientId);
                if (client != null) {
                    int minQos = MessageUtil.getMinQos((int) message.getHeader(MessageHeader.QOS), subscription.getQos());
                    int messageId = client.generateMessageId();
                    message.putHeader(MessageHeader.QOS, minQos);
                    message.setMsgId(messageId);
                    if (minQos > 0) {
                        OutflowMessageHandler.cacheOutflowMsg(clientId, message);
                    }
                    MqttPublishMessage publishMessage = MessageUtil.getPubMessage(message, false, subscription.getOption(), clientId);
                    client.getCtx().writeAndFlush(publishMessage);
                } else {
                    log.warn("share subscription msg dispatcher faild, subscription: {}, message: {}", subscription, message);
                }
            });
        }
    }

    private void kickConnection(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof String) {
            String clientId = (String) body;
            Optional.ofNullable(ConnectManager.getInstance().getClient(clientId)).ifPresent(s -> {
                Mqtt5Utils.sendDisconnectAndClose(s, (byte) 0x8B);
            });
        }
    }

    private void response(Letter res, String responsePath) {
        log.debug("responsePath: {}", responsePath);
        ActorSelection selection = Adapter.toClassic(getContext().getSystem())
                .actorSelection(responsePath);
        selection.tell(res, Adapter.toClassic(getContext().getSelf()));
    }
}
