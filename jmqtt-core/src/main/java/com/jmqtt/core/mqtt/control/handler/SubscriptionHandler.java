package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;

public interface SubscriptionHandler {

    void handleSubscription(ClientSession clientSession, MqttSubscribeMessage subscribeMessage);

}
