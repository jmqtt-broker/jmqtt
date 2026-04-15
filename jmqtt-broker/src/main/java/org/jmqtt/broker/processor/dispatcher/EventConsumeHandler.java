package org.jmqtt.broker.processor.dispatcher;

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
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;
import org.slf4j.Logger;

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
        eventHandlerMap.put(EventCode.SESSION_STATE.getCode(), this::clientOnOffline);
        eventHandlerMap.put(EventCode.SUBSCRIPTION.getCode(), this::subscription);
        eventHandlerMap.put(EventCode.UNSUBSCRIPTION.getCode(), this::unsubscription);
        eventHandlerMap.put(EventCode.CLEAR_SESSION.getCode(), this::clearClientSession);
        eventHandlerMap.put(EventCode.DISPATCHER_CLIENT_MESSAGE.getCode(), this::dispatcherMessage);

        eventHandlerMap.put(EventCode.KICK_CONNECTION.getCode(), this::kickConnection);
        eventHandlerMap.put(EventCode.STORE_RETAIN_MSG.getCode(), this::storeRetain);
        eventHandlerMap.put(EventCode.CLEAR_RETAIN_MSG.getCode(), this::clearRetain);
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

    public void clientOnOffline(Event event) {
        SessionState session = (SessionState) event.getBody();
        if (!BrokerContext.getBrokerId().equals(session.getBrokerId())) {
            sessionStore.storeSession(session);
        }
        if (SessionState.StateEnum.ONLINE.equals(session.getState())) {
            // 如果是客户端上线，需要查看客户端是否存在离线消息，存在则返回

        }
    }

    private void dispatcherMessage(Event event) {
        if (ClusterHelper.lightning()) {
            this.innerMessageDispatcher.appendMessage((Message) event.getBody());
        } else {
            SourceWapper wapper = (SourceWapper) event.getBody();
            if (BrokerContext.getBrokerId().equals(wapper.getTargetBrokerId())) {
                this.innerMessageDispatcher.appendMessage((Message) wapper.getSource());
            }
        }
    }

    public void subscription(Event event) {
        Subscription subscription = (Subscription) event.getBody();
        if (!BrokerContext.getBrokerId().equals(subscription.getBrokerId())) {
            sessionStore.storeSubscription(subscription.getClientId(), subscription);
            BrokerContext.getSubscriptionMatcher().subscribe(subscription);
        }
    }

    public void unsubscription(Event event) {
        Subscription subscription = (Subscription) event.getBody();
        if (!BrokerContext.getBrokerId().equals(subscription.getBrokerId())) {
            String topic = subscription.getTopic();
            String clientId = subscription.getClientId();
            sessionStore.delSubscription(clientId, topic);
            BrokerContext.getSubscriptionMatcher().unSubscribe(topic, clientId);
        }
    }

    private void clearClientSession(Event event) {
        // if event from current node, ignore the event
        if (BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            LogUtil.debug(log, "Event from current node,ignore the event,fromBroker:{}", event.getFromBroker());
            return;
        }
        SessionConnect sc = (SessionConnect) event.getBody();
        String clientId = sc.getClientId();
        if (!ConnectManager.getInstance().containClient(clientId)) {
            TimerUtils.sessionTimeoutImmediately(clientId);
            TimerUtils.stopWillTimeout(clientId);
            return;
        }
        ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
        Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x8E, "Session taken over.");
        sessionStore.clearSession(clientId, sc.getCleanStart());
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

    private void storeRetain(Event event) {
        Object body = event.getBody();
        if (body instanceof Message) {
            Message message = (Message) body;
            String topic = (String) message.getHeader(MessageHeader.TOPIC);
            messageStore.storeRetainMessage(topic, message);
        }
    }

    private void clearRetain(Event event) {
        Object body = event.getBody();
        if (body instanceof String) {
            String topic = (String) body;
            messageStore.clearRetainMessage(topic);
        }
    }

}
