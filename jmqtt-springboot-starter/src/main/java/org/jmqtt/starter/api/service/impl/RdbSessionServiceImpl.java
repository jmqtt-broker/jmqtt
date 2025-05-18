package org.jmqtt.starter.api.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.store.rdb.DBUtils;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.broker.store.rdb.daoobject.SubscriptionDO;
import org.jmqtt.broker.store.rdb.daoobject.WillMessageDO;
import org.jmqtt.broker.store.rdb.mapper.SessionMapper;
import org.jmqtt.broker.store.rdb.mapper.SubscriptionMapper;
import org.jmqtt.broker.store.rdb.mapper.WillMessageMapper;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.starter.api.entity.PageVo;
import org.jmqtt.starter.api.service.SessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "jmqtt.broker", name = "store", havingValue = JmqttConst.RDB)
@RequiredArgsConstructor
public class RdbSessionServiceImpl implements SessionService {

    private final DBUtils dbUtils;

    @Override
    public SessionDO selectByClientId(String clientId) {
        SessionDO sessionDO = new SessionDO();
        sessionDO.setClientId(clientId);
        return this.dbUtils.execute(SessionMapper.class, mapper -> mapper.selectOne(sessionDO));
    }

    @Override
    public PageVo<SessionDO> page(int page, int pageSize, SessionDO sessionDO) {
        Example example = new Example(SessionDO.class);
        Example.Criteria criteria = example.createCriteria();
        String brokerId = sessionDO.getBrokerId();
        if (StringUtils.isNotBlank(brokerId)) {
            criteria.andEqualTo("brokerId", brokerId);
        }
        String clientId = sessionDO.getClientId();
        if (StringUtils.isNotBlank(clientId)) {
            criteria.andEqualTo("clientId", clientId);
        }
        String state = sessionDO.getState();
        if (StringUtils.isNotBlank(state)) {
            criteria.andEqualTo("state", state);
        }
        Integer version = sessionDO.getVersion();
        if (version != null) {
            criteria.andEqualTo("version", version);
        }
        example.setOrderByClause("online_time desc");
        Page<SessionDO> pageInfo = PageHelper.startPage(page, pageSize);
        List<SessionDO> dataList = this.dbUtils.execute(SessionMapper.class, mapper -> mapper.selectByExample(example));
        return new PageVo<>(dataList, pageInfo.getTotal(), page, pageSize);
    }

    @Override
    public List<SubscriptionDO> getSubscriptions(String clientId) {
        SubscriptionDO s = new SubscriptionDO();
        s.setClientId(clientId);
        return this.dbUtils.execute(SubscriptionMapper.class, mapper -> mapper.select(s));
    }

    @Override
    public Message getWillMessage(String clientId) {
        WillMessageDO will = new WillMessageDO();
        will.setClientId(clientId);
        WillMessageDO willMessage = this.dbUtils.execute(WillMessageMapper.class, mapper -> mapper.selectOne(will));
        if (willMessage != null) {
            return JSONObject.parseObject(willMessage.getContent(), Message.class);
        }
        return null;
    }
}
