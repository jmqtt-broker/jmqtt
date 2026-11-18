package com.jmqtt.core.mqtt.timer;

import lombok.Getter;

@Getter
public enum TimerType {

    /**
     * 会话超时
     */
    SESSION,

    /**
     * 遗嘱消息
     */
    WILL,

    /**
     * 延迟发布
     */
    DELAY_PUB,

}
