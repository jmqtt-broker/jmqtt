package org.jmqtt.broker.store.rdb.daoobject;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@Getter
@Setter
@Table(name = "jmqtt_subscription")
public class SubscriptionDO implements Serializable {

    private static final long serialVersionUID = 12213131231231L;

    @Id
    private Long id;

    private String clientId;

    private String topic;

    private Integer qos;

    private String opt;
}
