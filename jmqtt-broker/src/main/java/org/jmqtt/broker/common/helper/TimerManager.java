package org.jmqtt.broker.common.helper;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TimerManager {

    private final static Map<TimerType, Consumer<Object>> timeoutHook = new HashMap<>();

    static {
        timeoutHook.put(TimerType.SESSION, obj -> TimerManager.sessionTimeout((String) obj));
        timeoutHook.put(TimerType.WILL, obj -> TimerManager.willTimeout((Message) obj));
    }

    private static void start(TimerBO task) {
        int expire = task.getExpire();
        log.info("start delay task: {}, {}, expire: {}", task.getType(), task.getTimerId(), expire);
        ScheduleManager.getInstance().addDelay(task);
    }

    private static void stop(TimerBO task) {
        boolean cancel = ScheduleManager.getInstance().cancel(task);
        if (cancel) {
            log.info("stop delay task:{}, {}", task.getType().name(), task.getTimerId());
        }
    }

    public static void startSessionTimeout(String clientId) {
        int expire = Mqtt5Utils.timeoutSecond(clientId);
        if (expire > 0) {
            TimerBO task = new TimerBO(clientId, TimerType.SESSION, clientId, expire);
            start(task);
        }
    }

    public static void stopSessionTimeout(String clientId) {
        stop(new TimerBO(clientId, TimerType.SESSION));
    }

    public static void sessionTimeoutImmediately(String clientId) {
        executeImmediately(clientId, TimerType.SESSION);
    }

    public static void startWillTimeout(String clientId, Message will) {
        Map<Integer, Object> properties = will.getProperties();
        int willDelay = 0;
        if (properties != null && !properties.isEmpty()) {
            willDelay = (Integer) Optional.ofNullable(properties.get(MqttProperties.MqttPropertyType.WILL_DELAY_INTERVAL.value())).orElse(0);
        }
        TimerBO task = new TimerBO(clientId, TimerType.WILL, will, willDelay);
        start(task);
    }

    public static void stopWillTimeout(String clientId) {
        stop(new TimerBO(clientId, TimerType.WILL));
    }

    public static void sendWillImmediately(String clientId) {
        executeImmediately(clientId, TimerType.WILL);
    }

    private static void executeImmediately(String clientId, TimerType timerType) {
        TimerBO timerData = ScheduleManager.getInstance().getTimerData(new TimerBO(clientId, timerType));
        Optional.ofNullable(timerData).ifPresent(TimerBO::process);
    }

    private static void sessionTimeout(String clientId) {
        log.info("session expired. clientId: {}", clientId);
        BrokerContext.getSessionStore().clearSession(clientId, false);
    }

    private static void willTimeout(Message will) {
        String clientId = will.getClientId();
        log.info("will message published, clientId: {}", clientId);
        BrokerContext.getMessageDispatcher().appendMessage(will);
        Optional.ofNullable(will.getHeader(MessageHeader.RETAIN)).ifPresent(retain -> {
            if ((boolean) retain) {
                log.info("will message store as retain, clientId: {}", clientId);
                BrokerContext.getMessageStore().storeRetainMessage((String) will.getHeader(MessageHeader.TOPIC), will);
            }
        });
        BrokerContext.getMessageStore().clearWillMessage(clientId);
    }

    public static void afterExpire(TimerType type, Object data) {
        Optional.ofNullable(timeoutHook.get(type)).ifPresent(hook -> hook.accept(data));
    }

    public enum TimerType {

        /**
         * 延时任务类型
         */
        SESSION,
        WILL,
        ;
    }

}
