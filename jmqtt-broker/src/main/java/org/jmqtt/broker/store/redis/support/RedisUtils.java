package org.jmqtt.broker.store.redis.support;

import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.store.redis.RedisCallBack;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * redis 访问模板方法类
 */
public class RedisUtils {
    private static final RedisUtils redisUtils = new RedisUtils();
    private volatile RedisOperator redisSupport;

    private AtomicBoolean start = new AtomicBoolean(false);

    private RedisUtils() {
    }

    public static RedisUtils getInstance() {
        return redisUtils;
    }

    public RedisOperator createSupport(BrokerConfig brokerConfig) {
        if (start.compareAndSet(false, true)) {
            if (redisSupport == null) {
                this.redisSupport = new RedisSupportImpl(brokerConfig);
                this.redisSupport.init();
            }
        }
        return redisSupport;
    }

    public void setOperator(RedisOperator operator) {
        if (start.compareAndSet(false, true)) {
            this.redisSupport = operator;
        }
    }

    public void close() {
        if (start.compareAndSet(true, false) && redisSupport != null) {
            redisSupport.close();
        }
    }
}
