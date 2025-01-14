package org.jmqtt.broker.common.helper;

import lombok.*;

import java.util.function.BiConsumer;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = {"timerId", "type"})
@ToString
public class TimerBO {

    private String timerId;

    private TimerManager.TimerType type;

    private Object data;

    private int expire;

    private BiConsumer<String, Object> expiredFunc;

}
