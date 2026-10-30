package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.mqtt.JmqttContext;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.store.SessionState;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class SessionConnectHandler implements ClusterMessageHandler<SessionState> {

    @Override
    public ClusterMessage.EventType getType() {
        return ClusterMessage.EventType.SESSION_CONNECT;
    }

    @Override
    public void handle(ClusterMessage<SessionState> msg) {
        SessionState session = msg.getPayload();
        String clientId = session.getClientId();
        ClientSession local = ClientManager.getClient(clientId);
        if (local != null) {
            log.info("[Cluster] client {} connected to remote broker {}, kicking local connection.", clientId, session.getBrokerId());
            MessageUtil.sendDisconnect(local, Reason.SESSION_TAKEN_OVER);
        }
        JmqttContext.getSessionStore().clearSession(clientId, true);
        JmqttContext.getSessionStore().storeSession(session);
    }

    public static SessionConnectHandler of() {
        return new SessionConnectHandler();
    }

}
