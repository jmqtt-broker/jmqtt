package com.jmqtt.core.mqtt.store.persist.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@TableName("schedule_task")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleTask {

    @TableId
    private String timerId;

    private String type;

    private Long expireAt;

    private Long expire;

    private String data;

    private Integer cycle;

    private Integer exec;
}
