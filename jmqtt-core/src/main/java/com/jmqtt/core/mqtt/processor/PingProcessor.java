package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.netty.helper.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳
 */
@Slf4j
public class PingProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttMessage pingRespMessage = MessageUtil.getPingRespMessage();
        log.debug("[HEART_BEAT]->{{}}心跳响应消息", NettyUtil.getClientId(ctx.channel()));
        ctx.writeAndFlush(pingRespMessage);
    }
}
