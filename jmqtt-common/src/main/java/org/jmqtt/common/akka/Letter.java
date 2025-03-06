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

    public Letter(Event message, String responsePath) {
        this.message = message;
        this.responsePath = responsePath;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
