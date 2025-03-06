package org.jmqtt.broker.processor.dispatcher;

import org.jmqtt.broker.common.model.SubscriptionRetainMessage;

/**
 * 本节点服务器向设备分发保留消息
 */
public interface RetainMessageDispatcher {

    void start();

    void shutdown();

    boolean appendMessage(SubscriptionRetainMessage message);

}
