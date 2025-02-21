package org.jmqtt.broker.store.local;

import com.alibaba.fastjson.JSON;
import org.jmqtt.broker.remoting.util.IdWorker;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.local.mapper.LocalBrokerMapper;
import org.jmqtt.broker.store.local.mapper.LocalSessionMapper;
import org.jmqtt.broker.store.rdb.DBCallback;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;

import java.util.List;
import java.util.Optional;

public class LocalStoreImpl implements LocalStore {

    @Override
    public void start() {
        LocalDB.getInstance().start();
    }

    protected Object operate(DBCallback callback) {
        return LocalDB.getInstance().operate(callback);
    }

    @Override
    public BrokerDO getBroker(String brokerId) {
        return (BrokerDO) operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).getBroker(brokerId));
    }

    @Override
    public List<BrokerDO> getAll() {
        return (List<BrokerDO>) operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).getAll());
    }

    @Override
    public boolean storeBroker(BrokerDO broker) {
        return operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).storeBroker(broker)) != null;
    }

    @Override
    public boolean del(String brokerId) {
        return operate(sqlSession -> sqlSession.getMapper(LocalBrokerMapper.class).del(brokerId)) != null;
    }

    @Override
    public boolean storeSession(SessionState sessionState) {
        SessionDO sessionDO = new SessionDO();
        sessionDO.setId(IdWorker.getId());
        sessionDO.setBrokerId(sessionState.getBrokerId());
        sessionDO.setClientId(sessionState.getClientId());
        sessionDO.setState(sessionState.getState().getCode());
        sessionDO.setOfflineTime(sessionState.getOfflineTime());
        sessionDO.setVersion(sessionState.getVersion());
        Optional.ofNullable(sessionState.getPropertyMap()).ifPresent(p -> sessionDO.setProperty(JSON.toJSONString(p)));
        return operate(sqlSession -> sqlSession.getMapper(LocalSessionMapper.class).storeSession(sessionDO)) != null;
    }
}
