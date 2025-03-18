package org.jmqtt.broker.processor.dispatcher.akka;

import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.List;

/**
 * 集群相关操作
 */
public class ClusterHelper {

    private static Boolean KEEPER;

    private static Boolean LIGHTNING;

    private static final BrokerConfig BROKER_CONFIG;

    static {
        BROKER_CONFIG = BrokerContext.getBrokerConfig();
        AkkaConfig akka = BrokerContext.getBrokerConfig().getAkka();
        if (akka != null && akka.getEnable()) {
            List<String> roles = akka.getRoles();
            KEEPER = roles != null && roles.contains(AkkaConst.KEEPER);
            LIGHTNING = JmqttConst.MEM.equals(BROKER_CONFIG.getStore());
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
            BrokerContext.getBrokerController().getClusterEventHandler().sendToKeeper(event);
        }
        return LIGHTNING;
    }

    public static boolean sendToOneKeeper(Event event) {
        if (LIGHTNING) {
            BrokerContext.getBrokerController().getClusterEventHandler().sendToOneKeeper(event);
        }
        return LIGHTNING;
    }

    public static void syncToKeeper(Letter letter) {
        letter.setResponsePath(null);
        letter.setSync(true);
        letter.getMessage().setFromBroker(BrokerContext.getBrokerId());
        BrokerContext.getBrokerController().getClusterEventHandler().syncToKeeper(letter);
    }

    public static void sendToCluster(Event event) {
        BrokerContext.getBrokerController().getClusterEventHandler().sendEvent(event);
    }

    public static boolean reportSessionToKeeper(SessionState sessionState) {
        return sendToOneKeeper(getEvent(EventCode.SESSION_STATE, sessionState));
    }

    public static boolean reportUnsubscriptionToKeeper(Subscription subscription) {
        return sendToKeeper(getEvent(EventCode.UNSUBSCRIPTION, subscription));
    }

    public static boolean reportSubscriptionToKeeper(Subscription subscription) {
        return sendToOneKeeper(getEvent(EventCode.SUBSCRIPTION, subscription));
    }

}
