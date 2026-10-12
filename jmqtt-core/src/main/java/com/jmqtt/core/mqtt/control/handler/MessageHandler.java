package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

public interface MessageHandler {

    Message buildMessage(ClientSession clientSession, MqttPublishMessage publishMessage);

    default MessageHandler checkMessage(ClientSession clientSession, MqttFixedHeader fixedHeader, Message message) {
        return this;
    }

    void handleMessage(ClientSession clientSession, Message message, MqttQoS qos);

    void dispatcherMessage(Message message);

}
