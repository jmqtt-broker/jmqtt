package org.jmqtt.broker.common.helper;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.common.helper.ThreadFactoryImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class ScheduleManager {

    private static final HashedWheelTimer SCHEDULER = new HashedWheelTimer(
            new ThreadFactoryImpl("JMQTT_Schedule_thread"),
            100,
            TimeUnit.MILLISECONDS,
            512,
            true
    );

    private static final Map<TimerBO, Timeout> CALLBACK_MAP = new ConcurrentHashMap<>();

    /**
     * 添加定时任务
     *
     * @param timerBO 任务
     */
    public static void addScheduled(TimerBO timerBO) {
        Timeout expired = SCHEDULER.newTimeout(timeout -> {
            try {
                timerBO.process();
            } catch (Exception e) {
                log.error("scheduled error.", e);
            }
            Timeout nextTimeout = SCHEDULER.newTimeout(timeout.task(), timerBO.getExpire(), TimeUnit.SECONDS);
            CALLBACK_MAP.put(timerBO, nextTimeout);
        }, timerBO.getExpire(), TimeUnit.SECONDS);
        CALLBACK_MAP.put(timerBO, expired);
    }

    /**
     * 添加延时任务
     *
     * @param timerBO 任务
     */
    public static void addDelay(TimerBO timerBO) {
        Timeout expired = SCHEDULER.newTimeout(timeout -> {
            CALLBACK_MAP.remove(timerBO);
            timerBO.process();
        }, timerBO.getExpire(), TimeUnit.SECONDS);
        CALLBACK_MAP.put(timerBO, expired);
    }

    public static void addSimpleDelay(Consumer<Timeout> execute, long seconds) {
        SCHEDULER.newTimeout(execute::accept, seconds, TimeUnit.SECONDS);
    }

    /**
     * 取消任务
     *
     * @param timerBO 任务
     * @return 成功失败
     */
    public static boolean cancel(TimerBO timerBO) {
        Timeout timeout = CALLBACK_MAP.remove(timerBO);
        if (timeout != null) {
            return timeout.cancel();
        }
        return false;
    }

    public static void executeImmediately(TimerBO timerBO) {
        Timeout timeout = CALLBACK_MAP.remove(timerBO);
        try {
            if (timeout != null && !timeout.isCancelled()) {
                timeout.task().run(timeout);
                timeout.cancel();
            }
        } catch (Exception e) {
            log.error("executeImmediately error.", e);
        }
    }

}
