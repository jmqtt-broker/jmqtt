package org.jmqtt.broker.store.local;

import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;

import java.util.List;

public interface LocalStore {

    void start();

    boolean storeBroker(BrokerDO broker);

    BrokerDO getBroker(String brokerId);

    List<BrokerDO> getAll();

    boolean del(String brokerId);

}
