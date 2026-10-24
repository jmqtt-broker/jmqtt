package com.jmqtt.core.cluster;

import com.jmqtt.core.JmqttContext;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterMessage<T> implements Serializable {

    private EventType eventType;

    private String brokerId;

    private T payload;

    public enum EventType {

        SESSION_CONNECT,

        SESSION_DISCONNECT,

        SUBSCRIBE_SYNC,

        UNSUBSCRIBE_SYNC,

        MESSAGE_DISPATCH,

        RETAIN_STORE,

        RETAIN_CLEAR,

    }

    public static <T> ClusterMessage<T> of(EventType type, String brokerId, T payload) {
        return ClusterMessage.<T>builder()
                .eventType(type)
                .brokerId(brokerId)
                .payload(payload)
                .build();
    }

    public static <T> ClusterMessage<T> of(EventType type, T payload) {
        return ClusterMessage.of(type, JmqttContext.currentNodeName(), payload);
    }

}
