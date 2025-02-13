package org.jmqtt.common.helper;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class CacheObject {
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
