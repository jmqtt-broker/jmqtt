package org.jmqtt.broker.store.rdb.daoobject;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
public class SubscriptionDO implements Serializable {

    private static final long serialVersionUID = 12213131231231L;

    private Long id;

    private String clientId;

    private String topic;

    private Integer qos;

    private String opt;
}
