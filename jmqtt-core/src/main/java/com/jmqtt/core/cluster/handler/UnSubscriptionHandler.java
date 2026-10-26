package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.JmqttContext;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.mqtt.model.Subscription;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class UnSubscriptionHandler implements ClusterMessageHandler<Subscription> {

    @Override
    public ClusterMessage.EventType getType() {
        return ClusterMessage.EventType.UNSUBSCRIBE_SYNC;
    }

    @Override
    public void handle(ClusterMessage<Subscription> msg) {
        Subscription subscription = msg.getPayload();
        JmqttContext.getSubscriptionMatcher().unSubscribe(subscription.getTopic(), subscription.getClientId());
        JmqttContext.getSessionStore().storeSubscription(subscription);
    }

    public static UnSubscriptionHandler of() {
        return new UnSubscriptionHandler();
    }

}
