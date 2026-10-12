package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Mqtt_3_4_message implements MessageHandler {

    protected final JmqttAuth jmqttAuth;

    protected final BrokerConfig brokerConfig;

    protected final ClusterConfig clusterConfig;

    protected final SessionStore sessionStore;

    protected final MessageStore messageStore;

    protected final SubscriptionMatcher subscriptionMatcher;

    protected final MessageDispatcher messageDispatcher;

    public Mqtt_3_4_message(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                            SessionStore sessionStore, MessageStore messageStore,
                            SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        this.jmqttAuth = jmqttAuth;
        this.brokerConfig = brokerConfig;
        this.clusterConfig = clusterConfig;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.subscriptionMatcher = subscriptionMatcher;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public Message buildMessage(ClientSession clientSession, MqttPublishMessage publishMessage) {
        String clientId = clientSession.getClientId();
        MqttFixedHeader fixedHeader = publishMessage.fixedHeader();
        MqttPublishVariableHeader variableHeader = publishMessage.variableHeader();
        MqttQoS qos = fixedHeader.qosLevel();
        if (qos.value() > brokerConfig.getMaximumQos()) {
            throw BrokerException.of(Reason.QOS_NOT_SUPPORTED);
        }
        String topic = variableHeader.topicName();
        Message msg = new Message();
        msg.setStoreTime(System.currentTimeMillis());
        msg.setMsgId(variableHeader.packetId());
        msg.setPayload(MessageUtil.readBytesFromByteBuf(publishMessage.payload()));
        msg.setClientId(clientId);
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageHeader.TOPIC, topic);
        headers.put(MessageHeader.QOS, qos.value());
        headers.put(MessageHeader.RETAIN, fixedHeader.isRetain());
        headers.put(MessageHeader.DUP, fixedHeader.isDup());
        headers.put(MessageHeader.REMAINING_LENGTH, fixedHeader.remainingLength());
        msg.setHeaders(headers);
        return msg;
    }

    @Override
    public MessageHandler checkMessage(ClientSession clientSession, MqttFixedHeader fixedHeader, Message message) {
        if (!jmqttAuth.publishVerify(clientSession.getClientId(), (String) message.getHeader(MessageHeader.TOPIC))) {
            throw BrokerException.of(Reason.PERMISSION_NOT_ALLOWED);
        }
        return this;
    }

    @Override
    public void handleMessage(ClientSession clientSession, Message message, MqttQoS qos) {
        switch (qos) {
            case MqttQoS.AT_MOST_ONCE:
                dispatcherMessage(message);
                break;
            case MqttQoS.AT_LEAST_ONCE:
                handleQos1(clientSession, message);
                break;
            case MqttQoS.EXACTLY_ONCE:
                handleQos2(clientSession, message);
                break;
            default:
                log.warn("publish wrong message, clientId = {}, qos = {}", message.getClientId(), qos);
        }
    }

    protected void handleQos1(ClientSession clientSession, Message message) {
        MqttMessage pubAckMessage = MessageUtil.getPubReplyMessage(message.getMsgId(),
                MqttMessageType.PUBACK, Reason.SUCCESS.getReasonCode(), null, false);
        clientSession.getCtx().writeAndFlush(pubAckMessage).addListener(future -> {
            if (future.isDone()) {
                log.info("pubAckMessage success");
                dispatcherMessage(message);
            }
        });
    }

    protected void handleQos2(ClientSession clientSession, Message message) {
        MqttMessage pubRecMessage = MessageUtil.getPubReplyMessage(message.getMsgId(),
                MqttMessageType.PUBREC, Reason.SUCCESS.getReasonCode(), null, false);
        clientSession.getCtx().writeAndFlush(pubRecMessage).addListener(future -> {
            if (future.isDone()) {
                log.info("pubRecMessage success");
                sessionStore.cacheInflowMsg(message.getClientId(), message);
            }
        });
    }

    @Override
    public void dispatcherMessage(Message message) {
        messageStore.retainHandle(message);
        byte[] payload = message.getPayload();
        if (payload != null && payload.length > 0) {
            messageDispatcher.accept(message);
        }
    }

}
