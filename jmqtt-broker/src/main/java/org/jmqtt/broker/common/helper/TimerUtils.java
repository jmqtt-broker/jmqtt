package org.jmqtt.broker.common.helper;

import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.future.PersistHelper;
import org.jmqtt.broker.common.future.ScheduleTask;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.processor.dispatcher.akka.ClusterHelper;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.local.model.TimerDO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class TimerUtils {

    private final static Map<String, ScheduleTask> taskMap = new ConcurrentHashMap<>(128);

    // 超时任务的回调
    private final static Map<String, Consumer<Object>> timeoutHook = new ConcurrentHashMap<>();

    static {
        timeoutHook.put(TimerType.SESSION.name(), obj -> {
            startSessionTimeout((String) obj);
            String clientId = (String) obj;
            sessionTimeout(clientId);
            removeTask(TimerType.SESSION.name() + clientId);
        });
        timeoutHook.put(TimerType.WILL.name(), obj -> {
            Message will = (Message) obj;
            willTimeout(will);
            removeTask(TimerType.WILL.name() + will.getClientId());
        });
    }

    public static void startSessionTimeout(String clientId) {
        int expire = Mqtt5Utils.timeoutSecond(clientId);
        if (expire > 0) {
            String taskId = TimerType.SESSION.name() + clientId;
            ScheduleTask sessionTimeout = ScheduleTask.delayTask(taskId, timeout -> {
                sessionTimeout(clientId);
                removeTask(taskId);
            }, expire).persist(true, clientId, TimerType.SESSION.name(), true);
            addTask(sessionTimeout);
        }
    }

    public static void stopSessionTimeout(String clientId) {
        Optional.ofNullable(removeTask(TimerType.SESSION.name() + clientId)).ifPresent(ScheduleTask::close);
    }

    public static void sessionTimeoutImmediately(String clientId) {
        Optional.ofNullable(removeTask(TimerType.SESSION.name() + clientId)).ifPresent(ScheduleTask::executeImmediately);
    }

    public static void startWillTimeout(String clientId, Message will) {
        Map<Integer, Object> properties = will.getProperties();
        int willDelay = 0;
        if (properties != null && !properties.isEmpty()) {
            willDelay = (Integer) Optional.ofNullable(properties.get(MqttProperties.MqttPropertyType.WILL_DELAY_INTERVAL.value())).orElse(0);
        }
        String taskId = TimerType.WILL.name() + clientId;
        ScheduleTask willTimeout = ScheduleTask.delayTask(taskId, timeout -> {
            willTimeout(will);
            removeTask(taskId);
        }, willDelay).persist(true, will, TimerType.WILL.name(), true);
        addTask(willTimeout);
    }

    public static void stopWillTimeout(String clientId) {
        Optional.ofNullable(removeTask(TimerType.WILL.name() + clientId)).ifPresent(ScheduleTask::close);
    }

    public static void sendWillImmediately(String clientId) {
        Optional.ofNullable(removeTask(TimerType.WILL.name() + clientId)).ifPresent(ScheduleTask::executeImmediately);
    }

    private static void sessionTimeout(String clientId) {
        log.info("session expired. clientId: {}", clientId);
        SessionState session = BrokerContext.getSessionStore().getSession(clientId);
        BrokerContext.getSessionStore().clearSession(clientId, true);
        SessionState sessionState = new SessionState(clientId, SessionState.StateEnum.NULL);
        sessionState.setCleanStart(true);
        ClusterHelper.sendSessionToCluster(sessionState);
    }

    private static void willTimeout(Message will) {
        String clientId = will.getClientId();
        log.info("will message published, clientId: {}", clientId);
        BrokerContext.getMessageDispatcher().appendMessage(will);
        Optional.ofNullable(will.getHeader(MessageHeader.RETAIN)).ifPresent(retain -> {
            if ((boolean) retain) {
                log.info("will message store as retain, clientId: {}", clientId);
                BrokerContext.getMessageStore().retainHandle(will);
            }
        });
        BrokerContext.getMessageStore().clearWillMessage(clientId);
    }

    public static void addTask(ScheduleTask task) {
        taskMap.put(task.getTaskId(), task);
    }

    public static ScheduleTask removeTask(String taskId) {
        return taskMap.remove(taskId);
    }

    public static void restoreTask() {
        // 系统故障或重启，从本地恢复重启前的调度任务
        List<TimerDO> tasks = PersistHelper.getAll();
        tasks.forEach(timerDO -> {
            ScheduleTask load = restore(timerDO);
            if (load != null) {
                addTask(load);
            }
        });
    }

    private static ScheduleTask restore(TimerDO timer) {
        long remain = timer.getExpireAt() - System.currentTimeMillis();
        Boolean exec = timer.getExec();
        JSONObject dataObj = JSONObject.parseObject(timer.getData());
        Object obj;
        try {
            Class<?> clazz = Class.forName(dataObj.getString("type"));
            Object data = dataObj.get("data");
            if (data instanceof String) {
                obj = data;
            } else {
                obj = ((JSONObject) data).toJavaObject(clazz);
            }
        } catch (Exception e) {
            log.error("schedule task dirty data.");
            PersistHelper.del(timer.getTimerId());
            return null;
        }
        if (remain > 0) {
            return new ScheduleTask(timer.getTimerId(), timeout -> timeoutHook.get(timer.getType()).accept(obj),
                    remain / 1000, timer.getCycle() ? ScheduleTask.TaskType.TIMED : ScheduleTask.TaskType.DELAY)
                    .persist(false, obj, timer.getType(), timer.getExec()).start();
        } else {
            if (exec != null && exec) {
                log.info("restore task: {}, expired, execute callback.", timer.getTimerId());
                PersistHelper.del(timer.getTimerId());
                return new ScheduleTask(timer.getTimerId(), timeout -> timeoutHook.get(timer.getType()).accept(obj),
                        0, timer.getCycle() ? ScheduleTask.TaskType.TIMED : ScheduleTask.TaskType.DELAY).start();
            }
        }
        return null;
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
