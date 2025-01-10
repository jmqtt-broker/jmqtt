
package org.jmqtt.broker.store;

import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;

import java.util.Collection;
import java.util.Optional;

/**
 * 设备消息处理：
 * will消息（遗嘱消息）
 * retain消息（保留消息）
 */
public interface MessageStore {

    void start(BrokerConfig brokerConfig);

    void shutdown();

    /**
     * 存储clientId的遗嘱消息
     */
    boolean storeWillMessage(String clientId, Message message);

    /**
     * 清理该clientId的遗嘱消息
     */
    boolean clearWillMessage(String clientId);

    default void clearWillAndWillRetain(String clientId) {
        Message willMessage = getWillMessage(clientId);
        if (willMessage != null) {
            clearWillMessage(clientId);
            Optional.ofNullable(willMessage.getHeader(MessageHeader.RETAIN)).ifPresent(retain -> {
                if ((boolean) retain) {
                    clearRetainMessage((String) willMessage.getHeader(MessageHeader.TOPIC));
                }
            });
        }
    }

    /**
     * 获取will消息
     */
    Message getWillMessage(String clientId);

    /**
     * 存储retain消息
     */
    boolean storeRetainMessage(String topic,Message message);

    /**
     * 清理该topic的 retain消息
     */
    boolean clearRetainMessage(String topic);

    /**
     * 获取所有retain消息
     */
    Collection<Message> getAllRetainMsg();
}
