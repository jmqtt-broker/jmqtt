package org.jmqtt.broker.common.helper;

import com.alibaba.fastjson.JSONObject;
import io.netty.util.Timeout;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.store.local.model.TimerDO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"timerId", "type"})
@ToString
@Slf4j
public class TimerBO {

    private String timerId;

    private TimerUtils.TimerType type;

    private Object data;

    private int expire;

    private transient Timeout timeout;

    private long expireAt;

    private Boolean cycle;

    private Boolean exec;

    public TimerBO(String timerId, TimerUtils.TimerType type) {
        this.timerId = timerId;
        this.type = type;
    }

    public TimerBO(String timerId, TimerUtils.TimerType type, Object data, int expire) {
        this.timerId = timerId;
        this.type = type;
        this.data = data;
        this.expire = expire;
        this.expireAt = System.currentTimeMillis() + this.expire * 1000;
    }

    public TimerBO(TimerDO timerDO) {
        this.timerId = timerDO.getTimerId();
        this.type = TimerUtils.TimerType.valueOf(timerDO.getType());
        this.expire = timerDO.getExpire();
        this.expireAt = timerDO.getExpireAt();
        JSONObject dataObj = JSONObject.parseObject(timerDO.getData());
        try {
            Class<?> clazz = Class.forName(dataObj.getString("type"));
            Object data = dataObj.get("data");
            if (data instanceof String) {
                this.data = data;
            } else {
                this.data = ((JSONObject) data).toJavaObject(clazz);
            }
        } catch (ClassNotFoundException e) {
            log.error("schedule task dirty data.");
        }
        this.cycle = timerDO.getCycle();
        this.exec = timerDO.getExec();
    }

    public void process() {
        TimerUtils.afterExpire(this.type, this.data);
    }

}
