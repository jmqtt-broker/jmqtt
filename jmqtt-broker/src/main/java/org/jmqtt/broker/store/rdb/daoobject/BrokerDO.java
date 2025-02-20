package org.jmqtt.broker.store.rdb.daoobject;

import lombok.*;
import org.jmqtt.broker.remoting.util.IdWorker;
import org.jmqtt.common.entity.BrokerInfo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"brokerId"})
public class BrokerDO {

    private Long id;

    private String brokerId;

    private String ip;

    private int tcpPort;
    private int tcpPortSsl;
    private int wsPort;
    private int wsPortSsl;

    private Boolean status;

    private Long onlineAt;

    private Long offLineAt;

    public BrokerDO(BrokerInfo brokerInfo) {
        this.id = IdWorker.getId();
        this.brokerId = brokerInfo.getBrokerId();
        this.ip = brokerInfo.getIp();
        this.tcpPort = brokerInfo.getTcpPort();
        this.tcpPortSsl = brokerInfo.getTcpPortSsl();
        this.wsPort = brokerInfo.getWsPort();
        this.wsPortSsl = brokerInfo.getWsPortSsl();
        this.status = brokerInfo.getStatus();
        this.onlineAt = brokerInfo.getOnlineAt();
        this.offLineAt = brokerInfo.getOffLineAt();
    }

}
