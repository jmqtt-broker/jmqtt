package org.jmqtt.broker.store.local.model;

import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.jmqtt.broker.common.future.ScheduleTask;

import javax.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = {"timerId", "type"})
@ToString
@Table(name = "schedule_task")
public class TimerDO {

    private String timerId;

    private String type;

    private String data;

    private long expire;

    private long expireAt;

    private Boolean cycle;

    private Boolean exec;

    public TimerDO(ScheduleTask task) {
        this.timerId = task.getTaskId();
        this.type = task.getBizType();
        this.expire = task.getDelay();
        this.expireAt = task.getExpireAt();
        this.exec = task.getExec();
        this.cycle = task.timed();
        Object data = task.getData();
        if (data != null) {
            JSONObject obj = new JSONObject();
            obj.put("type", data.getClass());
            obj.put("data", data);
            this.data = obj.toJSONString();
        }
    }

}
