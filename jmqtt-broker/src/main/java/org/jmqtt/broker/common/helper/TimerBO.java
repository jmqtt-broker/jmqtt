package org.jmqtt.broker.common.helper;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/13 10:13
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = {"timerId", "type"})
public class TimerBO {

    private String timerId;

    private TimerManager.TimerType type;

    private Object data;

    private int expire;

}
