package org.jmqtt.broker.processor.dispatcher;

import io.netty.handler.codec.mqtt.MqttMessage;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.helper.TimerUtils;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.processor.dispatcher.akka.ClusterHelper;
import org.jmqtt.broker.processor.dispatcher.event.EventHandler;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集群事件处理
 */
public class EventConsumeHandler {

    private static final Logger log = JmqttLogger.eventLog;

    private InnerMessageDispatcher innerMessageDispatcher;
    private ClusterEventHandler clusterEventHandler;
    private AtomicBoolean pollStoped = new AtomicBoolean(false);
    private int maxPollNum;
    private int pollWaitInterval;
    private SessionStore sessionStore;
    private MessageStore messageStore;
    private Map<Integer, EventHandler> eventHandlerMap = new ConcurrentHashMap<>();

    public EventConsumeHandler(BrokerController brokerController) {
        this.innerMessageDispatcher = brokerController.getInnerMessageDispatcher();
        this.clusterEventHandler = brokerController.getClusterEventHandler();
        clusterEventHandler.setEventConsumeHandler(this);
        this.maxPollNum = brokerController.getBrokerConfig().getMaxPollEventNum();
        this.pollWaitInterval = brokerController.getBrokerConfig().getPollWaitInterval();
        this.sessionStore = brokerController.getSessionStore();
        this.messageStore = brokerController.getMessageStore();

        eventHandlerMap.put(EventCode.BROKER_ONLINE.getCode(), this::brokerOnline);
        eventHandlerMap.put(EventCode.SUBSCRIPTION.getCode(), this::subscription);
        eventHandlerMap.put(EventCode.SUBSCRIPTION_SYNC.getCode(), this::subscriptionSync);
        eventHandlerMap.put(EventCode.UNSUBSCRIPTION.getCode(), this::unsubscription);
        eventHandlerMap.put(EventCode.SEND_RETAIN_MSG.getCode(), this::sendRetainMessage);
        eventHandlerMap.put(EventCode.SEND_OFFLINE_MSG.getCode(), this::sendOfflineMessage);
        eventHandlerMap.put(EventCode.SESSION_STATE.getCode(), this::clientOnOffline);
        eventHandlerMap.put(EventCode.DISPATCHER_CLIENT_MESSAGE.getCode(), this::dispatcherMessage);

        eventHandlerMap.put(EventCode.KICK_CONNECTION.getCode(), this::kickConnection);
    }

    // 集群方式1: consume event from cluster
    public void consumeEvent(Event event) {
        EventHandler eventHandler = eventHandlerMap.get(event.getEventCode());
        if (eventHandler != null) {
            eventHandler.handle(event);
        } else {
            LogUtil.warn(log, "[EventConsumeHandler] consume event is not supported,event:{}", event);
        }
    }

    // 集群方式2: poll event from cluster
    private List<Event> pollEvent() {
        return clusterEventHandler.pollEvent(maxPollNum);
    }

    public void start() {
        new Thread(() -> {
            while (!pollStoped.get()) {
                try {
                    List<Event> eventList = pollEvent();
                    if (!MixAll.isEmpty(eventList)) {
                        for (Event event : eventList) {
                            consumeEvent(event);
                        }
                    }
                    if (MixAll.isEmpty(eventList) || eventList.size() < 5) {
                        Thread.sleep(pollWaitInterval);
                    }
                } catch (Exception e) {
                    LogUtil.warn(log, "Poll event from cluster error.", e);
                }
            }
        }).start();
    }

    public void shutdown() {

    }

    private void brokerOnline(Event event) {
        BrokerInfo brokerInfo = (BrokerInfo) event.getBody();
        BrokerDO broker = new BrokerDO(brokerInfo);
        BrokerContext.getLocalStore().storeBroker(broker);
    }

    private void dispatcherMessage(Event event) {
        if (ClusterHelper.lightning()) {
            MessageDispatchDTO dispatch = (MessageDispatchDTO) event.getBody();
            this.innerMessageDispatcher.send(dispatch.getSubscription(), dispatch.getMessage());
        } else {
            SourceWapper wapper = (SourceWapper) event.getBody();
            if (BrokerContext.getBrokerId().equals(wapper.getTargetBrokerId())) {
                this.innerMessageDispatcher.appendMessage((Message) wapper.getSource());
            }
        }
    }

