package org.jmqtt.broker.store.rdb.daoobject;

import com.alibaba.fastjson.JSON;
import lombok.*;
import org.jmqtt.broker.remoting.util.IdWorker;
import org.jmqtt.common.entity.BrokerInfo;

import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"brokerId"})
@Table(name = "jmqtt_broker")
public class BrokerDO {

    @Id
    private Long id;

    private String brokerId;

    private String ip;

    private Integer tcpPort;
    private Integer tcpPortSsl;
    private Integer wsPort;
    private Integer wsPortSsl;

    private Boolean status;

    private Long onlineAt;

    private Long offlineAt;

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
        this.offlineAt = brokerInfo.getOffLineAt();
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
