package org.jmqtt.starter.redis;

import org.jmqtt.broker.store.redis.support.RedisOperator;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RedisOperatorImpl implements RedisOperator {

    private RedisConnectionFactory factory;

    private RedisMessageListenerContainer container;

    public RedisOperatorImpl(RedisConnectionFactory factory,
                             RedisMessageListenerContainer container) {
        Assert.notNull(factory, "redisConnectionFactory must not be null!");
        Assert.notNull(container, "redisMessageListenerContainer must not be null!");
        this.factory = factory;
        this.container = container;
    }

    private <T> T operate(Function<RedisConnection, T> func) {
        try (RedisConnection conn = factory.getConnection()) {
            return func.apply(conn);
        }
    }

    @Override
    public boolean set(String key, String value) {
        Boolean res = operate(conn -> conn.set(key.getBytes(), value.getBytes(),
                Expiration.seconds(DEFAULT_EXPIRE),
                RedisStringCommands.SetOption.UPSERT));
        return res != null && res;
    }

    @Override
    public String get(String key) {
        byte[] res = operate(conn -> conn.get(key.getBytes()));
        return Optional.ofNullable(res).map(String::new).orElse(null);
    }

    @Override
    public boolean del(String key) {
        operate(conn -> conn.del(key.getBytes()));
        return true;
    }

    @Override
    public boolean hset(String table, String key, String value) {
        Boolean result = operate(conn -> {
            byte[] tableBytes = table.getBytes();
            Boolean res = conn.hSet(tableBytes, key.getBytes(), value.getBytes());
            conn.expire(tableBytes, DEFAULT_EXPIRE);
            return res;
        });
        return result != null && result;
    }

    @Override
    public String hget(String table, String key) {
        byte[] res = operate(conn -> conn.hGet(table.getBytes(), key.getBytes()));
        return Optional.ofNullable(res).map(String::new).orElse(null);
    }

    @Override
    public Map<String, String> hgetAll(String table) {
        Map<byte[], byte[]> byteMap = operate(conn -> conn.hGetAll(table.getBytes()));
        Map<String, String> res = new HashMap<>();
        byteMap.forEach((k, v) -> res.put(new String(k), new String(v)));
        return res;
    }

    @Override
    public boolean hdel(String table, String key) {
        operate(conn -> conn.hDel(table.getBytes(), key.getBytes()));
        return true;
    }

    @Override
    public boolean publish(String channel, String message) {
        operate(conn -> conn.publish(channel.getBytes(), message.getBytes()));
        return true;
    }

    @Override
    public void subscribe(String channelPattern, BiConsumer<String, String> consumer) {
        container.addMessageListener(
                (message, pattern) -> {
                    consumer.accept(new String(message.getChannel()),
                            new String(message.getBody()));
                },
                new PatternTopic(channelPattern)
        );
    }

}
