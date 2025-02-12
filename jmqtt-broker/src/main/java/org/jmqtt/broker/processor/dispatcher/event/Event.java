package org.jmqtt.broker.processor.dispatcher.event;

import lombok.Getter;
import lombok.Setter;
import org.jmqtt.broker.common.helper.BrokerContext;

import java.io.Serializable;

/**
 * cluster event model
 */
@Getter
@Setter
public class Event implements Serializable {

    private static final long serialVersionUID = -12893791131231231L;

    /**
     * {@link EventCode}
     */
    private int eventCode;

    private String body;

    private long sendTime;

    private String fromIp;

    public Event(int eventCode, String body,long sendTime,String fromIp) {
        this.eventCode = eventCode;
        this.body = body;
        this.sendTime = sendTime;
        this.fromIp = fromIp;
    }

    public Event(int eventCode, String body,long sendTime) {
        this.eventCode = eventCode;
        this.body = body;
        this.sendTime = sendTime;
        this.fromIp = BrokerContext.getBrokerId();
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventCode=" + eventCode +
                ", body='" + body + '\'' +
                '}';
    }
}
