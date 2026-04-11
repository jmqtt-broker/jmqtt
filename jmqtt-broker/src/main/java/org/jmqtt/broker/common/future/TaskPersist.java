package org.jmqtt.broker.common.future;

import org.jmqtt.broker.store.local.model.TimerDO;

import java.util.List;

public interface TaskPersist {

    void store(TimerDO timer);

    void del(String taskId);

    List<TimerDO> getAll();

}
