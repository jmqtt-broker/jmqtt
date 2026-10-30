package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.mqtt.JmqttContext;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.mqtt.model.Subscription;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class SubscriptionHandler implements ClusterMessageHandler<Subscription> {

    @Override
    public ClusterMessage.EventType getType() {
        return ClusterMessage.EventType.SUBSCRIBE_SYNC;
    }

    @Override
    public void handle(ClusterMessage<Subscription> msg) {
        Subscription subscription = msg.getPayload();
        JmqttContext.getSubscriptionMatcher().subscribe(subscription);
        JmqttContext.getSessionStore().storeSubscription(subscription);
    }

    public static SubscriptionHandler of() {
        return new SubscriptionHandler();
    }

}
