package org.jmqtt.broker.common.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Topic {

    private String topicName;
    private int qos;

    private SubscriptionOption option;

    public Topic(String topicName, int qos) {
        this.topicName = topicName;
        this.qos = qos;
    }

}
