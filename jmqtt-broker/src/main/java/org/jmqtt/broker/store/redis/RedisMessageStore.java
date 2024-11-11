package org.jmqtt.broker.store.redis;

import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.redis.support.*;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisMessageStore implements MessageStore {

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
    public boolean storeWillMessage(String clientId, Message message) {
        return redisOperator.set(RedisKeySupport.WILL+clientId, JSONObject.toJSONString(message));
    }

    @Override
    public boolean clearWillMessage(String clientId) {
        return redisOperator.del(RedisKeySupport.WILL+clientId);
    }

    @Override
    public Message getWillMessage(String clientId) {
        String messageStr = redisOperator.get(RedisKeySupport.WILL + clientId);
        if (messageStr != null && !"".equals(messageStr)) {
            return JSONObject.parseObject(messageStr,Message.class);
        }
        return null;
    }

    @Override
    public boolean storeRetainMessage(String topic, Message message) {
        return redisOperator.hset(RedisKeySupport.RETAIN,String.valueOf(topic),JSONObject.toJSONString(message));
    }

    @Override
    public boolean clearRetainMessage(String topic) {
        return redisOperator.hdel(RedisKeySupport.RETAIN,topic);
    }

    @Override
    public Collection<Message> getAllRetainMsg() {
        Map<String,String> retainMessages = redisOperator.hgetAll(RedisKeySupport.RETAIN);
        return retainMessages.values().stream().map(val->JSONObject.parseObject(val,Message.class)).collect(Collectors.toList());
    }
}
