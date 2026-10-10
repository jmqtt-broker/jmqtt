package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端publish消息到jmqtt broker
 */
@Slf4j
public class PublishCtrl extends BaseCtrl {

    public PublishCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore, MessageStore messageStore, SubscriptionMatcher subscriptionMatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {

    }


}
