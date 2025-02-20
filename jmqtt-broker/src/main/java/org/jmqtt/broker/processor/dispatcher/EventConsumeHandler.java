package org.jmqtt.broker.processor.dispatcher;

import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.dispatcher.event.EventHandler;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
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
    private String currentIp;
    private SessionStore sessionStore;
    private Map<Integer, EventHandler> eventHandlerMap = new ConcurrentHashMap<>();

    public EventConsumeHandler(BrokerController brokerController) {
        this.innerMessageDispatcher = brokerController.getInnerMessageDispatcher();
        this.clusterEventHandler = brokerController.getClusterEventHandler();
        clusterEventHandler.setEventConsumeHandler(this);
        this.maxPollNum = brokerController.getBrokerConfig().getMaxPollEventNum();
        this.pollWaitInterval = brokerController.getBrokerConfig().getPollWaitInterval();
        this.currentIp = brokerController.getCurrentIp();
        this.sessionStore = brokerController.getSessionStore();
        eventHandlerMap.put(EventCode.CLEAR_SESSION.getCode(), this::clearClientSession);
        eventHandlerMap.put(EventCode.DISPATCHER_CLIENT_MESSAGE.getCode(), this::dispatcherMessage);
        eventHandlerMap.put(EventCode.DISPATCHER_WILL_MESSAGE.getCode(), this::dispatcherMessage);
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

    void dispatcherMessage(Event event) {
        this.innerMessageDispatcher.appendMessage((Message) event.getBody());
    }

    void clearClientSession(Event event) {
        // if event from current node, ignore the event
        if (BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            LogUtil.debug(log, "Event from current node,ignore the event,fromBroker:{}", event.getFromBroker());
            return;
        }
        String clientId = (String) event.getBody();
        if (!ConnectManager.getInstance().containClient(clientId)) {
            return;
        }
        ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
        Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x8E);
        sessionStore.clearSession(clientId, false);
    }

}
