package com.jmqtt.core.mqtt.store.persist.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("jmqtt_session")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @TableId
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
