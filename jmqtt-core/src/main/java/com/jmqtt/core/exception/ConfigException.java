package com.jmqtt.core.exception;

import lombok.Getter;

@Getter
public class ConfigException extends RuntimeException {

    public ConfigException() {
        super();
    }

    public ConfigException(String message) {
        super(message);
    }

    public static ConfigException of(String msg) {
        return new ConfigException(msg);
    }

}
