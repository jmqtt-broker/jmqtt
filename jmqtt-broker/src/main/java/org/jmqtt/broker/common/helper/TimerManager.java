package org.jmqtt.broker.common.helper;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/13 10:40
 */
@Slf4j
public class TimerManager {

    private static final Map<TimerType, BiConsumer<String, Object>> OBSERVERS = new ConcurrentHashMap<>();

    static {
        CaffeineUtil.addRemoveListener((k, v) -> {
            log.info("key: {}, vlaue: {} expired.", k, v);
            OBSERVERS.forEach((type, consumer) -> {
                if (k.startsWith(type.name())) {
                    consumer.accept(k, v);
                }
            });
        });
    }

    public static void start(TimerBO task, BiConsumer<String, Object> consumer) {
        String key = task.getType() + ":" + task.getTimerId();
        int expire = task.getExpire();
        log.info("start delay task: {}, expire: {}", key, expire);
        CaffeineUtil.put(key, task.getData(), expire);
        OBSERVERS.putIfAbsent(task.getType(), consumer);
    }

    public static void stop(String key) {
        if (CaffeineUtil.get(key) != null) {
            log.info("stop delay task:{}", key);
            CaffeineUtil.del(key);
        }
    }

    public static void startSessionTimeout(String clientId, BiConsumer<String, Object> consumer) {
        int expire = Mqtt5Utils.timeoutSecond(clientId);
        if (expire > 0) {
            TimerBO task = new TimerBO(clientId, TimerType.SESSION, clientId, expire);
            start(task, consumer);
        }
    }

    public static void stopSessionTimeout(String clientId) {
        stop(TimerType.SESSION.name() + ":" + clientId);
    }

    public static void startWillTimeout(String clientId, Message will, BiConsumer<String, Object> consumer) {
        Map<Integer, Object> properties = will.getProperties();
        if (properties != null && !properties.isEmpty()) {
            Integer willDelay = (Integer) properties.get(MqttProperties.MqttPropertyType.WILL_DELAY_INTERVAL.value());
            if (willDelay != null) {
                TimerBO task = new TimerBO(clientId, TimerType.WILL, will, willDelay);
                start(task, consumer);
            }
        }
    }

    public static void sendWillImmediately(String clientId) {
        String key = TimerType.WILL.name() + ":" + clientId;
        Object val = CaffeineUtil.get(key);
        if (val != null) {
            CaffeineUtil.put(key, val, 0);
        }
    }

    public static void stopWillTimeout(String clientId) {
        stop(TimerType.WILL.name() + ":" + clientId);
    }

    enum TimerType {

        /**
         * 延时任务类型
         */
        SESSION,
        RETAIN,
        WILL

    }

}
