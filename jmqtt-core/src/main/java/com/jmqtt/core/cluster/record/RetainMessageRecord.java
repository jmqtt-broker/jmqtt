package com.jmqtt.core.cluster.record;

import com.jmqtt.core.mqtt.model.Message;

public record RetainMessageRecord(String topic, Message message) {

    public static RetainMessageRecord of(String topic, Message message) {
        return new RetainMessageRecord(topic, message);
    }

}