    public void subscription(Event event) {
        Subscription subscription = (Subscription) event.getBody();
        if (!BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            sessionStore.storeSubscription(subscription.getClientId(), subscription);
            boolean subRes = BrokerContext.getSubscriptionMatcher().subscribe(subscription);
            if (ClusterHelper.isEarliestKeeper()) {
                Collection<Message> retainMsgs = messageStore.getRetainMsg(subscription.getTopic());
                if (!retainMsgs.isEmpty()) {
                    // TODO 待优化
                    SubscriptionRetainMessage message = new SubscriptionRetainMessage(retainMsgs, subscription, subRes);
                    ClusterHelper.sendEvent(ClusterHelper.getEvent(EventCode.SEND_RETAIN_MSG, message), event.getFromBroker());
                }
            }
        }
    }

    public void subscriptionSync(Event event) {
        SubscriptionSyncDTO sync = (SubscriptionSyncDTO) event.getBody();
        if (!BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            sync.getSubscriptions().forEach(subscription -> {
                sessionStore.storeSubscription(subscription.getClientId(), subscription);
                BrokerContext.getSubscriptionMatcher().subscribe(subscription);
            });
        }
    }

    public void sendRetainMessage(Event event) {
        SubscriptionRetainMessage message = (SubscriptionRetainMessage) event.getBody();
        BrokerContext.getRetainMessageDispatcher().appendMessage(message);
    }

    public void unsubscription(Event event) {
        Subscription subscription = (Subscription) event.getBody();
        if (!BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            String topic = subscription.getTopic();
            String clientId = subscription.getClientId();
            sessionStore.delSubscription(clientId, topic);
            BrokerContext.getSubscriptionMatcher().unSubscribe(topic, clientId);
        }
    }

    private void clientOnOffline(Event event) {
        if (!BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            SessionState session = (SessionState) event.getBody();
            BrokerContext.getSessionStore().storeSession(session);
            String clientId = session.getClientId();
            if (SessionState.StateEnum.ONLINE.equals(session.getState())) {
                // 如果是客户端上线，查看本地有无在线的client，如果有则踢掉连接
                ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
                if (clientSession != null) {
                    Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x8E, "Session taken over.");
                    sessionStore.clearSession(clientId, true);
                } else {
                    // 需要查看客户端是否存在离线消息，存在则返回
                    if (ClusterHelper.isEarliestKeeper()) {
                        Collection<Message> allOfflineMsg = BrokerContext.getSessionStore().getAllOfflineMsg(clientId);
                        if (!allOfflineMsg.isEmpty()) {
                            // TODO 待优化
                            ClusterHelper.sendEvent(ClusterHelper.getEvent(EventCode.SEND_OFFLINE_MSG,
                                    new OfflineMessageList(clientId, allOfflineMsg)), event.getFromBroker());
                            BrokerContext.getSessionStore().clearOfflineMsg(clientId);
                        }
                    }
                    TimerUtils.sessionTimeoutImmediately(clientId);
                    TimerUtils.stopWillTimeout(clientId);
                }
            } else {
                BrokerContext.getSessionStore().clearSession(session.getClientId(), session.getCleanStart());
            }
        }
    }

    public void sendOfflineMessage(Event event) {
        OfflineMessageList offlineMsg = (OfflineMessageList) event.getBody();
        String clientId = offlineMsg.getClientId();
        ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
        if (clientSession != null) {
            offlineMsg.getOfflineList().forEach(msg -> {
                MqttMessage mqttMessage = MessageUtil.getPubMessage(msg, false);
                clientSession.getCtx().writeAndFlush(mqttMessage);
            });
        }
    }

    private void kickConnection(Event event) {
        Object body = event.getBody();
        if (body instanceof String) {
            String clientId = (String) body;
            Optional.ofNullable(ConnectManager.getInstance().getClient(clientId)).ifPresent(s -> {
                Mqtt5Utils.sendDisconnectAndClose(s, (byte) 0x8B, "Server terminated connection.");
            });
        }
    }

}
