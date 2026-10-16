package com.jmqtt.core.mqtt.control;

import cn.hutool.core.collection.CollUtil;
import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.control.handler.Mqtt_3_4_subscription;
import com.jmqtt.core.mqtt.control.handler.Mqtt_5_subscription;
import com.jmqtt.core.mqtt.control.handler.SubscriptionHandler;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.mqtt.model.*;
import com.jmqtt.core.netty.session.ClientSession;
import com.jmqtt.core.netty.session.ClientManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订阅控制报文
 */
@Slf4j
public class SubscribeCtrl extends BaseCtrl {

    protected final Map<Byte, SubscriptionHandler> subscriptionHandlerMap;

    public SubscribeCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                       SessionStore sessionStore, MessageStore messageStore,
                       SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
        Mqtt_3_4_subscription mqtt_3_4_subscription = new Mqtt_3_4_subscription(jmqttAuth, brokerConfig, clusterConfig, messageStore, sessionStore, subscriptionMatcher, messageDispatcher);
        Mqtt_5_subscription mqtt_5_subscription = new Mqtt_5_subscription(jmqttAuth, brokerConfig, clusterConfig, messageStore, sessionStore, subscriptionMatcher, messageDispatcher);
        this.subscriptionHandlerMap = Map.of(
                MqttVersion.MQTT_3_1.protocolLevel(), mqtt_3_4_subscription,
                MqttVersion.MQTT_3_1_1.protocolLevel(), mqtt_3_4_subscription,
                MqttVersion.MQTT_5.protocolLevel(), mqtt_5_subscription
        );
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) mqttMessage;
        String clientId = ChannelUtil.getClientId(ctx.channel());
        ClientSession clientSession = ClientManager.getClient(clientId);
        List<MqttTopicSubscription> subscriptions = subscribeMessage.payload().topicSubscriptions();
        List<SubTopic> subTopicList = getTopics(subscriptions);
        if (CollUtil.isEmpty(subTopicList)) {
            throw BrokerException.of(Reason.UNSPECIFIED_ERROR);
        }
        MqttMessageIdAndPropertiesVariableHeader variableHeader = (MqttMessageIdAndPropertiesVariableHeader) subscribeMessage.variableHeader();
        try {
            SubscriptionHandler handler = subscriptionHandlerMap.get(ChannelUtil.getVersion(ctx.channel()).byteValue());
            handler.handleSubscription(clientSession, subTopicList, variableHeader);
        } catch (BrokerException e) {
            log.error("subscribe topic error, clientId: {}.", clientId, e);
            MessageUtil.sendDisconnect(clientSession, e.getReason());
        } catch (Exception e) {
            log.error("subscribe topic unknown error, clientId: {}.", clientId, e);
            MessageUtil.sendDisconnect(clientSession, Reason.UNSPECIFIED_ERROR);
        }
    }

    private List<SubTopic> getTopics(List<MqttTopicSubscription> subscriptions) {
        List<SubTopic> topicList = new ArrayList<>(subscriptions.size());
        for (MqttTopicSubscription subscription : subscriptions) {
            SubTopic topic = new SubTopic(subscription.topicName(), subscription.qualityOfService().value());
            topic.setOption(new SubscriptionOption(subscription.option()));
            topicList.add(topic);
        }
        return topicList;
    }

}
