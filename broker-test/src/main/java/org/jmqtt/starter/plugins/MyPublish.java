package org.jmqtt.starter.plugins;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.processor.protocol.PublishProcessor;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.springframework.stereotype.Service;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/3/27 14:15
 */
@Slf4j
@Service
public class MyPublish extends PublishProcessor {

    public MyPublish(BrokerController controller) {
        super(controller);
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String topic = ((MqttPublishMessage) mqttMessage).variableHeader().topicName();
        byte[] payload = MessageUtil.getBytesFromByteBuf(((MqttPublishMessage) mqttMessage).payload());
        log.info("Topic: {}, 发布的消息: {}", topic, new String(payload));
        super.processRequest(ctx, mqttMessage);
    }
}
