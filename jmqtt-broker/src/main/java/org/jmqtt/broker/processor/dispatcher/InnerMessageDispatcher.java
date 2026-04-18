package org.jmqtt.broker.processor.dispatcher;

import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;

/**
 * 本节点服务器向设备分发消息
 */
public interface InnerMessageDispatcher {

    void start();

    void shutdown();

    boolean appendMessage(Message message);

    void send(Subscription subscription, Message message);

}
