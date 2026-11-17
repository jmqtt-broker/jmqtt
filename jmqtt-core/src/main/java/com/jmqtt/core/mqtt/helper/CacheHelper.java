package com.jmqtt.core.mqtt.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheHelper {

    private static final Cache<String, Object> CACHE = Caffeine.newBuilder()
            // key过期后处理逻辑
            .removalListener((String key, Object val, RemovalCause cause) -> {
                log.debug("CacheHelper缓存过期，key: {}, value: {}, reason: {}", key, val, cause);
            })
            .expireAfterWrite(1, TimeUnit.MINUTES)
            // 过期时间到后立即触发，默认key过期后不触发回调，等下次调用或者缓存空间不足时才清理过期的key
            .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(1)))
            // 最大key个数
            .maximumSize(1000000)
            .build();

    public static void put(String key, Object value) {
        CACHE.put(key, value);
    }

    public static Object get(String key) {
        return CACHE.getIfPresent(key);
    }

    public static boolean exist(String key) {
        return get(key) != null;
    }

    public static void remove(String key) {
        CACHE.invalidate(key);
    }

}
