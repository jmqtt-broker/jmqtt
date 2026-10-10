package com.jmqtt.core.mqtt.store.persist.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("broker_config")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerConfig {

    @TableId
    private Long id;

    private String brokerId;

    private String config;
}
