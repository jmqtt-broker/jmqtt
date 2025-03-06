package org.jmqtt.broker.common.helper;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.common.helper.CaffeineUtil;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TimerManager {

    static {
        CaffeineUtil.addRemoveListener((k, v) -> {
            if (v != null && (v.getData() instanceof TimerBO)) {
                TimerBO bo = (TimerBO) v.getData();
                bo.process();
            }
        });
    }

    public static void start(TimerBO task) {
        String key = task.getType() + ":" + task.getTimerId();
        int expire = task.getExpire();
        log.info("start delay task: {}, expire: {}", key, expire);
        // CaffeineUtil.put(key, task, expire);
        ScheduleManager.addDelay(task);
    }

    public static void stop(TimerBO task) {
        boolean cancel = ScheduleManager.cancel(task);
        if (cancel) {
            String key = task.getType().name() + ":" + task.getTimerId();
            log.info("stop delay task:{}", key);
        }
        /*String key = task.getType().name() + ":" + task.getTimerId();
        if (CaffeineUtil.get(key) != null) {
            log.info("stop delay task:{}", key);
            CaffeineUtil.del(key);
        }*/
    }

    public static void startSessionTimeout(String clientId, Consumer<TimerBO> consumer) {
        int expire = Mqtt5Utils.timeoutSecond(clientId);
        if (expire > 0) {
            TimerBO task = new TimerBO(clientId, TimerType.SESSION, clientId, expire, consumer);
            start(task);
        }
    }

    public static void stopSessionTimeout(String clientId) {
        stop(new TimerBO(clientId, TimerType.SESSION));
    }

    public static void startWillTimeout(String clientId, Message will, Consumer<TimerBO> consumer) {
        Map<Integer, Object> properties = will.getProperties();
        int willDelay = 0;
        if (properties != null && !properties.isEmpty()) {
            willDelay = (Integer) Optional.ofNullable(properties.get(MqttProperties.MqttPropertyType.WILL_DELAY_INTERVAL.value())).orElse(0);
        }
        TimerBO task = new TimerBO(clientId, TimerType.WILL, will, willDelay, consumer);
        start(task);
    }

    public static void sessionTimeoutImmediately(String clientId) {
        ScheduleManager.executeImmediately(new TimerBO(clientId, TimerType.SESSION));
    }

    public static void stopWillTimeout(String clientId) {
        stop(new TimerBO(clientId, TimerType.WILL));
    }

    public static void sendWillImmediately(String clientId) {
        ScheduleManager.executeImmediately(new TimerBO(clientId, TimerType.WILL));
        /*String key = TimerType.WILL.name() + ":" + clientId;
        Object val = CaffeineUtil.get(key);
        if (val != null) {
            CaffeineUtil.put(key, val, 0);
        }*/
    }

    public enum TimerType {

        /**
         * 延时任务类型
         */
        SESSION,
        RETAIN,
        WILL,
        BROKER_KEEPALIVE,
        BROKER_KEEPALIVE_DETECT,
        DEFAULT
        ;

    }

}
