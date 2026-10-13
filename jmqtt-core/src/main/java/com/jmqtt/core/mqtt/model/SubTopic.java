package com.jmqtt.core.mqtt.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubTopic {

    private String topicName;
    private int qos;

    private SubscriptionOption option;

    public SubTopic(String topicName, int qos) {
        this.topicName = topicName;
        this.qos = qos;
    }

}
