package org.jmqtt.starter.api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.model.Message;

@Getter
@Setter
@NoArgsConstructor
public class MessageVo extends Message {

    private String payloadStr;

    public MessageVo(Message message) {
        MixAll.copyProperties(message, this);
        this.payloadStr = new String(message.getPayload());
    }

}
