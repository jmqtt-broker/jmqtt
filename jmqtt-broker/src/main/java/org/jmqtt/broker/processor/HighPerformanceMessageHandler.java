package org.jmqtt.broker.processor;

import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.highperformance.InflowMessageHandler;
import org.jmqtt.broker.store.highperformance.OutflowMessageHandler;
import org.jmqtt.broker.store.highperformance.OutflowSecMessageHandler;

import java.util.Collection;

public abstract class HighPerformanceMessageHandler {

    private boolean highPerformance;
    private SessionStore sessionStore;

    public HighPerformanceMessageHandler(boolean highPerformance, SessionStore sessionStore) {
        this.highPerformance = highPerformance;
        this.sessionStore = sessionStore;
    }

    protected boolean cacheInflowMsg(String clientId, Message message) {
        if (highPerformance) {
            return InflowMessageHandler.cacheInflowMsg(clientId, message);
        }
        return this.sessionStore.cacheInflowMsg(clientId, message);
    }

    protected Message releaseInflowMsg(String clientId, Integer msgId) {
        if (highPerformance) {
            return InflowMessageHandler.releaseInflowMsg(clientId, msgId);
        }
        return sessionStore.releaseInflowMsg(clientId, msgId);
    }

    protected Collection<Message> getAllInflowMsg(String clientId) {
        if (highPerformance) {
            return InflowMessageHandler.getAllInflowMsg(clientId);
        }
        return sessionStore.getAllInflowMsg(clientId);
    }

    protected boolean cacheOutflowMsg(String clientId, Message message) {
        if (highPerformance) {
            return OutflowMessageHandler.cacheOutflowMsg(clientId, message);
        }
        return this.sessionStore.cacheOutflowMsg(clientId, message);
    }

    protected void releaseOutflowMsg(String clientId, Integer msgId) {
        if (highPerformance) {
            OutflowMessageHandler.releaseOutflowMsg(clientId, msgId);
        } else {
            this.sessionStore.releaseOutflowMsg(clientId, msgId);
        }
    }

    protected Collection<Message> getAllOutflowMsg(String clientId) {
        if (highPerformance) {
            return OutflowMessageHandler.getAllOutflowMsg(clientId);
        }
        return this.sessionStore.getAllOutflowMsg(clientId);
    }

    protected boolean cacheOutflowSecMsgId(String clientId, int msgId) {
        if (highPerformance) {
            return OutflowSecMessageHandler.cacheOutflowSecMsgId(clientId, msgId);
        }
        return this.sessionStore.cacheOutflowSecMsgId(clientId, msgId);
    }

    protected boolean releaseOutflowSecMsgId(String clientId, Integer msgId) {
        if (highPerformance) {
            return OutflowSecMessageHandler.releaseOutflowSecMsgId(clientId, msgId);
        }
        return this.sessionStore.releaseOutflowSecMsgId(clientId, msgId);
    }

    protected Collection<Integer> getAllOutflowSecMsgId(String clientId) {
        if (highPerformance) {
            return OutflowSecMessageHandler.getAllOutflowSecMsgId(clientId);
        }
        return this.sessionStore.getAllOutflowSecMsgId(clientId);
    }

}
