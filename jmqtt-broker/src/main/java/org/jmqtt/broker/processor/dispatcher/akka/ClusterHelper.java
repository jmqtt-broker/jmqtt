package org.jmqtt.broker.processor.dispatcher.akka;

import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.Optional;

/**
 * 集群相关操作
 */
public class ClusterHelper {

    /**
     * 内存模式下，并且开启了akka，为高性能集群模式
     */
    private static final Boolean LIGHTNING;

    private static final Boolean KEEPER;

    private static final BrokerConfig BROKER_CONFIG;

    private static final ClusterEventHandler EVENT_HANDLER;

    static {
        BROKER_CONFIG = BrokerContext.getBrokerConfig();
        AkkaConfig akka = BrokerContext.getBrokerConfig().getAkka();
        if (akka != null && akka.getEnable()) {
            LIGHTNING = JmqttConst.MEM.equals(BROKER_CONFIG.getStore());
            KEEPER = Optional.ofNullable(akka.getRoles()).filter(roles ->
                    roles.contains(AkkaConst.KEEPER)).isPresent() && LIGHTNING;
        } else {
            LIGHTNING = false;
            KEEPER = false;
        }
        EVENT_HANDLER = BrokerContext.getBrokerController().getClusterEventHandler();
    }

    public static boolean lightning() {
        return LIGHTNING;
    }

    public static boolean isKeeper() {
        return KEEPER;
    }

    public static boolean isEarliestKeeper() {
        return lightning() && BrokerContext.getBrokerId().equals(((AkkaClusterEventHandler) EVENT_HANDLER).getEarliestKeeper());
    }

    public static Event getEvent(EventCode code, Object body) {
        return new Event(code.getCode(), body, System.currentTimeMillis(), BrokerContext.getBrokerId());
    }

    public static void sendToCluster(Event event) {
        EVENT_HANDLER.sendEvent(event);
    }

    public static void sendToCluster(Event event, String targetBrokerId) {
        if (LIGHTNING) {
            sendEvent(event, targetBrokerId);
        } else {
            event.setBody(new SourceWapper(event.getBody(), targetBrokerId));
            EVENT_HANDLER.sendEvent(event);
        }
    }

    public static void sendLetter(Letter letter, String address) {
        ((AkkaClusterEventHandler) EVENT_HANDLER).sendByPath(letter, address);
    }

    public static void sendEvent(Event event, String address) {
        ((AkkaClusterEventHandler) EVENT_HANDLER).sendByPath(event, address);
    }

    public static void sendSessionToCluster(SessionState sessionState) {
        /*if (lightning()) {
            EVENT_HANDLER.sendToKeeper(Letter.of(getEvent(EventCode.SESSION_STATE, sessionState)));
        }*/
        EVENT_HANDLER.sendEvent(getEvent(EventCode.SESSION_STATE, sessionState));
    }

    public static void sendMessageToCluster(MessageDispatchDTO dispatch, String targetBrokerId) {
        Event event = ClusterHelper.getEvent(EventCode.DISPATCHER_CLIENT_MESSAGE, dispatch);
        sendToCluster(event, targetBrokerId);
    }

    public static void subscribeToCluster(Subscription subscription) {
        if (lightning()) {
            EVENT_HANDLER.sendEvent(getEvent(EventCode.SUBSCRIPTION, subscription));
        }
    }

    public static void unSubscribeToCluster(Subscription subscription) {
        if (lightning()) {
            EVENT_HANDLER.sendEvent(getEvent(EventCode.UNSUBSCRIPTION, subscription));
        }
    }

    public static void storeRetainMessage(Message message) {
        if (lightning()) {
            EVENT_HANDLER.sendToKeeper(Letter.of(ClusterHelper.getEvent(EventCode.STORE_RETAIN_MSG, message)));
        } else {
            String topic = (String) message.getHeader(MessageHeader.TOPIC);
            BrokerContext.getMessageStore().storeRetainMessage(topic, message);
        }
    }

    public static void clearRetainMessage(String topic) {
        if (lightning()) {
            EVENT_HANDLER.sendToKeeper(Letter.of(ClusterHelper.getEvent(EventCode.CLEAR_RETAIN_MSG, topic)));
        } else {
            BrokerContext.getMessageStore().clearRetainMessage(topic);
        }
    }

    public static void storeOfflineMessage(String clientId, Message message) {
        if (lightning()) {
            EVENT_HANDLER.sendToKeeper(Letter.of(ClusterHelper.getEvent(
                    EventCode.STORE_OFFLINE_MSG, new OfflineMessageDTO(clientId, message))));
        } else {
            BrokerContext.getSessionStore().storeOfflineMsg(clientId, message);
        }
    }

    public static void clearOfflineMessage(String clientId) {
        if (lightning()) {
            EVENT_HANDLER.sendToKeeper(Letter.of(ClusterHelper.getEvent(
                    EventCode.CLEAR_OFFLINE_MSG, clientId)));
        } else {
            BrokerContext.getSessionStore().clearOfflineMsg(clientId);
        }
    }

}
