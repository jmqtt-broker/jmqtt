package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.model.*;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class Mqtt_3_4_subscription implements SubscriptionHandler {

    protected final SubscriptionMatcher subscriptionMatcher;
    protected final JmqttAuth jmqttAuth;
    protected final MessageStore messageStore;
    protected final SessionStore sessionStore;
    protected final MessageDispatcher messageDispatcher;
    protected final BrokerConfig brokerConfig;
    protected final ClusterConfig clusterConfig;

    public Mqtt_3_4_subscription(JmqttAuth jmqttAuth,
                                 BrokerConfig brokerConfig,
                                 ClusterConfig clusterConfig,
                                 MessageStore messageStore,
                                 SessionStore sessionStore,
                                 SubscriptionMatcher subscriptionMatcher,
                                 MessageDispatcher messageDispatcher) {
        this.brokerConfig = brokerConfig;
        this.clusterConfig = clusterConfig;
        this.jmqttAuth = jmqttAuth;
        this.messageStore = messageStore;
        this.sessionStore = sessionStore;
        this.subscriptionMatcher = subscriptionMatcher;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void handleSubscription(ClientSession clientSession, List<SubTopic> subTopics, MqttMessageIdAndPropertiesVariableHeader variableHeader) {
        List<Integer> qosList = getTopicQos(subTopics);
        MqttMessage subAckMessage = MessageUtil.getSubAckMessage(variableHeader.messageId(), qosList);
        clientSession.getCtx().writeAndFlush(subAckMessage).addListener(future -> {
            if (future.isSuccess()) {
                subscribe(clientSession, subTopics);
            }
        });
    }

    private List<Integer> getTopicQos(List<SubTopic> topics) {
        List<Integer> qosList = new ArrayList<>(topics.size());
        for (SubTopic topic : topics) {
            qosList.add(Math.min(topic.getQos(), brokerConfig.getMaximumQos()));
        }
        return qosList;
    }

    private void subscribe(ClientSession clientSession, List<SubTopic> subTopics) {
        String clientId = clientSession.getClientId();
        for (SubTopic topic : subTopics) {
            String subTopic = topic.getTopicName();
            Subscription subscription = new Subscription(clusterConfig.getCurrentNodeName(), clientId, subTopic, topic.getQos(), null);
            SubscriptionOption option = topic.getOption();
            subscription.setOption(option);
            this.subscriptionMatcher.subscribe(subscription);
            this.sessionStore.storeSubscription(subscription);
            // 将订阅广播给其他节点

            // 发送保留消息
            Collection<Message> retainList = messageStore.getRetainMsg(subTopic);
            if (!retainList.isEmpty()) {

            }
            log.debug("subscribe topic success, clientId: {}, topic: {}.", clientId, subTopic);
        }
    }

}
