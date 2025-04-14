package org.jmqtt.broker.store.local;

import org.jmqtt.broker.store.local.mapper.LocalBrokerMapper;
import org.jmqtt.broker.store.rdb.DBCallback;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;

import java.util.List;

public class LocalStoreImpl implements LocalStore {

    @Override
    public void start() {
        LocalDB.getInstance().start();
    }

    protected <R> R operate(DBCallback<R> callback) {
        return LocalDB.getInstance().operate(callback);
    }

    @Override
    public BrokerDO getBroker(String brokerId) {
        return operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).getBroker(brokerId));
    }

    @Override
    public List<BrokerDO> getAll() {
        return operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).getAll());
    }

    @Override
    public boolean storeBroker(BrokerDO broker) {
        return operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).storeBroker(broker)) != null;
    }

    @Override
    public boolean del(String brokerId) {
        return operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).del(brokerId)) != null;
    }

}
