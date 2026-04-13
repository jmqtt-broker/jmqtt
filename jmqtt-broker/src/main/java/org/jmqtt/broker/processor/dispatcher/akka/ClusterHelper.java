package org.jmqtt.broker.processor.dispatcher.akka;

import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;
import scala.collection.immutable.SortedSet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群相关操作
 */
public class ClusterHelper {

    /**
     * 当前节点是否拥有keeper角色
     */
    private static final Boolean KEEPER;

    /**
     * 内存模式下，并且开启了akka，为高性能集群模式
     */
    private static final Boolean LIGHTNING;

    private static final BrokerConfig BROKER_CONFIG;

    private static AkkaClusterEventHandler AKKA_HANDLER;

    private static final Map<String, Member> MEMBER_MAP;

    static {
        BROKER_CONFIG = BrokerContext.getBrokerConfig();
        MEMBER_MAP = new ConcurrentHashMap<>();
        AkkaConfig akka = BrokerContext.getBrokerConfig().getAkka();
        if (akka != null && akka.getEnable()) {
            List<String> roles = akka.getRoles();
            KEEPER = roles != null && roles.contains(AkkaConst.KEEPER);
            LIGHTNING = JmqttConst.MEM.equals(BROKER_CONFIG.getStore());
            AKKA_HANDLER = (AkkaClusterEventHandler) BrokerContext.getBrokerController().getClusterEventHandler();
        } else {
            KEEPER = false;
            LIGHTNING = false;
        }
    }

    public static boolean isKeeper() {
        return KEEPER;
    }

    public static boolean lightning() {
        return LIGHTNING;
    }

    public static Event getEvent(EventCode code, Object body) {
        return new Event(code.getCode(), body, System.currentTimeMillis(), BrokerContext.getBrokerId());
    }

    public static boolean sendToKeeper(Event event) {
        if (LIGHTNING) {
            AKKA_HANDLER.sendToKeeper(event);
        }
        return LIGHTNING;
    }

    public static boolean sendToOneKeeper(Event event) {
        if (LIGHTNING) {
            AKKA_HANDLER.sendToOneKeeper(event);
        }
        return LIGHTNING;
    }

    public static void syncToKeeper(Letter letter) {
        letter.setResponsePath(null);
        letter.setSync(true);
        letter.getMessage().setFromBroker(BrokerContext.getBrokerId());
        AKKA_HANDLER.syncToKeeper(letter);
    }

    public static void sendToCluster(Event event) {
        AKKA_HANDLER.sendEvent(event);
    }

    public static void sendByPath(Letter letter, String path) {
        AKKA_HANDLER.sendByPath(letter, path);
    }

    public static void sendByPath(Event event, String path) {
        AKKA_HANDLER.sendByPath(event, path);
    }

    public static boolean reportSessionToKeeper(SessionState sessionState) {
        return sendToOneKeeper(getEvent(EventCode.SESSION_STATE, sessionState));
    }

    public static void subscribeToCluster(Subscription subscription) {
        AKKA_HANDLER.sendEvent(getEvent(EventCode.SUBSCRIPTION, subscription));
    }

    public static void unSubscribeToCluster(Subscription subscription) {
        AKKA_HANDLER.sendEvent(getEvent(EventCode.UNSUBSCRIPTION, subscription));
    }

    public static void storeRetainMessage(Message message) {
        AKKA_HANDLER.sendEvent(ClusterHelper.getEvent(EventCode.STORE_RETAIN_MSG, message));
    }

    public static void clearRetainMessage(String topic) {
        AKKA_HANDLER.sendEvent(ClusterHelper.getEvent(EventCode.CLEAR_RETAIN_MSG, topic));
    }

}
