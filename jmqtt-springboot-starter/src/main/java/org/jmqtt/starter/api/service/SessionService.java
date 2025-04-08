package org.jmqtt.starter.api.service;

import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.starter.api.entity.PageVo;

public interface SessionService {

    SessionDO selectByClientId(String clientId);

    PageVo<SessionDO> page(int page, int pageSize, SessionDO sessionDO);

}
