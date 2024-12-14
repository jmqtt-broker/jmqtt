
package org.jmqtt.broker.store.rdb.daoobject;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SessionDO implements Serializable {

    private static final long serialVersionUID = 12213131231231L;

    private Long id;

    private String clientId;

    private String state;

    private Long offlineTime;

    private String property;

}
