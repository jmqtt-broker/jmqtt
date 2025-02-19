package org.jmqtt.common.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Letter<T> {

    private int eventCode;

    private T body;

    private long sendTime;

    private String fromBroker;

}
