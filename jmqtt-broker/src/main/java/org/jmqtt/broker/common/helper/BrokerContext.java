package org.jmqtt.broker.common.helper;

import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;

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
}
