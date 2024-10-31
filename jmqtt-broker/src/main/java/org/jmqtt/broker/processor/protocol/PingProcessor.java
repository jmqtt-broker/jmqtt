package org.jmqtt.broker.processor.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.slf4j.Logger;

/**
 * 心跳
 */
public class PingProcessor implements RequestProcessor {
    private static final Logger log = JmqttLogger.clientTraceLog;

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttMessage pingRespMessage = MessageUtil.getPingRespMessage();
        log.debug("[HEART_BEAT]->{{}}心跳响应消息", NettyUtil.getClientId(ctx.channel()));
        ctx.writeAndFlush(pingRespMessage);
    }
}
