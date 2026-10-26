package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.JmqttContext;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.cluster.record.ClusterMessageRecord;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
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
        String clientId = message.getClientId();
        ClientSession clientSession = ClientManager.getClient(message.getClientId());
        if (clientSession != null && clientSession.getCtx().channel().isWritable()) {
            MqttPublishMessage publishMessage = MessageUtil.getPubMessage(message, false, subscription.getOption(), subscription.getClientId());
            clientSession.getCtx().writeAndFlush(publishMessage);
        } else {
            log.warn("client session not found, clientId = {}", clientId);
        }
    }

    public static PublishMessageHandler of() {
        return new PublishMessageHandler();
    }

}
