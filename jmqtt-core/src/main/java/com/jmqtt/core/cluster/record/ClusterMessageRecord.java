package com.jmqtt.core.cluster.record;

import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.Subscription;

public record ClusterMessageRecord(Message message, Subscription subscription) {

    public static ClusterMessageRecord of(Message message, Subscription subscription) {
        return new ClusterMessageRecord(message, subscription);
    }

}
