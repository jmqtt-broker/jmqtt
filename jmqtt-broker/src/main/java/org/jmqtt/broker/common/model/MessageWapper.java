package org.jmqtt.broker.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MessageWapper {

    private Message message;

    // 要发送给该broker
    private String targetBrokerId;

}
