
package org.jmqtt.broker.store.redis.support;

import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.store.redis.RedisCallBack;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.function.BiConsumer;

public class RedisSupportImpl implements RedisSupport {

    private static final Logger log = JmqttLogger.storeLog;

    private BrokerConfig brokerConfig;
    private JedisPool jedisPool;

    public static final String PROJECT = "JMQTT";

    public RedisSupportImpl(BrokerConfig brokerConfig) {
        this.brokerConfig = brokerConfig;
    }

    @Override
    public void init() {
        try {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMinIdle(brokerConfig.getMinIdle());
            jedisPoolConfig.setMaxTotal(brokerConfig.getMaxTotal());
            jedisPoolConfig.setTestOnBorrow(brokerConfig.isTestOnBorrow());
            jedisPoolConfig.setMaxTotal(jedisPoolConfig.getMaxIdle());
            jedisPoolConfig.setMaxWaitMillis(jedisPoolConfig.getMaxWaitMillis());
            if (StringUtils.isEmpty(brokerConfig.getRedisPassword())) {
                jedisPool = new JedisPool(jedisPoolConfig, brokerConfig.getRedisHost(),
                        brokerConfig.getRedisPort(), 2000, null,
                        brokerConfig.getDatabase(), null);
            } else {
                jedisPool = new JedisPool(jedisPoolConfig, brokerConfig.getRedisHost(),
                        brokerConfig.getRedisPort(), 10000, brokerConfig.getRedisPassword(),
                        brokerConfig.getDatabase(), null);
            }
        } catch (Exception ex) {
            LogUtil.error(log, "[Redis handle error],ex:{}", ex);
        }
    }

    @Override
    public <T> T operate(RedisCallBack<T> redisCallBack) {
        LogUtil.debug(log, "[Cluster] redis operate begin");
        long startTime = System.currentTimeMillis();
        try (Jedis jedis = jedisPool.getResource()) {
            return redisCallBack.operate(jedis);
        } catch (Exception ex) {
            LogUtil.error(log, "[Cluster] redis operate error,ex:{}", ex);
        } finally {
            LogUtil.debug(log, "[Cluster] redis operate cost:{}", (System.currentTimeMillis() - startTime));
        }
        return null;
    }

    @Override
    public void close() {
        LogUtil.info(log, "[Cluster] redis close");
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    @Override
    public boolean set(String key, String value) {
        String operate = operate(jedis -> {
            String res = jedis.set(key, value);
            jedis.expire(key, DEFAULT_EXPIRE);
            return res;
        });
        return RedisReplyUtils.isOk(operate);
    }

    @Override
    public String get(String key) {
        return operate(jedis -> jedis.get(key));
    }

    @Override
    public boolean del(String key) {
        operate(jedis -> jedis.del(key));
        return true;
    }

    @Override
    public boolean hset(String table, String key, String value) {
        operate(jedis -> {
            Long hset = jedis.hset(table, key, value);
            jedis.expire(table, DEFAULT_EXPIRE);
            return hset;
        });
        return true;
    }

    @Override
    public String hget(String table, String key) {
        return operate(jedis -> jedis.hget(table, key));
    }

    @Override
    public Map<String, String> hgetAll(String table) {
        return operate(jedis -> jedis.hgetAll(table));
    }

    @Override
    public boolean hdel(String table, String key) {
        operate(jedis -> jedis.hdel(table, key));
        return true;
    }

    @Override
    public boolean publish(String channel, String message) {
        operate(jedis -> jedis.publish(channel, message));
        return true;
    }

    @Override
    public void subscribe(String channelPattern, BiConsumer<String, String> consumer) {
        operate(jedis -> {
            jedis.psubscribe(new JedisPubSub() {
                @Override
                public void onPMessage(String pattern, String channel, String message) {
                    consumer.accept(channel, message);
                }
            }, channelPattern);
            return true;
        });
    }
}
