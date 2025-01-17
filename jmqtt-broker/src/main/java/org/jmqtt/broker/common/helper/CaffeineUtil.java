package org.jmqtt.broker.common.helper;

import com.github.benmanes.caffeine.cache.*;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.index.qual.NonNegative;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class CaffeineUtil {

    private final static List<BiConsumer<String, CacheObject>> LISTENERS = new CopyOnWriteArrayList<>();
    private final static Cache<String, CacheObject> CACHE = Caffeine.newBuilder()
            // key过期回调
            .removalListener((String key, CacheObject val, RemovalCause cause) -> {
                if (cause.equals(RemovalCause.EXPIRED)) {
                    LISTENERS.forEach(l -> l.accept(key, val));
                }
            })
            // caffeine默认key过期后不清理，等下次调用或者缓存空间不足时才清理，导致过期后不能及时触发回调
            // 这里加个处理过期key的线程池，能达到过期立即清理和触发回调的效果
            .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(10, new ThreadFactoryImpl("caffeine_scheduler"))))
            // 最大key数量
            .maximumSize(Integer.MAX_VALUE)
            // 过期策略
            .expireAfter(new Expiry<String, CacheObject>() {
                @Override
                public long expireAfterCreate(String key, CacheObject value, long currentTime) {
                    return value.expire;
                }

                @Override
                public long expireAfterUpdate(String key, CacheObject value, long currentTime, @NonNegative long currentDuration) {
                    // 每个key每次被更新后都刷新过期时间
                    return value.expire;
                }

                @Override
                public long expireAfterRead(String key, CacheObject value, long currentTime, @NonNegative long currentDuration) {
                    // 每个key每次被访问都刷新过期时间
                    return value.expire;
                }
            })
            .build();

    public static void put(String k, Object v) {
        CacheObject cacheObject = new CacheObject(v);
        CACHE.put(k, cacheObject);
    }

    public static void put(String k, Object v, long expireSeconds) {
        CacheObject cacheObject = new CacheObject(v, expireSeconds);
        CACHE.put(k, cacheObject);
    }

    public static Object get(String k) {
        CacheObject val = CACHE.getIfPresent(k);
        return Optional.ofNullable(val).isPresent() ? val.getData() : null;
    }

    public static void del(String k) {
        CACHE.invalidate(k);
    }

    public static void addRemoveListener(BiConsumer<String, CacheObject> listener) {
        LISTENERS.add(listener);
    }

    @Getter
    @Setter
    static class CacheObject {

        private static Long defaultExpire = 1800L;
        Object data;
        long expire;

        public CacheObject(Object data, long second) {
            this.data = data;
            this.expire = TimeUnit.SECONDS.toNanos(second);
        }

        public CacheObject(Object data) {
            this.data = data;
            this.expire = TimeUnit.SECONDS.toNanos(defaultExpire);
        }
    }
}
