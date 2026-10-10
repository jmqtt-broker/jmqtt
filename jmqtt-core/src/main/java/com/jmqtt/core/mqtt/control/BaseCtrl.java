package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.handler.ConnectHandler;
import com.jmqtt.core.mqtt.control.handler.Mqtt_3_4;
import com.jmqtt.core.mqtt.control.handler.Mqtt_5;
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

    protected final Map<Byte, ConnectHandler> clientHandlerMap;

    protected final SessionStore sessionStore;

    protected final MessageStore messageStore;

    protected final SubscriptionMatcher subscriptionMatcher;

    public BaseCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore,
                    MessageStore messageStore, SubscriptionMatcher subscriptionMatcher) {
        this.jmqttAuth = jmqttAuth;
        this.brokerConfig = brokerConfig;
        this.clusterConfig = clusterConfig;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.subscriptionMatcher = subscriptionMatcher;
        Mqtt_3_4 mqtt_3_4 = new Mqtt_3_4(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
        Mqtt_5 mqtt_5 = new Mqtt_5(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
        this.clientHandlerMap = Map.of(
                MqttVersion.MQTT_3_1.protocolLevel(), mqtt_3_4,
                MqttVersion.MQTT_3_1_1.protocolLevel(), mqtt_3_4,
                MqttVersion.MQTT_5.protocolLevel(), mqtt_5
        );
    }

    protected abstract void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage);

}
