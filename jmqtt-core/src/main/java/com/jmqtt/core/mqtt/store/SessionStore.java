
package com.jmqtt.core.mqtt.store;

import cn.hutool.core.bean.BeanUtil;
import com.jmqtt.core.common.JmqttConst;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.netty.session.ClientManager;
import io.netty.handler.codec.mqtt.MqttVersion;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface SessionStore {

    /**
     * session
     */
    Map<String, SessionState> memSessionStore = new ConcurrentHashMap<>();

    default SessionState getSession(String clientId) {
        return memSessionStore.get(clientId);
    }

    default List<SessionDO> getSessionList(Collection<String> clientIds) {
        return null;
    }

    default boolean storeSession(SessionState sessionState) {
        return memSessionStore.putIfAbsent(sessionState.getClientId(), sessionState) == null;
    }

    default void clearSession(String clientId, boolean cleanStart){
        SessionState session = getSession(clientId);
        long onlineTime = Optional.ofNullable(session.getOnlineTime()).orElse(0L);
        long cur = System.currentTimeMillis();
        if (SessionState.StateEnum.ONLINE.equals(session.getState()) && (onlineTime > 0 && cur < onlineTime)) {
            return;
        }
        Optional.ofNullable(session.getVersion()).ifPresent(v -> {
            if (v == MqttVersion.MQTT_5.protocolLevel()) {
                // TopicAliasManager.clear(clientId);
                clearClientProperty(clientId);
                // 会话到期了，如果存在延迟未发送的遗嘱消息，此时需要立即发送
                // TimerUtils.sendWillImmediately(clientId);
            }
        });
        SessionState update = new SessionState(clientId, SessionState.StateEnum.NEVER_CONNECTED);
        if (cleanStart) {
            clearOfflineMsg(clientId);
            clearSubscriptionAndSubTree(clientId);
        } else {
            update.setState(SessionState.StateEnum.OFFLINE);
        }
        BeanUtil.copyProperties(session, update);
        storeSession(update);
        ClientManager.removeClient(clientId);
        releaseInflowMsg(clientId, null);
        releaseOutflowMsg(clientId, null);
        releaseOutflowSecMsgId(clientId, null);
    }

    /**
     * 存储订阅关系
     * @param clientId      clientId
     * @param subscription  subscription
     * @return  return
     */
    default boolean storeSubscription(String clientId, Subscription subscription) {
        return false;
    }

    /**
     * 移除订阅关系
     * @param clientId  clientId
     * @param topic     topic
     * @return  return
     */
    default boolean delSubscription(String clientId,String topic) {
        return false;
    }

    /**
     * 清理订阅信息
     * @param clientId  clientId
     * @return  return
     */
    default boolean clearSubscription(String clientId) {
        return false;
    }

    default void clearSubscriptionAndSubTree(String clientId) {
        Set<Subscription> subscriptions = getSubscriptions(clientId);
        if (!subscriptions.isEmpty()) {
            // subscriptions.forEach(s -> BrokerContext.getSubscriptionMatcher().unSubscribe(s.getTopic(), clientId));
        }
        clearSubscription(clientId);
    }

    /**
     * 获取该clientId的所有的订阅关系
     * @param clientId  clientId
     * @return  return
     */
    default Set<Subscription> getSubscriptions(String clientId) {
        return new HashSet<>();
    }

    default Set<Subscription> getAllSubscription() {

        return new HashSet<>();
    }

    default Subscription getOneSubscription(String clientId, String topic) {
        return null;
    }

    /**
     * 缓存qos2 publish报文消息-入栈消息
     * @param clientId  clientId
     * @param message   message
     * @return  return
     */
    default boolean cacheInflowMsg(String clientId, Message message) {
        return false;
    }

    /**
     * 获取并删除接收到的qos2消息-入栈消息
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    default Message releaseInflowMsg(String clientId,Integer msgId) {
        return null;
    }

    /**
     * 获取所有的入栈消息
     * @param clientId  clientId
     * @return  return
     */
    default Collection<Message> getAllInflowMsg(String clientId) {
        return new ArrayList<>();
    }

    /**
     * 缓存出栈消息-分发给客户端的qos1,qos2消息
     * @param clientId  clientId
     * @param message   message
     * @return  return
     */
    default boolean cacheOutflowMsg(String clientId,Message message) {
        return false;
    }

    /**
     * 获取所有的出栈消息
     * @param clientId  clientId
     * @return  return
     */
    default Collection<Message> getAllOutflowMsg(String clientId) {
        return new ArrayList<>();
    }

    /**
     * 获取并删除发送的出栈消息
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    default Message releaseOutflowMsg(String clientId,Integer msgId) {
        return null;
    }

    /**
     * 出栈qos2第二阶段，缓存msgId
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    default boolean cacheOutflowSecMsgId(String clientId,int msgId) {
        return false;
    }

    /**
     * 出栈qos2第二阶段，释放msgId
     * 若为false，说明msgId不存在（异常情况）
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    default boolean releaseOutflowSecMsgId(String clientId,Integer msgId) {
        return false;
    }

    /**
     * 获取所有的信息，进行发送
     * @param clientId  clientId
     * @return  return
     */
    default List<Integer> getAllOutflowSecMsgId(String clientId) {
        return new ArrayList<>();
    }

    /**
     * 缓存离线消息
     * @param clientId  clientId
     * @param message   message
     * @return  return
     */
    default boolean storeOfflineMsg(String clientId,Message message) {
        return false;
    }

    /**
     * 获取所有的离线消息
     * @param clientId  clientId
     * @return  return
     */
    default Collection<Message> getAllOfflineMsg(String clientId) {
        return new ArrayList<>();
    }

    /**
     * 清理该客户端的离线消息
     * @param clientId  clientId
     * @return  return
     */
    default boolean clearOfflineMsg(String clientId) {
        return false;
    }

    /**
     * 获取某个客户端的某个连接属性
     * @param clientId      clientId
     * @param propertyId    propertyId
     * @return  return
     */
    default Object getClientProperty(String clientId, Integer propertyId) {
        /*String key = JmqttConst.CLIENT_PROPERTIES + clientId;
        Map<Integer, Object> propertyMap = (Map<Integer, Object>) CaffeineUtil.get(key);
        if (propertyMap != null) {
            return propertyMap.get(propertyId);
        }
        SessionState session = getSession(clientId);
        propertyMap = session.getPropertyMap();
        if (propertyMap != null) {
            CaffeineUtil.put(clientId, propertyMap, 30 * 60);
            return propertyMap.get(propertyId);
        }
        return null;*/
        SessionState session = getSession(clientId);
        return session.getPropertyMap().get(propertyId);
    }

    default void clearClientProperty(String clientId) {
        // CaffeineUtil.del(JmqttConst.CLIENT_PROPERTIES + clientId);
    }

}
