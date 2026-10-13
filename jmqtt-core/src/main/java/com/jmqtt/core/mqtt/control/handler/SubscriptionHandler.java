package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.mqtt.model.SubTopic;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;

import java.util.List;

public interface SubscriptionHandler {

    void handleSubscription(ClientSession clientSession, List<SubTopic> subTopics, MqttMessageIdAndPropertiesVariableHeader variableHeader);

}
