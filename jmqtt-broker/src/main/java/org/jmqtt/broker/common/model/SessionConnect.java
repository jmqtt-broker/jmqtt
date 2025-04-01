package org.jmqtt.broker.common.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SessionConnect {

    private String clientId;

    private Boolean cleanStart;

}
