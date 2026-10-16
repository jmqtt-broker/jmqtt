package com.jmqtt.core.mqtt.store.persist.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("jmqtt_broker")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Broker {

    @TableId
    private String brokerId;

    private String ip;

    private Integer tcpPort;

    private Integer tcpPortSsl;

    private Integer wsPort;

    private Integer wsPortSsl;

    private Integer status;

    private Long onlineAt;

    private Long offlineAt;
}
