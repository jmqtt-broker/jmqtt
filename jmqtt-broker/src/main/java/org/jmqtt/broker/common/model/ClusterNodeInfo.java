package org.jmqtt.broker.common.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"nodeId"})
public class ClusterNodeInfo {

    private String nodeId;

    private Boolean status = false;

    private Long onlineAt;

    private Long offLineAt;

    private Long lastKeepaliveTime;

    public ClusterNodeInfo(String nodeId, Boolean status) {
        this.nodeId = nodeId;
        this.status = status;
        if (status) {
            this.onlineAt = System.currentTimeMillis();
        } else {
            this.offLineAt = System.currentTimeMillis();
        }
    }

}
