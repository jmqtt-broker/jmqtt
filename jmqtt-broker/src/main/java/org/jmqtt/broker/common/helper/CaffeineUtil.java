package org.jmqtt.broker.common.helper;

import com.github.benmanes.caffeine.cache.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
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
            // key过期后处理逻辑
            .removalListener((String key, CacheObject val, RemovalCause cause) -> {
                if (cause.equals(RemovalCause.EXPIRED)) {
                    if (val != null && (val.getData() instanceof TimerBO)) {
                        TimerBO bo = (TimerBO) val.getData();
                        Optional.ofNullable(bo.getExpiredFunc()).ifPresent(func -> func.accept(key, bo.getData()));
                    }
                    LISTENERS.forEach(l -> l.accept(key, val));
                }
            })
            // 过期时间到后立即触发，默认key过期后不触发回调，等下次调用或者缓存空间不足时才清理过期的key
            .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(10, new ThreadFactoryImpl("caffeine_scheduler"))))
            // 最大key个数
            .maximumSize(Integer.MAX_VALUE)
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

    @SneakyThrows
    public static void main(String[] args) {
        addRemoveListener((k, v) -> {
            System.out.println("key过期了 -> " + k + ": " + v.getData());
        });
        put("test", "testVal", 6);
        put("test1", "testVal1", 20);
        // Thread thread = new Thread(() -> {
        //     try {
        //         Thread.sleep(6000);
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     }
        //     System.out.println(get("test"));
        //     put("test1", "test1111", 0);
        // });
        // thread.start();
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
