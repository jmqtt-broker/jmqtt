package org.jmqtt.common.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"brokerId"})
public class BrokerInfo {

    private String brokerId;

    private String ip;

    private Integer tcpPort;
    private Integer tcpPortSsl;
    private Integer wsPort;
    private Integer wsPortSsl;

    private Boolean status;

    private Long onlineAt;

    private Long offLineAt;

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
