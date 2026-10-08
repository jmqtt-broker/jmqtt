
package com.jmqtt.core.mqtt.store;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionDO {

    private String clientId;

    private String brokerId;

    private String state;

    private Long onlineTime;

    private Long offlineTime;

    private String property;

    private Integer version;

    private String address;

    private Boolean cleanStart;

    private Integer keepalive;

}
