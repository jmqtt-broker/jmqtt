package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.netty.helper.NettyUtil;
import com.jmqtt.core.netty.session.ClientSession;
import com.jmqtt.core.netty.session.ConnectManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 取消订阅
 */
@Slf4j
public class UnSubscribeProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) mqttMessage;
        MqttUnsubscribePayload unsubscribePayload = unsubscribeMessage.payload();
        List<String> topics = unsubscribePayload.topics();
        String clientId = NettyUtil.getClientId(ctx.channel());
        ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
        if (Objects.isNull(clientSession)) {
            log.warn("[UnSubscribe] -> The client is not online.clientId={}", clientId);
        }
        byte reasonCode = 0;
        for (String topic : topics) {
            // if (!subscriptionMatcher.unSubscribe(topic, clientId)) {
            //     // 取消订阅没有发现匹配的订阅
            //     reasonCode = 0x11;
            // }
            // Subscription subscription = sessionStore.getOneSubscription(clientId, topic);
            // if (subscription != null) {
            //     sessionStore.delSubscription(clientId, topic);
            //     ClusterHelper.unSubscribeToCluster(subscription);
            // }
        }
        MqttUnsubAckMessage unsubAckMessage = MessageUtil.getUnSubAckMessage(MessageUtil.getMessageId(mqttMessage), reasonCode);
        ctx.writeAndFlush(unsubAckMessage);
    }
}
