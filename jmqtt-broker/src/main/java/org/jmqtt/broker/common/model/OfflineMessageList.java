package org.jmqtt.broker.common.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
public class OfflineMessageList {

    private String clientId;

    private Collection<Message> offlineList;

    public OfflineMessageList(String clientId, Collection<Message> offlineList) {
        this.clientId = clientId;
        this.offlineList = offlineList;
    }

}
