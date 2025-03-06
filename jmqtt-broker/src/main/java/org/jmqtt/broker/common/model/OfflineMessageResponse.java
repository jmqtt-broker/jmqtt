package org.jmqtt.broker.common.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
public class OfflineMessageResponse {

    private String clientId;

    private Collection<Message> offlineList;

    public OfflineMessageResponse(String clientId, Collection<Message> offlineList) {
        this.clientId = clientId;
        this.offlineList = offlineList;
    }

}
