package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.handler.Mqtt_3_4_subscription;
import com.jmqtt.core.mqtt.control.handler.Mqtt_5_subscription;
import com.jmqtt.core.mqtt.control.handler.SubscriptionHandler;
import com.jmqtt.core.mqtt.helper.MessageUtil;
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

    public SubscribeCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore, MessageStore messageStore, SubscriptionMatcher subscriptionMatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
        Mqtt_3_4_subscription mqtt_3_4_subscription = new Mqtt_3_4_subscription(jmqttAuth, brokerConfig, messageStore, sessionStore, subscriptionMatcher, messageDispatcher);
        Mqtt_5_subscription mqtt_5_subscription = new Mqtt_5_subscription(jmqttAuth, brokerConfig, messageStore, sessionStore, subscriptionMatcher, messageDispatcher);
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
        List<Topic> validTopicList = validTopics(clientSession, subscriptions);
        MqttMessageIdAndPropertiesVariableHeader variableHeader = (MqttMessageIdAndPropertiesVariableHeader) subscribeMessage.variableHeader();
        MqttProperties properties = variableHeader.properties();



        List<Integer> ackQos = getTopicQos(validTopicList);
        MqttMessage subAckMessage = MessageUtil.getSubAckMessage(variableHeader.messageId(), ackQos);
        ctx.writeAndFlush(subAckMessage);
        subscribe(clientSession, validTopicList);
    }

    private List<Integer> getTopicQos(List<Topic> topics) {
        List<Integer> qoss = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            // qoss.add(Math.min(topic.getQos(), BrokerContext.getBrokerConfig().getMaximumQos()));
        }
        return qoss;
    }

    private void subscribe(ClientSession clientSession, List<Topic> validTopicList) {
        String clientId = clientSession.getClientId();
        for (Topic topic : validTopicList) {
            String subTopic = topic.getTopicName();
            // Subscription subscription = new Subscription(BrokerContext.getBrokerId(), clientId, subTopic, topic.getQos(), null);
            // SubscriptionOption option = topic.getOption();
            // subscription.setOption(option);

        }
    }

    private List<Topic> validTopics(ClientSession clientSession, List<MqttTopicSubscription> subscriptions) {
        List<Topic> topicList = new ArrayList<>();
        for (MqttTopicSubscription subscription : subscriptions) {

            Topic topic = new Topic(subscription.topicName(), subscription.qualityOfService().value());
            if (clientSession.isMqtt5()) {
                MqttSubscriptionOption option = subscription.option();
                topic.setOption(new SubscriptionOption(option));
            }
            topicList.add(topic);
        }
        return topicList;
    }

}
