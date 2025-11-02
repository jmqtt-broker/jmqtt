package org.jmqtt.broker.common.future;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2025/10/14 9:53
 */
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
