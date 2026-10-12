package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mqtt_3_4_subscription implements SubscriptionHandler {

    private final SubscriptionMatcher subscriptionMatcher;
    private final JmqttAuth jmqttAuth;
    private final MessageStore messageStore;
    private final SessionStore sessionStore;
    private final MessageDispatcher messageDispatcher;
    private final BrokerConfig brokerConfig;

    public Mqtt_3_4_subscription(JmqttAuth jmqttAuth,
                                  BrokerConfig brokerConfig,
                                  MessageStore messageStore,
                                  SessionStore sessionStore,
                                  SubscriptionMatcher subscriptionMatcher,
                                  MessageDispatcher messageDispatcher) {
        this.brokerConfig = brokerConfig;
        this.jmqttAuth = jmqttAuth;
        this.messageStore = messageStore;
        this.sessionStore = sessionStore;
        this.subscriptionMatcher = subscriptionMatcher;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void handleSubscription(ClientSession clientSession, MqttSubscribeMessage subscribeMessage) {

    }

}
