package org.jmqtt.starter.api.service;

import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.starter.api.entity.PageVo;

public interface BrokerService {

    BrokerDO selectByBrokerId(String brokerId);

    PageVo<BrokerDO> page(int page, int pageSize, BrokerDO brokerDO);

}
