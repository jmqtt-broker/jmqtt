package org.jmqtt.broker.common.helper;

import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.store.local.LocalStore;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.common.event.Event;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;

import java.util.concurrent.atomic.AtomicBoolean;

public class BrokerContext {

    private static BrokerController brokerController;

    private static AtomicBoolean READY = new AtomicBoolean(false);

    public static BrokerController getBrokerController() {
        return brokerController;
    }

    public static void setBrokerController(BrokerController brokerController) {
        if (READY.compareAndSet(false, true)) {
            BrokerContext.brokerController = brokerController;
        }
    }

    public static String getBrokerId() {
        BrokerController ctl = getBrokerController();
        AkkaConfig akka = getBrokerConfig().getAkka();
        if (akka != null && akka.getEnable()) {
            return JmqttConst.PROJECT + "@" + ctl.getCurrentIp() + ":" + akka.getPort();
        }
        return JmqttConst.PROJECT + "@" + ctl.getCurrentIp() + ":" + ctl.getNettyConfig().getTcpPort();
    }

    /**
     * 非内存存储使用的是rdb或redis中央存储，每个节点访问的是同一份数据，
     * 内存存储条件下，每个节点访问的是本地存储数据
     * @return boolean
     */
    public static boolean centerStore() {
        return !JmqttConst.MEM.equals(getBrokerConfig().getStore());
    }

    public static SessionStore getSessionStore() {
        return brokerController.getSessionStore();
    }

    public static MessageStore getMessageStore() {
        return brokerController.getMessageStore();
    }

    public static LocalStore getLocalStore() {
        return brokerController.getLocalStore();
    }

    public static BrokerConfig getBrokerConfig() {
        return brokerController.getBrokerConfig();
    }

    public static InnerMessageDispatcher getMessageDispatcher() {
        return brokerController.getInnerMessageDispatcher();
    }

    public static SubscriptionMatcher getSubscriptionMatcher() {
        return brokerController.getSubscriptionMatcher();
    }

    public static void sendEvent(Event event) {
        brokerController.getClusterEventHandler().sendEvent(event);
    }

}
