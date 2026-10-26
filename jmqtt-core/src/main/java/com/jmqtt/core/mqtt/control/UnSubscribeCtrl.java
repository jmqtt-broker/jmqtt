package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.cluster.JmqttCluster;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.netty.session.ClientSession;
import com.jmqtt.core.netty.session.ClientManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 取消订阅
 */
@Slf4j
public class UnSubscribeCtrl extends BaseCtrl {

    public UnSubscribeCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                           SessionStore sessionStore, MessageStore messageStore,
                           SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) mqttMessage;
        MqttUnsubscribePayload unsubscribePayload = unsubscribeMessage.payload();
        List<String> topics = unsubscribePayload.topics();
        String clientId = ChannelUtil.getClientId(ctx.channel());
        ClientSession clientSession = ClientManager.getClient(clientId);
        if (Objects.isNull(clientSession)) {
            log.warn("[UnSubscribe] -> The client is not online.clientId={}", clientId);
        }
        byte reasonCode = 0;
        for (String topic : topics) {
            if (!subscriptionMatcher.unSubscribe(topic, clientId)) {
                // 取消订阅没有发现匹配的订阅
                reasonCode = 0x11;
            }
            Thread.startVirtualThread(() -> {
                Subscription subscription = sessionStore.getOneSubscription(clientId, topic);
                if (subscription != null) {
                    sessionStore.delSubscription(clientId, topic);
                    JmqttCluster.broadcast(ClusterMessage.of(ClusterMessage.EventType.UNSUBSCRIBE_SYNC, subscription));
                }
            });
            log.debug("unSubscribe topic success, clientId: {}, topic: {}.", clientId, topic);
        }
        MqttUnsubAckMessage unsubAckMessage = MessageUtil.getUnSubAckMessage(MessageUtil.getMessageId(mqttMessage), reasonCode);
        ctx.writeAndFlush(unsubAckMessage);
    }
}
