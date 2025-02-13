package org.jmqtt.common.event;

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

    /**
     * {@link EventCode}
     */
    private int eventCode;

    private String body;

    private long sendTime;

    private String fromIp;

    public Event(int eventCode, String body, long sendTime, String fromIp) {
        this.eventCode = eventCode;
        this.body = body;
        this.sendTime = sendTime;
        this.fromIp = fromIp;
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventCode=" + eventCode +
                ", body='" + body + '\'' +
                '}';
    }

}
