package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.mqtt.JmqttContext;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.cluster.record.RetainMessageRecord;
import com.jmqtt.core.mqtt.model.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class RetainMessageHandler implements ClusterMessageHandler<RetainMessageRecord> {

    @Override
    public ClusterMessage.EventType getType() {
        return ClusterMessage.EventType.RETAIN;
    }

    @Override
    public void handle(ClusterMessage<RetainMessageRecord> msg) {
        RetainMessageRecord record = msg.getPayload();
        String topic = record.topic();
        Message message = record.message();
        if (message != null) {
            JmqttContext.getMessageStore().storeRetainMessage(topic, message);
        } else {
            JmqttContext.getMessageStore().clearRetainMessage(topic);
        }
    }

    public static RetainMessageHandler of() {
        return new RetainMessageHandler();
    }

}
