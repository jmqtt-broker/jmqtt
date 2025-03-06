package org.jmqtt.broker.processor.dispatcher.akka;

import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.OfflineMessageDTO;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.List;

/**
 * 集群相关操作
 */
public class ClusterHelper {

    public static boolean isKeeper() {
        AkkaConfig akka = BrokerContext.getBrokerConfig().getAkka();
        if (akka != null && akka.getEnable()) {
            List<String> roles = akka.getRoles();
            return roles != null && roles.contains(AkkaConst.KEEPER);
        }
        return false;
    }

    public static boolean lightning() {
        BrokerConfig brokerConfig = BrokerContext.getBrokerConfig();
        AkkaConfig akka = brokerConfig.getAkka();
        return JmqttConst.MEM.equals(brokerConfig.getStore()) && akka != null && akka.getEnable();
    }

    public static Event getEvent(EventCode code, Object body) {
        return new Event(code.getCode(), body, System.currentTimeMillis(), BrokerContext.getBrokerId());
    }

    public static boolean sendToKeeper(Event event) {
        boolean send = lightning();
        if (send) {
            BrokerContext.getBrokerController().getClusterEventHandler().sendTokeeper(event);
        }
        return send;
    }

    public static void sendToCluster(Event event) {
        BrokerContext.getBrokerController().getClusterEventHandler().sendEvent(event);
    }

    public static boolean reportSessionToKeeper(SessionState sessionState) {
        return sendToKeeper(getEvent(EventCode.SESSION_STATE, sessionState));
    }

    public static boolean reportUnsubscriptionToKeeper(Subscription subscription) {
        return sendToKeeper(getEvent(EventCode.UNSUBSCRIPTION, subscription));
    }

    public static boolean reportSubscriptionToKeeper(Subscription subscription) {
        return sendToKeeper(getEvent(EventCode.SUBSCRIPTION, subscription));
    }

    public static boolean reportOfflineMessageToKeeper(String subClientId, Message message) {
        return sendToKeeper(getEvent(EventCode.STORE_OFFLINE_MSG, new OfflineMessageDTO(subClientId, message)));
    }

}
