package org.jmqtt.broker.common.helper;

import lombok.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"timerId", "type"})
@ToString
public class TimerBO {

    private String timerId;

    private TimerManager.TimerType type;

    private Object data;

    private int expire;

    private Consumer<TimerBO> timeoutConsumer;

    public TimerBO(String timerId, TimerManager.TimerType type) {
        this.timerId = timerId;
        this.type = type;
    }

    public void process() {
        if (this.timeoutConsumer != null) {
            this.timeoutConsumer.accept(this);
        }
    }

}
