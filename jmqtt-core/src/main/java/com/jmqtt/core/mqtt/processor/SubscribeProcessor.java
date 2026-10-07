package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.netty.helper.NettyUtil;
import com.jmqtt.core.mqtt.model.*;
import com.jmqtt.core.netty.session.ClientSession;
import com.jmqtt.core.netty.session.ConnectManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 订阅报文逻辑处理
 * TODO mqtt5协议支持
 */
@Slf4j
public class SubscribeProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) mqttMessage;
        String clientId = NettyUtil.getClientId(ctx.channel());
        ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
        List<MqttTopicSubscription> subscriptions = subscribeMessage.payload().topicSubscriptions();
        List<Topic> validTopicList = validTopics(clientSession, subscriptions);
        if (validTopicList == null || validTopicList.size() == 0) {
            log.warn("[Subscribe] -> Valid all subscribe topic failure,clientId:{}", clientId);
            return;
        }
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

    /**
     * 返回校验合法的topic
     */
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
