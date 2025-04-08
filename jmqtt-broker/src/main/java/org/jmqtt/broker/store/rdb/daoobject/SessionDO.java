
package org.jmqtt.broker.store.rdb.daoobject;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "jmqtt_session")
@Getter
@Setter
public class SessionDO implements Serializable {

    private static final long serialVersionUID = 12213131231231L;

    @Id
    private Long id;

    private String brokerId;

    private String clientId;

    private String state;

    private Long onlineTime;

    private Long offlineTime;

    private String property;

    private Integer version;

    private String address;

}
