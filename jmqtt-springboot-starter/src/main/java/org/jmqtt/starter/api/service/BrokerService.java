package org.jmqtt.starter.api.service;

import org.jmqtt.broker.common.helper.Pair;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.starter.api.entity.PageVo;

import java.util.List;

public interface BrokerService {

    BrokerDO selectByBrokerId(String brokerId);

    List<Pair<String, String>> brokerList();

    PageVo<BrokerDO> page(int page, int pageSize, BrokerDO brokerDO);

}
