package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.netty.helper.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 出栈消息接收到的qos2消息的pubComp报文：清除缓存的出栈消息
 */
@Slf4j
public class PubCompProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String clientId = NettyUtil.getClientId(ctx.channel());
        int messageId = MessageUtil.getMessageId(mqttMessage);
        // boolean flag = releaseOutflowSecMsgId(clientId, messageId);
        // log.debug("[PubComp] -> Receive PubCom and remove the flow message,clientId={},msgId={}", clientId, messageId);
        // if (!flag) {
        //     log.warn("[PubComp] -> The message is not in Flow cache,clientId={},msgId={}", clientId, messageId);
        // }
    }
}
