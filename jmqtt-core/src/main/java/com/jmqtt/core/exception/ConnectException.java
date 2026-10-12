package com.jmqtt.core.exception;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.Getter;

@Getter
public class ConnectException extends BrokerException {

    private MqttConnectReturnCode reasonCode;

    private String reasonString;

    public ConnectException() {
        super();
    }

    public ConnectException(String message){
        super(message);
    }

    public ConnectException(MqttConnectReturnCode reasonCode, String reasonString) {
        this.reasonCode = reasonCode;
        this.reasonString = reasonString;
    }

    public static ConnectException of(MqttConnectReturnCode reasonCode, String reasonString) {
        return new ConnectException(reasonCode, reasonString);
    }

}
