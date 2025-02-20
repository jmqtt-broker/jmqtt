package org.jmqtt.common.event;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * cluster event model
 */
@Getter
@Setter
@NoArgsConstructor
public class Event implements Serializable {

    private static final long serialVersionUID = -12893791131231231L;

    private int eventCode;

    private Object body;

    private long sendTime;

    private String fromBroker;

    public Event(int eventCode, Object body, long sendTime, String fromBroker) {
        this.eventCode = eventCode;
        this.body = body;
        this.sendTime = sendTime;
        this.fromBroker = fromBroker;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

}
