package org.jmqtt.starter.api.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.broker.store.redis.support.RedisKeySupport;
import org.jmqtt.broker.store.redis.support.RedisUtils;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.starter.api.entity.PageVo;
import org.jmqtt.starter.api.service.SessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * redis存储时，条件查询及分页等功能实现较困难，该情况暂不实现相关api
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "jmqtt.broker", name = "store", havingValue = JmqttConst.REDIS)
@RequiredArgsConstructor
public class RedisSessionServiceImpl implements SessionService {

    private final RedisUtils redisUtils;

    @Override
    public SessionDO selectByClientId(String clientId) {
        String sessionStr = redisUtils.getOperator().get(RedisKeySupport.SESSION + clientId);
        if (sessionStr != null) {
            return JSONObject.parseObject(sessionStr, SessionDO.class);
        }
        return null;
    }

    @Override
    public PageVo<SessionDO> page(int page, int pageSize, SessionDO sessionDO) {
        return null;
    }
}
