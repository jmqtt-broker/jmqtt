
package org.jmqtt.broker.store;

import io.netty.handler.codec.mqtt.MqttVersion;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.common.helper.CaffeineUtil;
import org.jmqtt.broker.common.helper.TimerUtils;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.remoting.session.ConnectManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface SessionStore {

    /**
     * session
     */
    Map<String, SessionState> sessionTable = new ConcurrentHashMap<>();

    void start(BrokerConfig brokerConfig);

    void shutdown();

    SessionState getSession(String clientId);

    default List<SessionDO> getSessionList(Collection<String> clientIds) {
        return null;
    }

    boolean storeSession(SessionState sessionState);

    default void clearSession(String clientId, boolean cleanStart){
        SessionState session = getSession(clientId);
        long onlineTime = session.getOnlineTime();
        long cur = System.currentTimeMillis();
        if (session.getState() == SessionState.StateEnum.ONLINE && (onlineTime > 0 && cur < onlineTime)) {
            return;
        }
        Optional.ofNullable(session.getVersion()).ifPresent(v -> {
            if (v == MqttVersion.MQTT_5.protocolLevel()) {
                TopicAliasManager.clear(clientId);
                clearClientProperty(clientId);
                // 会话到期了，如果存在延迟未发送的遗嘱消息，此时需要立即发送
                TimerUtils.sendWillImmediately(clientId);
            }
        });
        if (cleanStart) {
            clearOfflineMsg(clientId);
            clearSubscriptionAndSubTree(clientId);
        } else {
            clearSubscription(clientId);
        }
        releaseInflowMsg(clientId, null);
        releaseOutflowMsg(clientId, null);
        releaseOutflowSecMsgId(clientId, null);
        SessionState update = new SessionState(clientId, SessionState.StateEnum.NULL);
        update.setOnlineTime(session.getOnlineTime());
        update.setVersion(session.getVersion());
        update.setAddress(session.getAddress());
        storeSession(update);
        ConnectManager.getInstance().removeClient(clientId);
    }

    /**
     * 存储订阅关系
     * @param clientId      clientId
     * @param subscription  subscription
     * @return  return
     */
    boolean storeSubscription(String clientId,Subscription subscription);

    /**
     * 移除订阅关系
     * @param clientId  clientId
     * @param topic     topic
     * @return  return
     */
    boolean delSubscription(String clientId,String topic);

    /**
     * 清理订阅信息
     * @param clientId  clientId
     * @return  return
     */
    boolean clearSubscription(String clientId);

    default void clearSubscriptionAndSubTree(String clientId) {
        Set<Subscription> subscriptions = getSubscriptions(clientId);
        if (!subscriptions.isEmpty()) {
            subscriptions.forEach(s -> BrokerContext.getSubscriptionMatcher().unSubscribe(s.getTopic(), clientId));
        }
        clearSubscription(clientId);
    }

    /**
     * 获取该clientId的所有的订阅关系
     * @param clientId  clientId
     * @return  return
     */
    Set<Subscription> getSubscriptions(String clientId);

    default Subscription getOneSubscription(String clientId, String topic) {
        return null;
    }

    /**
     * 缓存qos2 publish报文消息-入栈消息
     * @param clientId  clientId
     * @param message   message
     * @return  return
     */
    boolean cacheInflowMsg(String clientId, Message message);

    /**
     * 获取并删除接收到的qos2消息-入栈消息
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    Message releaseInflowMsg(String clientId,Integer msgId);

    /**
     * 获取所有的入栈消息
     * @param clientId  clientId
     * @return  return
     */
    Collection<Message> getAllInflowMsg(String clientId);

    /**
     * 缓存出栈消息-分发给客户端的qos1,qos2消息
     * @param clientId  clientId
     * @param message   message
     * @return  return
     */
    boolean cacheOutflowMsg(String clientId,Message message);

    /**
     * 获取所有的出栈消息
     * @param clientId  clientId
     * @return  return
     */
    Collection<Message> getAllOutflowMsg(String clientId);

    /**
     * 获取并删除发送的出栈消息
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    Message releaseOutflowMsg(String clientId,Integer msgId);

    /**
     * 出栈qos2第二阶段，缓存msgId
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    boolean cacheOutflowSecMsgId(String clientId,int msgId);

    /**
     * 出栈qos2第二阶段，释放msgId
     * 若为false，说明msgId不存在（异常情况）
     * @param clientId  clientId
     * @param msgId     msgId
     * @return  return
     */
    boolean releaseOutflowSecMsgId(String clientId,Integer msgId);

    /**
     * 获取所有的信息，进行发送
     * @param clientId  clientId
     * @return  return
     */
    List<Integer> getAllOutflowSecMsgId(String clientId);

    /**
     * 缓存离线消息
     * @param clientId  clientId
     * @param message   message
     * @return  return
     */
    boolean storeOfflineMsg(String clientId,Message message);

    /**
     * 获取所有的离线消息
     * @param clientId  clientId
     * @return  return
     */
    Collection<Message> getAllOfflineMsg(String clientId);

    /**
     * 清理该客户端的离线消息
     * @param clientId  clientId
     * @return  return
     */
    boolean clearOfflineMsg(String clientId);

    /**
     * 获取某个客户端的某个连接属性
     * @param clientId      clientId
     * @param propertyId    propertyId
     * @return  return
     */
    default Object getClientProperty(String clientId, Integer propertyId) {
        String key = JmqttConst.CLIENT_PROPERTIES + clientId;
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
        return null;
    }

    default void clearClientProperty(String clientId) {
        CaffeineUtil.del(JmqttConst.CLIENT_PROPERTIES + clientId);
    }

}
