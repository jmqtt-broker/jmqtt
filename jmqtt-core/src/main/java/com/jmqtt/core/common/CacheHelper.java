package com.jmqtt.core.common;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheHelper {

    /**
     * 缓存，默认五分钟，以put时间为准
     */
    private final static TimedCache<String, Object> CACHE = CacheUtil.newTimedCache(DateUnit.MINUTE.getMillis() * 5);

    static {
        // 每30秒清理一次过期缓存
        CACHE.schedulePrune(DateUnit.SECOND.getMillis() * 30);
        CACHE.setListener((key, value) -> {
            log.debug("CacheHelper缓存过期: {}", key);
        });
    }

    public static void put(String key, Object value, long second) {
        CACHE.put(key, value, DateUnit.SECOND.getMillis() * second);
    }

    /**
     * 获取并重置key过期时间
     * @param key
     * @return
     */
    public static Object getAndRefresh(String key) {
        return CACHE.get(key, true);
    }

    public static Object get(String key, boolean updateAccess) {
        return CACHE.get(key, updateAccess);
    }

    public static boolean exist(String key) {
        return CACHE.containsKey(key);
    }

    public static void remove(String key) {
        CACHE.remove(key);
    }

}
