package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mqtt_5_subscription extends Mqtt_3_4_subscription {

    public Mqtt_5_subscription(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, MessageStore messageStore, SessionStore sessionStore,
                               SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, messageStore, sessionStore, subscriptionMatcher, messageDispatcher);
    }

}
