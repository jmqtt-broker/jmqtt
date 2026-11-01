
package com.jmqtt.core.mqtt.store;

import cn.hutool.core.bean.BeanUtil;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.mqtt.store.persist.po.Session;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.session.ClientManager;
import io.netty.handler.codec.mqtt.MqttVersion;

import java.util.*;

public interface SessionStore {

    SessionState getSession(String clientId);

    List<Session> getSessionList(Collection<String> clientIds);

    boolean storeSession(SessionState sessionState);

    default void clearSession(String clientId, boolean cleanStart) {
        SessionState session = getSession(clientId);
        long onlineTime = Optional.ofNullable(session.getOnlineTime()).orElse(0L);
        long cur = System.currentTimeMillis();
        if (SessionState.StateEnum.ONLINE.equals(session.getState()) && (onlineTime > 0 && cur < onlineTime)) {
            return;
        }
        Optional.ofNullable(session.getVersion()).ifPresent(v -> {
            if (v == MqttVersion.MQTT_5.protocolLevel()) {
                // TopicAliasManager.clear(clientId);
                // 会话到期了，如果存在延迟未发送的遗嘱消息，此时需要立即发送
                // TimerUtils.sendWillImmediately(clientId);
            }
        });
        SessionState update = new SessionState(clientId, SessionState.StateEnum.NEVER_CONNECTED);
        BeanUtil.copyProperties(session, update);
        if (cleanStart) {
            clearOfflineMsg(clientId);
            clearSubscriptionAndSubTree(clientId);
            update.setState(SessionState.StateEnum.NEVER_CONNECTED);
        } else {
            update.setState(SessionState.StateEnum.OFFLINE);
        }
        storeSession(update);
        ClientManager.removeClient(clientId);
        releaseInflowMsg(clientId, null);
        releaseOutflowMsg(clientId, null);
        releaseOutflowSecMsgId(clientId, null);
    }

    /**
     * 保存订阅
     *
     * @param subscription subscription
     * @return return
     */
    boolean storeSubscription(Subscription subscription);

    /**
     * 移除订阅
     *
     * @param clientId clientId
     * @param topic    topic
     * @return return
     */
    boolean delSubscription(String clientId, String topic);

    /**
     * 清除某个Client的所有
     *
     * @param clientId clientId
     * @return return
     */
    boolean clearSubscription(String clientId);

    default void clearSubscriptionAndSubTree(String clientId) {
        Set<Subscription> subscriptions = getSubscriptions(clientId);
        if (!subscriptions.isEmpty()) {
            subscriptions.forEach(s -> getSubscriptionMatcher().unSubscribe(s.getTopic(), clientId));
        }
        clearSubscription(clientId);
    }

    SubscriptionMatcher getSubscriptionMatcher();

    /**
     * 获取该clientId的所有的订阅关系
     *
     * @param clientId clientId
     * @return return
     */
    Set<Subscription> getSubscriptions(String clientId);

    Set<Subscription> getAllSubscription();

    Subscription getOneSubscription(String clientId, String topic);

    /**
     * 缓存入栈消息qos 1, 2 publish
     *
     * @param clientId clientId
     * @param message  message
     * @return return
     */
    boolean cacheInflowMsg(String clientId, Message message);

    /**
     * 获取并删除接收到的qos2消息-入栈消息
     *
     * @param clientId clientId
     * @param msgId    msgId
     * @return return
     */
    Message releaseInflowMsg(String clientId, Integer msgId);

    /**
     * 获取所有的入栈消息
     *
     * @param clientId clientId
     * @return return
     */
    Collection<Message> getAllInflowMsg(String clientId);

    /**
     * 缓存出栈消息-分发给客户端的qos1, 2消息
     *
     * @param clientId clientId
     * @param message  message
     * @return return
     */
    boolean cacheOutflowMsg(String clientId, Message message);

    /**
     * 获取所有的出栈消息
     *
     * @param clientId clientId
     * @return return
     */
    Collection<Message> getAllOutflowMsg(String clientId);

    /**
     * 获取并删除发送的出栈消息
     *
     * @param clientId clientId
     * @param msgId    msgId
     * @return return
     */
    Message releaseOutflowMsg(String clientId, Integer msgId);

    /**
     * 出栈qos2第二阶段，缓存msgId
     *
     * @param clientId clientId
     * @param msgId    msgId
     * @return return
     */
    boolean cacheOutflowSecMsgId(String clientId, int msgId);

    /**
     * 出栈qos2第二阶段，释放msgId
     * 返回false时说明msgId不存在，异常情况
     *
     * @param clientId clientId
     * @param msgId    msgId
     * @return return
     */
    boolean releaseOutflowSecMsgId(String clientId, Integer msgId);

    /**
     * 获取所有的信息，进行发送
     *
     * @param clientId clientId
     * @return return
     */
    List<Integer> getAllOutflowSecMsgId(String clientId);

    /**
     * 缓存离线消息
     *
     * @param clientId clientId
     * @param message  message
     * @return return
     */
    boolean storeOfflineMsg(String clientId, Message message);

    /**
     * 获取所有的离线消息
     *
     * @param clientId clientId
     * @return return
     */
    Collection<Message> getAllOfflineMsg(String clientId);

    /**
     * 清理该客户端的离线消息
     *
     * @param clientId clientId
     * @return return
     */
    boolean clearOfflineMsg(String clientId);

    /**
     * 获取某个客户端的某个连接属性
     *
     * @param clientId   clientId
     * @param propertyId propertyId
     * @return return
     */
    default Object getClientProperty(String clientId, Integer propertyId) {
        SessionState session = getSession(clientId);
        Map<Integer, Object> propertyMap = session.getPropertyMap();
        return propertyMap != null ? propertyMap.get(propertyId) : null;
    }

}
