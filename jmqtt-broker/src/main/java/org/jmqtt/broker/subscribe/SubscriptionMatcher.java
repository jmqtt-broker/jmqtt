package org.jmqtt.broker.subscribe;

import org.jmqtt.broker.common.model.Subscription;

import java.util.Set;

/**
 * Subscription tree
 * 订阅树处理
 *  TODO 1.支持共享订阅
 *  TODO 2.支持 p2p消息(jmqtt 特有)
 *  TODO 2.支持系统topic $SYS，需要定时采集本机节点等信息下发
 */
public interface SubscriptionMatcher {

    /**
     * add subscribe
     * @param subscription  subscription
     * @return  true: add new subscription success. false: subscription exist.
     */
    boolean subscribe(Subscription subscription);

    boolean unSubscribe(String topic,String clientId);

    /**
     * 获取匹配该topic下的所有订阅者
     * @param topic     topic
     * @param clientId  clientId
     * @return  return
     */
    Set<Subscription> match(String topic, String clientId);

    /**
     * 发布消息的Topic与订阅的topic是否匹配
     * @param pubTopic  pubTopic
     * @param subTopic  subTopic
     * @return  return
     */
    boolean isMatch(String pubTopic,String subTopic);

    String getPubTopic(String subTopic);
}
