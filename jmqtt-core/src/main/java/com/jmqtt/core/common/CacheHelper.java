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
    private final static TimedCache<String, Object> ssoTokenCache = CacheUtil.newTimedCache(DateUnit.MINUTE.getMillis() * 5);

    static {
        // 每30秒清理一次过期缓存
        ssoTokenCache.schedulePrune(DateUnit.SECOND.getMillis() * 30);
        ssoTokenCache.setListener((key, value) -> {
            log.info("CacheHelper缓存过期: {}", key);

        });
    }

    public static void put(String key, Object value, long second) {
        ssoTokenCache.put(key, value, DateUnit.SECOND.getMillis() * second);
    }

    /**
     * 获取并重置key过期时间
     * @param key
     * @return
     */
    public static Object getAndRefresh(String key) {
        return ssoTokenCache.get(key, true);
    }

    public static Object get(String key, boolean updateAccess) {
        return ssoTokenCache.get(key, updateAccess);
    }

    public static void remove(String key) {
        ssoTokenCache.remove(key);
    }

}
