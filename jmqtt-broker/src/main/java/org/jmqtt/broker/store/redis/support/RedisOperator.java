package org.jmqtt.broker.store.redis.support;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/11/10 12:32
 */
public interface RedisOperator {

    int DEFAULT_EXPIRE = 30 * 24 * 3600;

    default void init() {}

    boolean set(String key, String value);

    String get(String key);

    boolean del(String key);

    boolean hset(String table, String key, String value);

    String hget(String table, String key);

    Map<String, String> hgetAll(String table);

    boolean hdel(String table, String key);

    boolean publish(String channel, String message);

    void subscribe(String channelPattern, BiConsumer<String, String> consumer);

    default void close() {

    }

}
