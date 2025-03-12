package org.jmqtt.broker.store.mem;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.common.model.SubscriptionOption;
import org.jmqtt.broker.processor.dispatcher.akka.ClusterHelper;
import org.jmqtt.broker.remoting.util.IdWorker;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.local.LocalDB;
import org.jmqtt.broker.store.local.mapper.LocalOfflineMessageMapper;
import org.jmqtt.broker.store.local.mapper.LocalSessionMapper;
import org.jmqtt.broker.store.local.mapper.LocalSubscriptionMapper;
import org.jmqtt.broker.store.rdb.daoobject.OfflineMessageDO;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.broker.store.rdb.daoobject.SubscriptionDO;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class MemSessionStore extends AbstractMemStore implements SessionStore {
    private static final Object OBJECT = new Object();
    private static final Logger log = JmqttLogger.storeLog;
    /**
     * 离线消息
     */
    private final Map<String,/*clientId*/ BlockingQueue<Message>> offlineTable = new ConcurrentHashMap<>();
    /**
     * 离线消息警告阈值，超过当前数量的客户端会出警告
     */
    private int msgMaxNum = 1000;
    /**
     * 过程消息
     */
    private final Map<String,/*clientId*/ ConcurrentHashMap<Integer,/*msgId*/Message>> recCache = new ConcurrentHashMap<>();
    private final Map<String,/*clientId*/ ConcurrentHashMap<Integer,/*msgId*/Message>> sendCache = new ConcurrentHashMap<>();
    private final Map<String,/*clientId*/ ConcurrentHashMap<Integer,/*msgId*/Object>> secTwoCache = new ConcurrentHashMap<>();

    /**
     * session
     */
    // private Map<String, SessionState> sessionTable = new ConcurrentHashMap<>();
    /**
     * sub
     */
    private final Map<String, ConcurrentHashMap<String, Subscription>> subscriptionCache = new ConcurrentHashMap<>();

    @Override
    public void start(BrokerConfig brokerConfig) {
        super.start(brokerConfig);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public SessionState getSession(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        SessionState s = sessionTable.get(clientId);
        if (s == null) {
            SessionDO sessionDO = (SessionDO) LocalDB.getInstance().operate(sqlSession -> sqlSession.getMapper(LocalSessionMapper.class).getSession(clientId));
            if (sessionDO == null) {
                return new SessionState(SessionState.StateEnum.NULL);
            }
            String property = sessionDO.getProperty();
            s = new SessionState(sessionDO.getBrokerId(), clientId, SessionState.StateEnum.valueOf(sessionDO.getState()),
                    sessionDO.getOfflineTime(),
                    StringUtils.isNotBlank(property) ? new HashMap<Integer, Object>(JSONObject.parseObject(property, Map.class)) : null,
                    sessionDO.getVersion());
            sessionTable.put(clientId, s);
        }
        return s;
    }

    @Override
    public List<SessionDO> getSessionList(Collection<String> clientIds) {
        return (List<SessionDO>) LocalDB.getInstance().operate(sqlSession -> sqlSession.getMapper(LocalSessionMapper.class).getSessionList(clientIds));
    }

    @Override
    public boolean storeSession(String clientId, SessionState sessionState) {
        sessionTable.remove(clientId);
        LocalDB.getInstance().operate(sqlSession -> {
            SessionDO sessionDO = new SessionDO();
            sessionDO.setId(IdWorker.getId());
            sessionDO.setBrokerId(sessionState.getBrokerId());
            sessionDO.setClientId(clientId);
            sessionDO.setState(sessionState.getState().getCode());
            sessionDO.setOfflineTime(sessionState.getOfflineTime());
            sessionDO.setVersion(sessionState.getVersion());
            Optional.ofNullable(sessionState.getPropertyMap()).ifPresent(p -> sessionDO.setProperty(JSON.toJSONString(p)));
            return sqlSession.getMapper(LocalSessionMapper.class).storeSession(sessionDO);
        });
        sessionState.setClientId(clientId);
        return true;
    }

    @Override
    public boolean storeSubscription(String clientId, Subscription subscription) {
        LocalDB.getInstance().operate(sqlSession -> {
            SubscriptionDO subscriptionDO = new SubscriptionDO();
            subscriptionDO.setId(IdWorker.getId());
            subscriptionDO.setClientId(clientId);
            subscriptionDO.setTopic(subscription.getTopic());
            subscriptionDO.setQos(subscription.getQos());
            Optional.ofNullable(subscription.getOption()).ifPresent(opt -> {
                subscriptionDO.setOpt(JSON.toJSONString(opt));
            });
            return sqlSession.getMapper(LocalSubscriptionMapper.class).storeSubscription(subscriptionDO);
        });
        return true;
    }

    @Override
    public boolean delSubscription(String clientId, String topic) {
        LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalSubscriptionMapper.class).delSubscription(clientId, topic)
        );
        return true;
    }

    @Override
    public boolean clearSubscription(String clientId) {
        LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalSubscriptionMapper.class).clearSubscription(clientId)
        );
        // subscriptionCache.remove(clientId);
        return true;
    }

    @Override
    public Set<Subscription> getSubscriptions(String clientId) {
        List<SubscriptionDO> subscriptionDOList = (List<SubscriptionDO>) LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalSubscriptionMapper.class).querySubscription(clientId)
        );
        Set<Subscription> set = new HashSet<>();
        for (SubscriptionDO item : subscriptionDOList) {
            Subscription subscription = new Subscription(item.getClientId(), item.getTopic(), item.getQos());
            Optional.ofNullable(item.getOpt()).ifPresent(opt -> {
                subscription.setOption(JSON.parseObject(opt).toJavaObject(SubscriptionOption.class));
            });
            set.add(subscription);
        }
        return set;
    }

    @Override
    public Subscription getOneSubscription(String clientId, String topic) {
        SubscriptionDO subscriptionDO = (SubscriptionDO) LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalSubscriptionMapper.class).queryOneSubscription(clientId, topic)
        );
        Subscription subscription = new Subscription(subscriptionDO.getClientId(), subscriptionDO.getTopic(), subscriptionDO.getQos());
        Optional.ofNullable(subscriptionDO.getOpt()).ifPresent(opt -> {
            subscription.setOption(JSON.parseObject(opt).toJavaObject(SubscriptionOption.class));
        });
        return subscription;
    }

    @Override
    public boolean cacheInflowMsg(String clientId, Message message) {
        ConcurrentHashMap<Integer, Message> v = recCache.get(clientId);
        if (v == null) {
            synchronized (recCache) {
                v = recCache.get(clientId);
                if (v == null) {
                    v = new ConcurrentHashMap<>();
                    recCache.put(clientId, v);
                }
            }
        }
        v.put(message.getMsgId(), message);
        return true;
    }

    @Override
    public Message releaseInflowMsg(String clientId, Integer msgId) {
        ConcurrentHashMap<Integer, Message> v = recCache.get(clientId);
        if (msgId == null) {
            if (v != null) {
                v.clear();
            }
            return null;
        } else {
            if (v == null) {
                LogUtil.warn(log, "[MemStore] -> The inflow message:{} does not exist", msgId);
                return null;
            }
            return v.remove(msgId);
        }
    }

    @Override
    public Collection<Message> getAllInflowMsg(String clientId) {
        ConcurrentHashMap<Integer, Message> v = recCache.get(clientId);
        if (v == null || v.isEmpty()) {
            return new ArrayList<>();
        }
        return v.values();
    }

    @Override
    public boolean cacheOutflowMsg(String clientId, Message message) {
        ConcurrentHashMap<Integer, Message> v = sendCache.get(clientId);
        if (v == null) {
            synchronized (sendCache) {
                v = sendCache.get(clientId);
                if (v == null) {
                    v = new ConcurrentHashMap<>();
                    sendCache.put(clientId, v);
                }
            }
        }
        v.put(message.getMsgId(), message);
        return true;
    }

    @Override
    public Collection<Message> getAllOutflowMsg(String clientId) {
        ConcurrentHashMap<Integer, Message> v = sendCache.get(clientId);
        if (v == null || v.isEmpty()) {
            return new ArrayList<>();
        }
        return v.values();
    }

    @Override
    public Message releaseOutflowMsg(String clientId, Integer msgId) {
        ConcurrentHashMap<Integer, Message> v = sendCache.get(clientId);
        if (msgId == null) {
            if (v != null) {
                v.clear();
            }
            return null;
        } else {
            if (v == null) {
                LogUtil.warn(log, "[MemStore] -> The out of the stack message:{} does not exist", msgId);
                return null;
            }
            return v.remove(msgId);
        }
    }

    @Override
    public boolean cacheOutflowSecMsgId(String clientId, int msgId) {
        ConcurrentHashMap<Integer, Object> v = secTwoCache.get(clientId);
        if (v == null) {
            synchronized (secTwoCache) {
                v = secTwoCache.get(clientId);
                if (v == null) {
                    v = new ConcurrentHashMap<>();
                    secTwoCache.put(clientId, v);
                }
            }
        }
        v.put(msgId, OBJECT);
        return true;
    }

    @Override
    public boolean releaseOutflowSecMsgId(String clientId, Integer msgId) {
        ConcurrentHashMap<Integer, Object> v = secTwoCache.get(clientId);
        if (msgId == null) {
            if (v != null) {
                v.clear();
            }
            return true;
        } else {
            if (v == null) {
                LogUtil.warn(log, "[MemStore] -> The out flow QOS2 phase 2, client:{} outflow cache does not exist", clientId);
                return false;
            }
            Object os = v.remove(msgId);
            if (os == null) {
                LogUtil.warn(log, "[MemStore] -> The out flow QOS2 phase 2, msg:{} for client:{} does not exist", clientId, msgId);
            }
            return os != null;
        }
    }

    @Override
    public List<Integer> getAllOutflowSecMsgId(String clientId) {
        ConcurrentHashMap<Integer, Object> v = secTwoCache.get(clientId);
        if (v == null) {
            return new ArrayList<Integer>();
        }
        ArrayList<Integer> ret = new ArrayList<>(v.size());
        v.forEach((k, v2) -> {
            ret.add(k);
        });
        return ret;
    }

    @Override
    public boolean storeOfflineMsg(String clientId, Message message) {
        LocalDB.getInstance().operate(sqlSession -> {
            OfflineMessageDO offlineMessageDO = new OfflineMessageDO();
            offlineMessageDO.setId(IdWorker.getId());
            offlineMessageDO.setClientId(clientId);
            offlineMessageDO.setContent(JSONObject.toJSONString(message));
            offlineMessageDO.setGmtCreate(message.getStoreTime());
            return sqlSession.getMapper(LocalOfflineMessageMapper.class).storeOfflineMessage(offlineMessageDO);
        });
        /*BlockingQueue<Message> off = offlineTable.get(clientId);
        if (off == null) {
            synchronized (offlineTable) {
                off = offlineTable.get(clientId);
                if (off == null) {
                    off = new LinkedBlockingQueue<>();
                    offlineTable.put(clientId, off);
                }
            }
        }
        off.add(message);*/
        return true;
    }

    @Override
    public Collection<Message> getAllOfflineMsg(String clientId) {
        List<OfflineMessageDO> offlineMessageDOList = (List<OfflineMessageDO>) LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalOfflineMessageMapper.class).getAllOfflineMessage(clientId)
        );
        return offlineMessageDOList.stream().map(off -> JSONObject.parseObject(off.getContent(), Message.class)).collect(Collectors.toList());
        /*BlockingQueue<Message> off = offlineTable.get(clientId);
        if (off == null) {
            return new ArrayList<>();
        }
        return off;*/
    }

    @Override
    public boolean clearOfflineMsg(String clientId) {
        LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalOfflineMessageMapper.class).clearOfflineMessage(clientId)
        );
        // offlineTable.remove(clientId);
        return true;
    }

}
