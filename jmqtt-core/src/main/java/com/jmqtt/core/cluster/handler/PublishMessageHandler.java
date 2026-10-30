package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.JmqttContext;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.cluster.record.ClusterMessageRecord;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.Subscription;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class PublishMessageHandler implements ClusterMessageHandler<ClusterMessageRecord> {

    @Override
    public ClusterMessage.EventType getType() {
        return ClusterMessage.EventType.MESSAGE_DISPATCH;
    }

    @Override
    public void handle(ClusterMessage<ClusterMessageRecord> msg) {
        ClusterMessageRecord record = msg.getPayload();
        Message message = record.message();
        Subscription subscription = record.subscription();
        JmqttContext.getMessageDispatcher().send(subscription, message);
    }

    public static PublishMessageHandler of() {
        return new PublishMessageHandler();
    }

}
