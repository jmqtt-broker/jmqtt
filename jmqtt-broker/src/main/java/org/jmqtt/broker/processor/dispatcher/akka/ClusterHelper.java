package org.jmqtt.broker.processor.dispatcher.akka;

import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.SourceWapper;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

/**
 * 集群相关操作
 */
public class ClusterHelper {

    /**
     * 内存模式下，并且开启了akka，为高性能集群模式
     */
    private static final Boolean LIGHTNING;

    private static final BrokerConfig BROKER_CONFIG;

    private static final ClusterEventHandler EVENT_HANDLER;

    static {
        BROKER_CONFIG = BrokerContext.getBrokerConfig();
        AkkaConfig akka = BrokerContext.getBrokerConfig().getAkka();
        if (akka != null && akka.getEnable()) {
            LIGHTNING = JmqttConst.MEM.equals(BROKER_CONFIG.getStore());
        } else {
            LIGHTNING = false;
        }
        EVENT_HANDLER = BrokerContext.getBrokerController().getClusterEventHandler();
    }

    public static boolean lightning() {
        return LIGHTNING;
    }

    public static Event getEvent(EventCode code, Object body) {
        return new Event(code.getCode(), body, System.currentTimeMillis(), BrokerContext.getBrokerId());
    }

    public static void sendToCluster(Event event) {
        EVENT_HANDLER.sendEvent(event);
    }

    public static void sendToCluster(Event event, String targetBrokerId) {
        if (LIGHTNING) {
            ((AkkaClusterEventHandler) EVENT_HANDLER).sendByPath(event, targetBrokerId);
        } else {
            event.setBody(new SourceWapper(event.getBody(), targetBrokerId));
            EVENT_HANDLER.sendEvent(event);
        }
    }

    public static void sendByPath(Event event, String path) {
        if (LIGHTNING) {
            ((AkkaClusterEventHandler) EVENT_HANDLER).sendByPath(event, path);
        }
    }

    public static boolean sendSessionToCluster(SessionState sessionState) {
        return EVENT_HANDLER.sendEvent(getEvent(EventCode.SESSION_STATE, sessionState));
    }

    public static void sendMessageToCluster(Message message, String targetBrokerId) {
        Event event = ClusterHelper.getEvent(EventCode.DISPATCHER_CLIENT_MESSAGE, message);
        sendToCluster(event, targetBrokerId);
    }

    public static void subscribeToCluster(Subscription subscription) {
        EVENT_HANDLER.sendEvent(getEvent(EventCode.SUBSCRIPTION, subscription));
    }

    public static void unSubscribeToCluster(Subscription subscription) {
        EVENT_HANDLER.sendEvent(getEvent(EventCode.UNSUBSCRIPTION, subscription));
    }

    public static void storeRetainMessage(Message message) {
        EVENT_HANDLER.sendEvent(ClusterHelper.getEvent(EventCode.STORE_RETAIN_MSG, message));
    }

    public static void clearRetainMessage(String topic) {
        EVENT_HANDLER.sendEvent(ClusterHelper.getEvent(EventCode.CLEAR_RETAIN_MSG, topic));
    }

}
