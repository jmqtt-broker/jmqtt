package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.netty.helper.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 出栈消息接收到的客户端 pubAck报文：释放缓存的出栈消息
 */
@Slf4j
public class PubAckProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String clientId = NettyUtil.getClientId(ctx.channel());
        int messageId = MessageUtil.getMessageId(mqttMessage);
        log.info("[PubAck] -> Recieve PubAck message,clientId={},msgId={}", clientId, messageId);
        // releaseOutflowMsg(clientId, messageId);
    }
}
