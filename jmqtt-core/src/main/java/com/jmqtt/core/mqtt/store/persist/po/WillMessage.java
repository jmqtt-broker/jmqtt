package com.jmqtt.core.mqtt.store.persist.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("jmqtt_will_message")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WillMessage {

    @TableId
    private Long id;

    private String clientId;

    private String content;

    private Long gmtCreate;
}
