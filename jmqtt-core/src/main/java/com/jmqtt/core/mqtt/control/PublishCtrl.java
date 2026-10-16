package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.control.handler.MessageHandler;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端publish消息到jmqtt broker
 */
@Slf4j
public class PublishCtrl extends BaseCtrl {

    public PublishCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                       SessionStore sessionStore, MessageStore messageStore,
                       SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttPublishMessage publishMessage = (MqttPublishMessage) mqttMessage;
        MqttFixedHeader fixedHeader = publishMessage.fixedHeader();
        String clientId = ChannelUtil.getClientId(ctx.channel());
        ClientSession clientSession = ClientManager.getClient(clientId);
        MessageHandler handler = messageHandlerMap.get(ChannelUtil.getVersion(ctx.channel()).byteValue());
        try {
            Message message = handler.buildMessage(clientSession, publishMessage);
            handler.checkMessage(clientSession, fixedHeader, message)
                    .handleMessage(clientSession, message, fixedHeader.qosLevel());
        } catch (BrokerException e) {
            log.error("publish message error, clientId: {}.", clientId, e);
            MessageUtil.sendDisconnect(clientSession, e.getReason());
        } catch (Exception e) {
            log.error("publish message unknown error, clientId: {}.", clientId, e);
            MessageUtil.sendDisconnect(clientSession, Reason.UNSPECIFIED_ERROR);
        } finally {
            ReferenceCountUtil.release(mqttMessage.payload());
        }

    }


}
