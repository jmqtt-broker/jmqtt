package org.jmqtt.broker.common.model;

import lombok.*;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * 订阅关系
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"clientId", "topic"})
public class Subscription {
    private String brokerId;
    private String clientId;
    private int qos;
    private String topic;
    private Boolean subRes = true;
    private SubscriptionOption option;

    public Subscription(String clientId,String topic,int qos){
        this.clientId = clientId;
        this.topic = topic;
        this.qos = qos;
    }

    public Subscription(String brokerId, String clientId,String topic,int qos, Boolean subRes){
        this.brokerId = brokerId;
        this.clientId = clientId;
        this.topic = topic;
        this.qos = qos;
        this.subRes = subRes;
    }

}
