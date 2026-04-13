package org.jmqtt.broker.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2026/4/13 14:41
 */
@Getter
@Setter
@AllArgsConstructor
public class MessageWapper {

    private Message message;

    // 要发送给该broker
    private String targetBrokerId;

}
