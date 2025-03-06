package org.jmqtt.broker.common.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OfflineMessageDTO {

    private String subClientId;

    private Message message;

    public OfflineMessageDTO(String subClientId, Message message) {
        this.subClientId = subClientId;
        this.message = message;
    }

}
