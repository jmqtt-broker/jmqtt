package com.jmqtt.core.mqtt.store.persist.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("jmqtt_subscription")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDO {

    @TableId
    private Long id;

    private String brokerId;

    private String clientId;

    private String topic;

    private Integer qos;

    private Boolean subRes;

    private String opt;
}
