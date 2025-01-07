package org.jmqtt.broker.common.helper;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/13 10:40
 */
@Slf4j
public class TimerManager {

    private static final Map<TimerType, Map<String, BiConsumer<String, Object>>> OBSERVERS = new ConcurrentHashMap<>();

    static {
        CaffeineUtil.addRemoveListener((k, v) -> {
            TimerBO timer = (TimerBO) v.getData();
            log.info("key: {}, vlaue: {} expired.", k, timer);
            Optional.ofNullable(OBSERVERS.get(timer.getType())).ifPresent(consumerMap -> {
                Optional.ofNullable(consumerMap.get(k)).ifPresent(consumer -> consumer.accept(k, timer.getData()));
                consumerMap.remove(k);
            });
        });
    }

    public static void start(TimerBO task, BiConsumer<String, Object> consumer) {
        String key = task.getType() + ":" + task.getTimerId();
        int expire = task.getExpire();
        log.info("start delay task: {}, expire: {}", key, expire);
        CaffeineUtil.put(key, task, expire);
        Map<String, BiConsumer<String, Object>> consumerMap = OBSERVERS.get(task.getType());
        if (consumerMap == null) {
            synchronized (OBSERVERS) {
                consumerMap = OBSERVERS.get(task.getType());
                if (consumerMap == null) {
                    consumerMap = new ConcurrentHashMap<>();
                    OBSERVERS.put(task.getType(), consumerMap);
                }
            }
        }
        consumerMap.put(key, consumer);
    }

    public static void stop(String key) {
        TimerBO timer;
        if ((timer = (TimerBO) CaffeineUtil.get(key)) != null) {
            log.info("stop delay task:{}", key);
            CaffeineUtil.del(key);
            Optional.ofNullable(OBSERVERS.get(timer.getType())).ifPresent(consumerMap -> consumerMap.remove(key));
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
        int willDelay = 0;
        if (properties != null && !properties.isEmpty()) {
            willDelay = (Integer) Optional.ofNullable(properties.get(MqttProperties.MqttPropertyType.WILL_DELAY_INTERVAL.value())).orElse(0);
        }
        TimerBO task = new TimerBO(clientId, TimerType.WILL, will, willDelay);
        start(task, consumer);
    }

    public static void stopWillTimeout(String clientId) {
        stop(TimerType.WILL.name() + ":" + clientId);
    }

    public static void sendWillImmediately(String clientId) {
        String key = TimerType.WILL.name() + ":" + clientId;
        Object val = CaffeineUtil.get(key);
        if (val != null) {
            CaffeineUtil.put(key, val, 0);
        }
    }

    public static void startHeartbeat(String clientId, int expire, BiConsumer<String, Object> consumer) {
        TimerBO task = new TimerBO(clientId, TimerType.KEEPALIVE, clientId, expire);
        start(task, consumer);
    }

    public static void stopHeartbeat(String clientId) {
        stop(TimerType.KEEPALIVE.name() + ":" + clientId);
    }

    public static void resetTimerTask(TimerType type, String clientId) {
        CaffeineUtil.get(type.name() + ":" + clientId);
    }

    public enum TimerType {

        /**
         * 延时任务类型
         */
        SESSION,
        RETAIN,
        WILL,
        KEEPALIVE;

        private static final Map<String, TimerType> VALUE_MAP = new HashMap<>();

        static {
            for (TimerType type : values()) {
                VALUE_MAP.put(type.name(), type);
            }
        }

        public static TimerType getType(String name) {
            return VALUE_MAP.get(name);
        }
    }

}
