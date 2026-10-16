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
import lombok.extern.slf4j.Slf4j;

/**
 * 出栈消息接收到的qos2消息的pubComp报文：清除缓存的出栈消息
 */
@Slf4j
public class PubCompCtrl extends BaseCtrl {

    public PubCompCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                    SessionStore sessionStore, MessageStore messageStore,
                    SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String clientId = ChannelUtil.getClientId(ctx.channel());
        int messageId = MessageUtil.getMessageId(mqttMessage);
        boolean flag = sessionStore.releaseOutflowSecMsgId(clientId, messageId);
        log.debug("[PubComp] -> Receive PubCom and remove the flow message,clientId={},msgId={}", clientId, messageId);
        if (!flag) {
            log.warn("[PubComp] -> The message is not in Flow cache,clientId={},msgId={}", clientId, messageId);
        }
    }
}
