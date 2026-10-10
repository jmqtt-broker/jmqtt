package com.jmqtt.core.mqtt.store.persist.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("jmqtt_retain_message")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetainMessage {

    @TableId
    private Long id;

    private String topic;

    private String content;
}
