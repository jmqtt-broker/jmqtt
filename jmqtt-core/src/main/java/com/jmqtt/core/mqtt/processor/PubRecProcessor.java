package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.netty.helper.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * 出栈消息接收到qos2第一阶段的的pubRec报文: 丢弃消息，存储报文标志符
 */
@Slf4j
public class PubRecProcessor extends AbstractMessageProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String clientId = NettyUtil.getClientId(ctx.channel());
        int messageId = MessageUtil.getMessageId(mqttMessage);

        // releaseOutflowMsg(clientId,messageId);
        // cacheOutflowSecMsgId(clientId,messageId);

        log.debug("[PubRec] -> Receive PubRec message,clientId={},msgId={}",clientId,messageId);
        MqttMessage pubRelMessage = MessageUtil.getPubReplyMessage(messageId, MqttMessageType.PUBREL, (byte) 0x00, null, false);
        ctx.writeAndFlush(pubRelMessage);
    }
}
