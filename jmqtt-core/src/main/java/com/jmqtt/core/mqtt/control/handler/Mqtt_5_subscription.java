package com.jmqtt.core.mqtt.control.handler;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.common.JmqttConst;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.model.SubTopic;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class Mqtt_5_subscription extends Mqtt_3_4_subscription {

    public Mqtt_5_subscription(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, MessageStore messageStore, SessionStore sessionStore,
                               SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, messageStore, sessionStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public void handleSubscription(ClientSession clientSession, List<SubTopic> subTopics, MqttMessageIdAndPropertiesVariableHeader variableHeader) {
        String clientId = clientSession.getClientId();
        MqttProperties properties = variableHeader.properties();
        Optional<MqttProperties.IntegerProperty> subscriptionIdentifier = Optional.ofNullable((MqttProperties.IntegerProperty) properties.getProperty(MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value()));
        if (subscriptionIdentifier.isPresent()) {
            if (!brokerConfig.getSubscriptionIdentifierAvailable()) {
                log.warn("subscription Identifiers not supported,clientId:{}", clientId);
                throw BrokerException.of(Reason.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED);
            }
            subTopics.forEach(t -> t.getOption().setSubscriptionIdentifier(subscriptionIdentifier.get().value()));
        }
        for (SubTopic t : subTopics) {
            if (!brokerConfig.getSharedSubscriptionAvailable() && t.getTopicName().startsWith(JmqttConst.SHARE_IDENTIFIERS)) {
                log.warn("shared subscriptions not supported, clientId: {}", clientId);
                throw BrokerException.of(Reason.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED);
            }
            if (!brokerConfig.getWildcardSubscriptionAvailable() && (t.getTopicName().contains(JmqttConst.WILDCARD_SINGLE) ||
                    t.getTopicName().contains(JmqttConst.WILDCARD_MULTY))) {
                log.warn("wildcard subscriptions not supported, clientId: {}", clientId);
                throw BrokerException.of(Reason.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED);
            }
        }
        super.handleSubscription(clientSession, subTopics, variableHeader);
    }
}
