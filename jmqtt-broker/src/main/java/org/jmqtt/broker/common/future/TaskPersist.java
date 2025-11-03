package org.jmqtt.broker.common.future;

import org.jmqtt.broker.store.local.model.TimerDO;

import java.util.List;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2025/11/2 9:56
 */
public interface TaskPersist {

    void store(TimerDO timer);

    void del(String taskId);

    List<TimerDO> getAll();

}
