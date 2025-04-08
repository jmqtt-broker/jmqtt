package org.jmqtt.broker.store.local.model;

import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.jmqtt.broker.common.helper.TimerBO;

import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"timerId", "type"})
@ToString
@Table(name = "schedule_task")
public class TimerDO {

    private String timerId;

    private String type;

    private String data;

    private int expire;

    private long expireAt;

    private Boolean cycle;

    private Boolean exec;

    public TimerDO(TimerBO timerBO) {
        this.timerId = timerBO.getTimerId();
        this.type = timerBO.getType().name();
        this.expire = timerBO.getExpire();
        this.expireAt = timerBO.getExpireAt();
        Object data = timerBO.getData();
        JSONObject obj = new JSONObject();
        obj.put("type", data.getClass());
        obj.put("data", data);
        this.data = obj.toJSONString();
        this.cycle = timerBO.getCycle() != null ? timerBO.getCycle() : false;
        this.exec = timerBO.getExec() != null ? timerBO.getExec() : true;
    }
}
