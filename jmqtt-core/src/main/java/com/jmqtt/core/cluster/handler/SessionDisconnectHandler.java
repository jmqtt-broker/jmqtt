package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.mqtt.JmqttContext;
import com.jmqtt.core.cluster.ClusterMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionDisconnectHandler implements ClusterMessageHandler<String> {

    @Override
    public ClusterMessage.EventType getType() {
        return ClusterMessage.EventType.SESSION_DISCONNECT;
    }

    @Override
    public void handle(ClusterMessage<String> msg) {
        JmqttContext.getSessionStore().clearSession(msg.getPayload(), true);
        log.debug("[Cluster] session disconnected: clientId={}", msg.getPayload());
    }

    public static SessionDisconnectHandler of() {
        return new SessionDisconnectHandler();
    }

}
