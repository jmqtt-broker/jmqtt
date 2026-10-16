package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.control.handler.MessageHandler;
import com.jmqtt.core.mqtt.control.handler.Mqtt_3_4_message;
import com.jmqtt.core.mqtt.control.handler.Mqtt_5_message;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttVersion;

import java.util.Map;

public abstract class BaseCtrl {

    protected final BrokerConfig brokerConfig;

    protected final ClusterConfig clusterConfig;

    protected final JmqttAuth jmqttAuth;

    protected final Map<Byte, MessageHandler> messageHandlerMap;

    protected final SessionStore sessionStore;

    protected final MessageStore messageStore;

    protected final SubscriptionMatcher subscriptionMatcher;

    protected final MessageDispatcher messageDispatcher;

    public BaseCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore,
                    MessageStore messageStore, SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        this.jmqttAuth = jmqttAuth;
        this.brokerConfig = brokerConfig;
        this.clusterConfig = clusterConfig;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.subscriptionMatcher = subscriptionMatcher;
        this.messageDispatcher = messageDispatcher;

        Mqtt_3_4_message mqtt_3_4_message = new Mqtt_3_4_message(jmqttAuth, brokerConfig, clusterConfig,
                sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
        Mqtt_5_message mqtt_5_message = new Mqtt_5_message(jmqttAuth, brokerConfig, clusterConfig,
                sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
        this.messageHandlerMap = Map.of(
                MqttVersion.MQTT_3_1.protocolLevel(), mqtt_3_4_message,
                MqttVersion.MQTT_3_1_1.protocolLevel(), mqtt_3_4_message,
                MqttVersion.MQTT_5.protocolLevel(), mqtt_5_message
        );

    }

    public abstract void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage);

}
