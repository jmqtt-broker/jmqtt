package org.jmqtt.broker.common.future;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public abstract class Req {

    protected String identify;

    public Req() {
        this.identify = UUID.randomUUID().toString();
    }

    public Req(String identify) {
        this.identify = identify;
    }

}
