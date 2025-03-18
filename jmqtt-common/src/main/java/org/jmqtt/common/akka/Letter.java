package org.jmqtt.common.akka;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jmqtt.common.event.Event;

@Getter
@Setter
@NoArgsConstructor
public class Letter {

    private Event message;

    // 如果需要给回复，向这个actor回复
    private String responsePath;

    // 这是一个从keeper节点同步过来的消息
    private Boolean sync;

    public Letter(Event message, String responsePath) {
        this.message = message;
        this.responsePath = responsePath;
    }

    public Letter(Event message) {
        this.message = message;
    }

    public boolean isSync() {
        return this.sync != null && sync;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
