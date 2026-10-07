package com.jmqtt.core.exception;

public class BrokerException extends RuntimeException{

    public BrokerException() {

    }

    public BrokerException(String message){
        super(message);
    }

}
