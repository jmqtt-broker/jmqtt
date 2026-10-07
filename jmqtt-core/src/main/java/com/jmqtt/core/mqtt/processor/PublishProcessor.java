package com.jmqtt.core.mqtt.processor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端publish消息到jmqtt broker
 */
@Slf4j
public class PublishProcessor extends AbstractMessageProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {

    }


}
