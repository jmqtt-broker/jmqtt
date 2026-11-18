package com.jmqtt.core.mqtt.timer;

import com.jmqtt.core.mqtt.store.persist.po.ScheduleTask;

@FunctionalInterface
public interface TaskHandler {
    void handle(ScheduleTask task) throws Exception;
}
