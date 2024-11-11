package org.jmqtt.broker.store.redis;

import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.redis.support.RedisKeySupport;
import org.jmqtt.broker.store.redis.support.RedisOperator;
import org.jmqtt.broker.store.redis.support.RedisUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisSessionStore implements SessionStore {

    private RedisOperator redisOperator;

    @Override
    public void start(BrokerConfig brokerConfig) {
        this.redisOperator = RedisUtils.getInstance().createSupport(brokerConfig);
    }

    @Override
    public void shutdown() {
        RedisUtils.getInstance().close();
    }

    @Override
    public SessionState getSession(String clientId) {
        String sessionStr = redisOperator.get(RedisKeySupport.SESSION + clientId);
        SessionState sessionState;
        if (sessionStr == null) {
            return new SessionState(SessionState.StateEnum.NULL);
        } else {
            sessionState = JSONObject.parseObject(sessionStr, SessionState.class);
        }
        return sessionState;
    }

    @Override
    public boolean storeSession(String clientId, SessionState sessionState) {
        return redisOperator.set(RedisKeySupport.SESSION + clientId, JSONObject.toJSONString(sessionState));
    }

    @Override
    public boolean storeSubscription(String clientId, Subscription subscription) {
        return redisOperator.hset(RedisKeySupport.SUBSCRIPTION + clientId, subscription.getTopic(), JSONObject.toJSONString(subscription));
    }

    @Override
    public boolean delSubscription(String clientId, String topic) {
        return redisOperator.hdel(RedisKeySupport.SUBSCRIPTION, topic);
    }

    @Override
    public boolean clearSubscription(String clientId) {
        return redisOperator.del(RedisKeySupport.SUBSCRIPTION + clientId);
    }

    @Override
    public Set<Subscription> getSubscriptions(String clientId) {
        Map<String, String> subscriptions = redisOperator.hgetAll(RedisKeySupport.SUBSCRIPTION + clientId);
        return subscriptions.values().stream().map(val -> JSONObject.parseObject(val, Subscription.class)).collect(Collectors.toSet());
    }

    @Override
    public boolean cacheInflowMsg(String clientId, Message message) {
        return redisOperator.hset(RedisKeySupport.REC_FLOW_MESSAGE + clientId, String.valueOf(message.getMsgId()), JSONObject.toJSONString(message));
    }

    @Override
    public Message releaseInflowMsg(String clientId, int msgId) {
        String res = redisOperator.hget(RedisKeySupport.REC_FLOW_MESSAGE, String.valueOf(msgId));
        Message message = null;
        if (res != null) {
            message = JSONObject.parseObject(res, Message.class);
            redisOperator.hdel(RedisKeySupport.REC_FLOW_MESSAGE, String.valueOf(msgId));
        }
        return message;
    }

    @Override
    public Collection<Message> getAllInflowMsg(String clientId) {
        Map<String, String> inflowMessages = redisOperator.hgetAll(RedisKeySupport.REC_FLOW_MESSAGE + clientId);
        return inflowMessages.values().stream().map(val -> JSONObject.parseObject(val, Message.class)).collect(Collectors.toList());
    }

    @Override
    public boolean cacheOutflowMsg(String clientId, Message message) {
        return redisOperator.hset(RedisKeySupport.SEND_FLOW_MESSAGE + clientId, String.valueOf(message.getMsgId()), JSONObject.toJSONString(message));
    }

    @Override
    public Collection<Message> getAllOutflowMsg(String clientId) {
        Map<String, String> outflowMessages = redisOperator.hgetAll(RedisKeySupport.SEND_FLOW_MESSAGE + clientId);
        return outflowMessages.values().stream().map(val -> JSONObject.parseObject(val, Message.class)).collect(Collectors.toList());
    }

    @Override
    public Message releaseOutflowMsg(String clientId, int msgId) {
        String table = RedisKeySupport.SEND_FLOW_MESSAGE + clientId;
        String key = String.valueOf(msgId);
        Message message = JSONObject.parseObject(redisOperator.hget(table, key), Message.class);
        redisOperator.hdel(table, key);
        return message;
    }

    @Override
    public boolean cacheOutflowSecMsgId(String clientId, int msgId) {
        String value = String.valueOf(msgId);
        return redisOperator.hset(RedisKeySupport.SEND_FLOW_SEC_MESSAGE + clientId, value, value);
    }

    @Override
    public boolean releaseOutflowSecMsgId(String clientId, int msgId) {
        return redisOperator.hdel(RedisKeySupport.SEND_FLOW_SEC_MESSAGE + clientId, String.valueOf(msgId));
    }

    @Override
    public List<Integer> getAllOutflowSecMsgId(String clientId) {
        Map<String, String> secMsgIds = redisOperator.hgetAll(RedisKeySupport.SEND_FLOW_SEC_MESSAGE + clientId);
        return secMsgIds.values().stream().map(Integer::valueOf).collect(Collectors.toList());
    }

    @Override
    public boolean storeOfflineMsg(String clientId, Message message) {
        return redisOperator.hset(RedisKeySupport.OFFLINE + clientId, String.valueOf(message.getMsgId()), JSONObject.toJSONString(message));
    }

    @Override
    public Collection<Message> getAllOfflineMsg(String clientId) {
        Map<String, String> offlineMessages = redisOperator.hgetAll(RedisKeySupport.OFFLINE + clientId);
        return offlineMessages.values().stream().map(val -> JSONObject.parseObject(val, Message.class)).collect(Collectors.toList());
    }

    @Override
    public boolean clearOfflineMsg(String clientId) {
        return redisOperator.del(RedisKeySupport.OFFLINE + clientId);
    }
}
