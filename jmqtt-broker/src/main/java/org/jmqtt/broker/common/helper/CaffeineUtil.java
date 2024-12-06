package org.jmqtt.broker.common.helper;

import com.github.benmanes.caffeine.cache.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.checkerframework.checker.index.qual.NonNegative;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class CaffeineUtil {

    private static String REDIS_KEY_DELIMITER = ":";
    private final static Map<String, BiConsumer<String, String>> LISTENER_MAP = new HashMap<>();
    private static Cache<String, CacheObject> cache = Caffeine.newBuilder()
            // key过期后处理逻辑
            .removalListener((String key, CacheObject val, RemovalCause cause) ->
                    LISTENER_MAP.forEach((k, v) -> {
                        if (key.startsWith(k) && RemovalCause.EXPIRED.equals(cause)) {
                            v.accept(key, val.getData());
                        }
                    }))
            // 过期时间到后立即触发，默认key过期后不触发回调，等下次调用或者缓存空间不足时才清理过期的key
            .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(1)))
            // 最大key个数
            .maximumSize(1000000)
            // 可以针对每个key设置过期时间
            .expireAfter(new Expiry<String, CacheObject>() {
                @Override
                public long expireAfterCreate(String key, CacheObject value, long currentTime) {
                    return value.expire;
                }

                @Override
                public long expireAfterUpdate(String key, CacheObject value, long currentTime, @NonNegative long currentDuration) {
                    return value.expire;
                }

                @Override
                public long expireAfterRead(String key, CacheObject value, long currentTime, @NonNegative long currentDuration) {
                    return value.expire;
                }
            })
            .build();

    public static void put(String k, String v) {
        CacheObject cacheObject = new CacheObject(v);
        cache.put(k, cacheObject);
    }

    public static void put(String k, String v, long expireSeconds) {
        CacheObject cacheObject = new CacheObject(v, expireSeconds);
        cache.put(k, cacheObject);
    }

    public static String get(String k) {
        CacheObject val = cache.getIfPresent(k);
        return Optional.ofNullable(val).isPresent() ? val.getData() : "";
    }

    public static void del(String k) {
        cache.invalidate(k);
    }

    public static void addRemoveListener(String key, BiConsumer<String, String> listener) {
        LISTENER_MAP.put(key, listener);
    }

    public static String buildKey(String... args) {
        return String.join(REDIS_KEY_DELIMITER, args);
    }

    @SneakyThrows
    public static void main(String[] args) {
        addRemoveListener("test", (k, v) -> {
            System.out.println("key过期了 -> " + k + ": " + v);
        });
        put("test", "testVal", 6);
//        put("test", "testVal", 5);
//        put("test2", "testVal2", 6);
//        put("test3", "testVal3", 7);
//        put("4test", "testVal4", 8);
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(get("test"));
        });
        thread.start();
    }

    @Getter
    @Setter
    static class CacheObject {

        private static Long defaultExpire = 1800L;
        String data;
        long expire;

        public CacheObject(String data, long second) {
            this.data = data;
            this.expire = TimeUnit.SECONDS.toNanos(second);
        }

        public CacheObject(String data) {
            this.data = data;
            this.expire = TimeUnit.SECONDS.toNanos(defaultExpire);
        }
    }
}
