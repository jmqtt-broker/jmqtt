package com.jmqtt.core.exception;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BrokerException extends RuntimeException {

    private MqttConnectReturnCode reasonCode;

    private String reasonString;

    public BrokerException() {
        super();
    }

    public BrokerException(String message) {
        super(message);
    }

    public static BrokerException of(MqttConnectReturnCode reasonCode, String reasonString) {
        BrokerException exception = new BrokerException();
        exception.reasonCode = reasonCode;
        exception.reasonString = reasonString;
        return exception;
    }

}
