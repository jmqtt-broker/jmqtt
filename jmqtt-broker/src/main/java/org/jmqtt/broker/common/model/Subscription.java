package org.jmqtt.broker.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * 订阅关系
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"clientId", "topic"})
public class Subscription {
    private String clientId;
    private int qos;
    private String topic;
    private SubscriptionOption option;

    public Subscription(String clientId,String topic,int qos){
        this.clientId = clientId;
        this.topic = topic;
        this.qos = qos;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Subscription.class.getSimpleName() + "[", "]")
            .add("clientId='" + clientId + "'")
            .add("qos=" + qos)
            .add("topic='" + topic + "'")
            .toString();
    }
}
