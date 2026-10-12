package com.jmqtt.core.exception;

import com.jmqtt.core.mqtt.helper.Reason;
import lombok.Getter;

@Getter
public class BrokerException extends RuntimeException {

    private Reason reason;

    public BrokerException() {
        super();
    }

    public BrokerException(String message) {
        super(message);
    }

    public BrokerException(Reason reason) {
        super(reason.getReasonString());
        this.reason = reason;
    }

    public static BrokerException of(Reason reason) {
        return new BrokerException(reason);
    }

}
