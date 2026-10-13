
package com.jmqtt.core.mqtt.store;

import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * 设备消息处理：
 * will消息（遗嘱消息）
 * retain消息（保留消息）
 */
public interface MessageStore {

    /**
     * 存储clientId的遗嘱消息
     *
     * @param clientId clientId
     * @param message  message
     * @return return
     */
    default boolean storeWillMessage(String clientId, Message message) {
        return false;
    }

    /**
     * 清理该clientId的遗嘱消息
     *
     * @param clientId clientId
     * @return return
     */
    default boolean clearWillMessage(String clientId) {
        return false;
    }

    default void clearWillAndWillRetain(String clientId) {
        Message willMessage = getWillMessage(clientId);
        if (willMessage != null) {
            clearWillMessage(clientId);
            Optional.ofNullable(willMessage.getHeader(MessageHeader.RETAIN)).ifPresent(retain -> {
                if ((boolean) retain) {
                    // ClusterHelper.clearRetainMessage((String) willMessage.getHeader(MessageHeader.TOPIC));
                }
            });
        }
    }

    /**
     * 获取will消息
     *
     * @param clientId clientId
     * @return return
     */
    default Message getWillMessage(String clientId) {
        return null;
    }

    /**
     * 存储retain消息
     *
     * @param topic   topic
     * @param message message
     * @return return
     */
    default boolean storeRetainMessage(String topic, Message message) {
        return false;
    }

    /**
     * 清理该topic的 retain消息
     *
     * @param topic topic
     * @return return
     */
    default boolean clearRetainMessage(String topic) {
        return false;
    }

    /**
     * 获取所有retain消息
     *
     * @return return
     */
    default Collection<Message> getAllRetainMsg() {
        return new ArrayList<>();
    }

    /**
     * 根据订阅topic查询符合订阅的保留消息
     *
     * @return return
     */
    default Collection<Message> getRetainMsg(String topic) {
        return new ArrayList<>();
    }

    default void retainHandle(Message message) {
        boolean retain = (boolean) message.getHeader(MessageHeader.RETAIN);
        if (retain) {
            Thread.startVirtualThread(() -> {
                int qos = (int) message.getHeader(MessageHeader.QOS);
                byte[] payload = message.getPayload();
                String topic = (String) message.getHeader(MessageHeader.TOPIC);
                if (qos == 0 || payload == null || payload.length == 0) {
                    // ClusterHelper.clearRetainMessage(topic);
                } else {
                    // ClusterHelper.storeRetainMessage(message);
                    storeRetainMessage(topic, message);
                }
            });
        }
    }

}
