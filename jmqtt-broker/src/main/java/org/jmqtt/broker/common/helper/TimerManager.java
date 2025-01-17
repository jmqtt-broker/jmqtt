package org.jmqtt.broker.common.helper;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
public class TimerManager {

    static {
        CaffeineUtil.addRemoveListener((k, v) -> {
            if (v != null && (v.getData() instanceof TimerBO)) {
                TimerBO bo = (TimerBO) v.getData();
                Optional.ofNullable(bo.getExpiredFunc()).ifPresent(func -> func.accept(k, bo.getData()));
            }
        });
    }

    public static void start(TimerBO task) {
        String key = task.getType() + ":" + task.getTimerId();
        int expire = task.getExpire();
        log.info("start delay task: {}, expire: {}", key, expire);
        CaffeineUtil.put(key, task, expire);
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
            TimerBO task = new TimerBO(clientId, TimerType.SESSION, clientId, expire, consumer);
            start(task);
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
        TimerBO task = new TimerBO(clientId, TimerType.WILL, will, willDelay, consumer);
        start(task);
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

    public enum TimerType {

        /**
         * 延时任务类型
         */
        SESSION,
        RETAIN,
        WILL;

    }

}
