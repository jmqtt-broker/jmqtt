package org.jmqtt.starter.api.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.store.local.LocalDB;
import org.jmqtt.broker.store.local.mapper.LocalBrokerMapper;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.starter.api.entity.PageVo;
import org.jmqtt.starter.api.service.BrokerService;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrokerServiceImpl implements BrokerService {

    private final LocalDB localDb;

    private LocalBrokerMapper getMapper() {
        return this.localDb.getMapper(LocalBrokerMapper.class);
    }

    @Override
    public BrokerDO selectByBrokerId(String brokerId) {
        BrokerDO brokerDO = new BrokerDO();
        brokerDO.setBrokerId(brokerId);
        return getMapper().selectOne(brokerDO);
    }

    @Override
    public PageVo<BrokerDO> page(int page, int pageSize, BrokerDO brokerDO) {
        Example example = new Example(BrokerDO.class);
        Example.Criteria criteria = example.createCriteria();
        String brokerId = brokerDO.getBrokerId();
        if (StringUtils.isNotBlank(brokerId)) {
            criteria.andEqualTo("brokerId", brokerId);
        }
        String ip = brokerDO.getIp();
        if (StringUtils.isNotBlank(ip)) {
            criteria.andLike("ip", "%" + ip + "%");
        }
        Boolean status = brokerDO.getStatus();
        if (status != null) {
            criteria.andEqualTo("status", status);
        }
        example.setOrderByClause("online_at desc");
        Page<BrokerDO> pageInfo = PageHelper.startPage(page, pageSize);
        List<BrokerDO> dataList = getMapper().selectByExample(example);
        return new PageVo<>(dataList, pageInfo.getTotal(), page, pageSize);
    }
}
