package com.jmqtt.core.mqtt.timer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.cluster.JmqttCluster;
import com.jmqtt.core.cluster.record.ClusterSessionRecord;
import com.jmqtt.core.mqtt.JmqttContext;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;
import com.jmqtt.core.mqtt.store.MessageStore;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TimerUtil {

    private static ScheduleTaskScheduler scheduler;

    public static synchronized void start() {
        if (scheduler == null) {
            scheduler = new ScheduleTaskScheduler();
        }
        scheduler.start();
        scheduler.registerHandler(TimerType.SESSION, task -> {
            String clientId = task.getData();
            log.info("session timeout, clientId: {}", clientId);
            JmqttContext.getSessionStore().clearSession(clientId, true);
            JmqttCluster.broadcast(ClusterMessage.of(ClusterMessage.EventType.SESSION_DISCONNECT, ClusterSessionRecord.of(clientId, true)));
        });
        scheduler.registerHandler(TimerType.WILL, task -> {
            try {
                log.info("will publish, clientId: {}", task.getTimerId());
                Message willMessage = JSONUtil.toBean(task.getData(), Message.class);
                JmqttContext.getMessageDispatcher().accept(willMessage);
                MessageStore messageStore = JmqttContext.getMessageStore();
                Boolean retain = (Boolean) willMessage.getHeader(MessageHeader.RETAIN);
                if (retain != null && retain) {
                    messageStore.retainHandle(willMessage);
                }
                messageStore.clearWillMessage(willMessage.getClientId());
            } catch (Exception e) {
                log.warn("will timeout and publish failure: {}", task.getData());
            }
        });
        scheduler.registerHandler(TimerType.DELAY_PUB, task -> {
            try {
                log.debug("delay publish: {}", task.getTimerId());
                Message message = JSONUtil.toBean(task.getData(), Message.class);
                JmqttContext.getMessageDispatcher().accept(message);
                MessageStore messageStore = JmqttContext.getMessageStore();
                Boolean retain = (Boolean) message.getHeader(MessageHeader.RETAIN);
                if (retain != null && retain) {
                    messageStore.retainHandle(message);
                }
            } catch (Exception e) {
                log.warn("delay message timeout and publish failure: {}", task.getData());
            }
        });
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler.unregisterHandler(TimerType.SESSION);
            scheduler.unregisterHandler(TimerType.WILL);
            scheduler.unregisterHandler(TimerType.DELAY_PUB);
        }
    }

    public static synchronized ScheduleTaskScheduler getScheduler() {
        if (scheduler == null) {
            scheduler = new ScheduleTaskScheduler();
        }
        return scheduler;
    }

    public static void registerHandler(TimerType type, TaskHandler handler) {
        getScheduler().registerHandler(type, handler);
    }

    public static void unregisterHandler(TimerType type) {
        getScheduler().unregisterHandler(type);
    }

    public static void add(String timerId, TimerType type, long delaySec) {
        add(timerId, type, delaySec, null);
    }

    public static void add(String timerId, TimerType type, long delaySec, String data) {
        if (StrUtil.isBlank(timerId) || type == null || delaySec <= 0) {
            throw new IllegalArgumentException("timerId, type must not be blank, delaySec must > 0");
        }
        getScheduler().create(timerId, type, delaySec, data);
    }

    public static void addCycle(String timerId, TimerType type, long cycleSec, String data) {
        if (StrUtil.isBlank(timerId) || type == null || cycleSec <= 0) {
            throw new IllegalArgumentException("timerId, type must not be blank, cycleSec must > 0");
        }
        getScheduler().create(timerId, type, cycleSec, data, true, (int) cycleSec);
    }

    public static void cancel(String timerId) {
        getScheduler().cancel(timerId);
    }

    public static boolean exists(String timerId) {
        return getScheduler().exists(timerId);
    }

    public static void reschedule(String timerId, long newDelaySec) {
        getScheduler().reschedule(timerId, newDelaySec);
    }

    public static int activeCount() {
        return scheduler != null ? scheduler.activeTaskCount() : 0;
    }
}
