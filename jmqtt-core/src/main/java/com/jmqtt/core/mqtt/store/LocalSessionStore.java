package com.jmqtt.core.mqtt.store;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.mqtt.store.persist.LocalDB;
import com.jmqtt.core.mqtt.store.persist.mapper.OfflineMessageMapper;
import com.jmqtt.core.mqtt.store.persist.mapper.SessionMapper;
import com.jmqtt.core.mqtt.store.persist.mapper.SubscriptionMapper;
import com.jmqtt.core.mqtt.store.persist.po.OfflineMessage;
import com.jmqtt.core.mqtt.store.persist.po.Session;
import com.jmqtt.core.mqtt.store.persist.po.SubscriptionDO;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class LocalSessionStore implements SessionStore {

    /**
     * session
     */
    private final Map<String, SessionState> sessionTable = new ConcurrentHashMap<>();

    /**
     * Qos 1、2产生的过程消息
     */
    private final Map<String, ConcurrentHashMap<Integer, Message>> recCache = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<Integer, Message>> sendCache = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<Integer, Object>> secTwoCache = new ConcurrentHashMap<>();

    @Override
    public SessionState getSession(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return null;
        }
        SessionState s = sessionTable.get(clientId);
        if (s == null) {
            Session session = LocalDB.getInstance().operate(sqlSession -> sqlSession.getMapper(SessionMapper.class).selectById(clientId));
            if (session == null) {
                return new SessionState(clientId, SessionState.StateEnum.NEVER_CONNECTED);
            }
            s = new SessionState(session);
            sessionTable.put(clientId, s);
        }
        return s;
    }

    @Override
    public List<Session> getSessionList(Collection<String> clientIds) {
        return LocalDB.getInstance().operate(sqlSession -> sqlSession.getMapper(SessionMapper.class).selectByIds(clientIds));
    }

    @Override
    public boolean storeSession(SessionState sessionState) {
        String clientId = sessionState.getClientId();
        sessionTable.remove(clientId);
        return LocalDB.getInstance().operate(sqlSession -> {
            Session session = Session.builder().clientId(clientId)
                    .brokerId(sessionState.getBrokerId())
                    .state(sessionState.getState().getCode())
                    .onlineTime(sessionState.getOnlineTime())
                    .offlineTime(sessionState.getOfflineTime())
                    .version(sessionState.getVersion())
                    .address(sessionState.getAddress())
                    .cleanStart(sessionState.getCleanStart())
                    .keepalive(sessionState.getKeepalive())
                    .build();
            Optional.ofNullable(sessionState.getPropertyMap()).ifPresent(p -> session.setProperty(JSONUtil.toJsonStr(p)));
            return sqlSession.getMapper(SessionMapper.class).insert(session);
        }) > 0;
    }

    @Override
    public boolean storeSubscription(Subscription subscription) {
        return LocalDB.getInstance().operate(sqlSession -> {
            SubscriptionDO subscriptionDO = SubscriptionDO.builder().id(IdWorker.getId())
                    .brokerId(subscription.getBrokerId())
                    .clientId(subscription.getClientId())
                    .topic(subscription.getTopic())
                    .qos(subscription.getQos())
                    .subRes(subscription.getSubRes())
                    .build();
            Optional.ofNullable(subscription.getOption()).ifPresent(opt -> {
                subscriptionDO.setOpt(JSONUtil.toJsonStr(opt));
            });
            return sqlSession.getMapper(SubscriptionMapper.class).insert(subscriptionDO);
        }) > 0;
    }

    @Override
    public boolean delSubscription(String clientId, String topic) {
        return LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(SubscriptionMapper.class)
                        .delete(
                                Wrappers.lambdaUpdate(SubscriptionDO.class)
                                        .eq(SubscriptionDO::getClientId, clientId)
                                        .eq(SubscriptionDO::getTopic, topic)
                        )
        ) > 0;
    }

    @Override
    public boolean clearSubscription(String clientId) {
        return LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(SubscriptionMapper.class).delete(
                        Wrappers.lambdaUpdate(SubscriptionDO.class)
                                .eq(SubscriptionDO::getClientId, clientId)
                )
        ) > 0;
    }

    @Override
    public Set<Subscription> getSubscriptions(String clientId) {
        List<SubscriptionDO> subscriptionDOList = LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(SubscriptionMapper.class).selectList(Wrappers.lambdaQuery(SubscriptionDO.class).eq(SubscriptionDO::getClientId, clientId))
        );
        return subscriptionDOList.stream().map(Subscription::of).collect(Collectors.toSet());
    }

    @Override
    public Subscription getOneSubscription(String clientId, String topic) {
        SubscriptionDO subscriptionDO = LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(SubscriptionMapper.class).selectOne(Wrappers.lambdaQuery(SubscriptionDO.class)
                        .eq(SubscriptionDO::getClientId, clientId).eq(SubscriptionDO::getTopic, topic))
        );
        return Optional.ofNullable(subscriptionDO).map(Subscription::of).orElse(null);
    }

    @Override
    public Set<Subscription> getAllSubscription() {
        List<SubscriptionDO> subscriptionDOList = LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(SubscriptionMapper.class).selectList(Wrappers.lambdaQuery(SubscriptionDO.class))
        );
        return subscriptionDOList.stream().map(Subscription::of).collect(Collectors.toSet());
    }

    @Override
    public boolean cacheInflowMsg(String clientId, Message message) {
        ConcurrentHashMap<Integer, Message> v = recCache.get(clientId);
        if (v == null) {
            synchronized (recCache) {
                v = recCache.computeIfAbsent(clientId, k -> MapUtil.newConcurrentHashMap(64));
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
                log.warn("inflow message: [{}] does not exist", msgId);
                return null;
            }
            return v.remove(msgId);
        }
    }

    @Override
    public Collection<Message> getAllInflowMsg(String clientId) {
        ConcurrentHashMap<Integer, Message> v = recCache.get(clientId);
        if (v == null || v.isEmpty()) {
            return List.of();
        }
        return v.values();
    }

    @Override
    public boolean cacheOutflowMsg(String clientId, Message message) {
        ConcurrentHashMap<Integer, Message> v = sendCache.get(clientId);
        if (v == null) {
            synchronized (sendCache) {
                v = sendCache.computeIfAbsent(clientId, k -> MapUtil.newConcurrentHashMap(64));
            }
        }
        v.put(message.getMsgId(), message);
        return true;
    }

    @Override
    public Collection<Message> getAllOutflowMsg(String clientId) {
        ConcurrentHashMap<Integer, Message> v = sendCache.get(clientId);
        if (v == null || v.isEmpty()) {
            return List.of();
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
                log.warn("outflow message: [{}] does not exist", msgId);
                return null;
            } else {
                return v.remove(msgId);
            }
        }
    }

    @Override
    public boolean cacheOutflowSecMsgId(String clientId, int msgId) {
        ConcurrentHashMap<Integer, Object> v = secTwoCache.get(clientId);
        if (v == null) {
            synchronized (secTwoCache) {
                v = secTwoCache.computeIfAbsent(clientId, k -> MapUtil.newConcurrentHashMap(64));
            }
        }
        v.put(msgId, 1);
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
                log.warn("outflow cache does not exist. client: {}", clientId);
                return false;
            }
            Object os = v.remove(msgId);
            if (os == null) {
                log.warn("outflow sec message: [{}] does not exist", msgId);
            }
            return os != null;
        }
    }

    @Override
    public List<Integer> getAllOutflowSecMsgId(String clientId) {
        ConcurrentHashMap<Integer, Object> v = secTwoCache.get(clientId);
        if (v == null) {
            return List.of();
        }
        return CollUtil.newArrayList(v.keys());
    }

    @Override
    public boolean storeOfflineMsg(String clientId, Message message) {
        return LocalDB.getInstance().operate(sqlSession -> {
            OfflineMessage offlineMessage = OfflineMessage.builder()
                    .id(IdWorker.getId())
                    .clientId(clientId)
                    .content(JSONUtil.toJsonStr(message))
                    .gmtCreate(message.getStoreTime())
                    .build();
            return sqlSession.getMapper(OfflineMessageMapper.class).insert(offlineMessage);
        }) > 0;
    }

    @Override
    public Collection<Message> getAllOfflineMsg(String clientId) {
        List<OfflineMessage> offlineMessageList = LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(OfflineMessageMapper.class).selectList(Wrappers.lambdaQuery(OfflineMessage.class).eq(OfflineMessage::getClientId, clientId))
        );
        return offlineMessageList.stream().map(off -> JSONUtil.toBean(off.getContent(), Message.class)).collect(Collectors.toList());
    }

    @Override
    public boolean clearOfflineMsg(String clientId) {
        return LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(OfflineMessageMapper.class).delete(Wrappers.lambdaUpdate(OfflineMessage.class).eq(OfflineMessage::getClientId, clientId))
        ) > 0;
    }

}
