package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.control.handler.MessageHandler;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.netty.session.ClientManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class PubRelCtrl extends BaseCtrl {

    public PubRelCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                       SessionStore sessionStore, MessageStore messageStore,
                       SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String clientId = ChannelUtil.getClientId(ctx.channel());
        int msgId = MessageUtil.getMessageId(mqttMessage);
        if (ClientManager.containsClient(clientId)) {
            Message message = sessionStore.releaseInflowMsg(clientId, msgId);
            if (Objects.nonNull(message)) {
                MessageHandler handler = messageHandlerMap.get(ChannelUtil.getVersion(ctx.channel()).byteValue());
                MqttMessage pubComMessage = MessageUtil.getPubReplyMessage(msgId, MqttMessageType.PUBCOMP, (byte) 0x00, null, false);
                ctx.writeAndFlush(pubComMessage).addListener(future -> {
                    if (future.isSuccess()) {
                        handler.dispatcherMessage(message);
                    }
                });
            } else {
                log.warn("pubRelMessage error, the message is not exist,clientId={},messageId={}.", clientId, msgId);
            }
        } else {
            log.warn("pubRelMessage error, the client：{} disconnect to this server.", clientId);
            ctx.close();
        }
    }
}
