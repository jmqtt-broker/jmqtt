package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * 出栈消息接收到qos2第一阶段的的pubRec报文: 丢弃消息，存储报文标志符
 */
@Slf4j
public class PubRecCtrl extends BaseCtrl {

    public PubRecCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                       SessionStore sessionStore, MessageStore messageStore,
                       SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String clientId = ChannelUtil.getClientId(ctx.channel());
        int messageId = MessageUtil.getMessageId(mqttMessage);
        log.debug("pubRec message,clientId={},msgId={}",clientId,messageId);
        MqttMessage pubRelMessage = MessageUtil.getPubReplyMessage(messageId, MqttMessageType.PUBREL, (byte) 0x00, null, false);
        ctx.writeAndFlush(pubRelMessage).addListener(future -> {
            if (future.isSuccess()) {
                sessionStore.releaseOutflowMsg(clientId,messageId);
                sessionStore.cacheOutflowSecMsgId(clientId,messageId);
            }
        });
    }
}
