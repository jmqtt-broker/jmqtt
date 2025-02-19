package org.jmqtt.broker.store.rdb.daoobject;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"brokerId"})
public class BrokerDO {

    private String id;

    private String brokerId;

    private String ip;

    private int tcpPort;
    private int tcpPortSsl;
    private int wsPort;
    private int wsPortSsl;

    private Boolean status;

    private Long onlineAt;

    private Long offLineAt;

}
