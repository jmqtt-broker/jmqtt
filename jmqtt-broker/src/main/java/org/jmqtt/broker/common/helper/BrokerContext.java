package org.jmqtt.broker.common.helper;

import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2025/1/4 22:39
 */
public class BrokerContext {

    public static BrokerController brokerController;

    public static BrokerController getBrokerController() {
        return brokerController;
    }

    public static void setBrokerController(BrokerController brokerController) {
        BrokerContext.brokerController = brokerController;
    }

    public static SessionStore getSessionStore() {
        return brokerController.getSessionStore();
    }

    public static MessageStore getMessageStore() {
        return brokerController.getMessageStore();
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
}
