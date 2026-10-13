package com.jmqtt.core.mqtt.control.handler;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.helper.TopicAliasManager;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mqtt_5_message extends Mqtt_3_4_message {

    public Mqtt_5_message(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                          SessionStore sessionStore, MessageStore messageStore,
                          SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public Message buildMessage(ClientSession clientSession, MqttPublishMessage publishMessage) {
        Message message = super.buildMessage(clientSession, publishMessage);
        String clientId = clientSession.getClientId();
        MqttPublishVariableHeader variableHeader = publishMessage.variableHeader();
        String topic = variableHeader.topicName();
        MqttProperties properties = variableHeader.properties();
        if (properties != null && !properties.isEmpty()) {
            MqttProperties.IntegerProperty topicAlias = (MqttProperties.IntegerProperty)
                    properties.getProperty(MqttProperties.MqttPropertyType.TOPIC_ALIAS.value());
            if (topicAlias != null) {
                if (StrUtil.isNotBlank(topic)) {
                    TopicAliasManager.put(clientId, topicAlias.value(), topic);
                } else {
                    if (checkAlias(clientId, topicAlias.value())) {
                        String realTopic = TopicAliasManager.get(clientId, topicAlias.value());
                        if (StringUtils.isNotBlank(realTopic)) {
                            message.getHeaders().put(MessageHeader.TOPIC, realTopic);
                        } else {
                            // 没有根据别名找到topic，可能是重连后发送了带别名的消息，正确做法是重连后
                            // 重新建立主题 -> 别名的映射关系
                            throw BrokerException.of(Reason.PROTOCOL_ERROR);
                        }
                    } else {
                        throw BrokerException.of(Reason.TOPIC_ALIAS_INVALID);
                    }
                }
            }
            message.setProperties(MessageUtil.propertyMap(properties));
        }
        return message;
    }

    @Override
    public MessageHandler checkMessage(ClientSession clientSession, MqttFixedHeader fixedHeader, Message message) {
        super.checkMessage(clientSession, fixedHeader, message);
        String clientId = clientSession.getClientId();
        if (!brokerConfig.getRetainAvailable() && fixedHeader.isRetain()) {
            // broker没有开启保留消息功能，禁止发布保留消息
            log.warn("retain not available, clientId: {}", clientId);
            throw BrokerException.of(Reason.RETAIN_NOT_SUPPORTED);
        }  else if (checkPackageSize(clientSession, fixedHeader.remainingLength())) {
            // 消息超出约定的大小
            log.warn("exceeding message, clientId: {}", clientId);
            throw BrokerException.of(Reason.PACKET_TOO_LARGE);
        }
        return this;
    }

    private boolean checkAlias(String clientId, Integer alias) {
        Integer maxAlias = (Integer) sessionStore.getClientProperty(clientId, MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM.value());
        return alias > 0 && maxAlias != null && alias <= maxAlias;
    }

    private boolean checkPackageSize(ClientSession clientSession, int remainingLength) {
        String clientId = clientSession.getClientId();
        Integer receiveMaximum = (Integer) sessionStore.getClientProperty(clientId, MqttProperties.MqttPropertyType.RECEIVE_MAXIMUM.value());
        return receiveMaximum != null && (remainingLength + 5) > receiveMaximum;
    }

}
