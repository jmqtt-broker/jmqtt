package com.jmqtt.core.exception;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

public class ConnectException extends BrokerException {

    public ConnectException() {
        super();
    }

    public ConnectException(String message){
        super(message);
    }

    public ConnectException(MqttConnectReturnCode reasonCode, String reasonString) {
        super(reasonCode, reasonString);
    }

    public static ConnectException of(MqttConnectReturnCode reasonCode, String reasonString) {
        return new ConnectException(reasonCode, reasonString);
    }

}
