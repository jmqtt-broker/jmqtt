package org.jmqtt.broker.processor.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.common.JmqttConst;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 订阅报文逻辑处理
 * TODO mqtt5协议支持
 */
public class SubscribeProcessor implements RequestProcessor {

    private static final Logger log = JmqttLogger.messageTraceLog;

    private SubscriptionMatcher subscriptionMatcher;
    private AuthValid authValid;
    private MessageStore messageStore;
    private SessionStore sessionStore;
    private InnerMessageDispatcher innerMessageDispatcher;

    public SubscribeProcessor(BrokerController controller) {
        this.subscriptionMatcher = controller.getSubscriptionMatcher();
        this.authValid = controller.getAuthValid();
        this.sessionStore = controller.getSessionStore();
        this.messageStore = controller.getMessageStore();
        this.innerMessageDispatcher = controller.getInnerMessageDispatcher();
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) mqttMessage;
        String clientId = NettyUtil.getClientId(ctx.channel());
        ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
        List<MqttTopicSubscription> subscriptions = subscribeMessage.payload().topicSubscriptions();
        List<Topic> validTopicList = validTopics(clientSession, subscriptions);
        if (validTopicList == null || validTopicList.size() == 0) {
            LogUtil.warn(log, "[Subscribe] -> Valid all subscribe topic failure,clientId:{}", clientId);
            return;
        }
        MqttMessageIdAndPropertiesVariableHeader variableHeader = (MqttMessageIdAndPropertiesVariableHeader) subscribeMessage.variableHeader();
        MqttProperties properties = variableHeader.properties();
        if (clientSession.isMqtt5() && properties != null) {
            Optional<MqttProperties.IntegerProperty> subscriptionIdentifier = Optional.ofNullable((MqttProperties.IntegerProperty) properties.getProperty(MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value()));
            if (subscriptionIdentifier.isPresent()) {
                if (!BrokerContext.getBrokerConfig().getSubscriptionIdentifierAvailable()) {
                    LogUtil.warn(log, "[Subscribe] -> Subscription Identifiers not supported,clientId:{}", clientId);
                    Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0xA1);
                    return;
                }
                validTopicList.forEach(t -> t.getOption().setSubscriptionIdentifier(subscriptionIdentifier.get().value()));
            }
            for (Topic t : validTopicList) {
                if (!BrokerContext.getBrokerConfig().getSharedSubscriptionAvailable() &&
                        t.getTopicName().startsWith(JmqttConst.SHARE_IDENTIFIERS)) {
                    LogUtil.warn(log, "[Subscribe] -> Shared Subscriptions not supported,clientId:{}", clientId);
                    Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x9E);
                    return;
                }
                if (!BrokerContext.getBrokerConfig().getWildcardSubscriptionAvailable() &&
                        (t.getTopicName().contains(JmqttConst.WILDCARD_SINGLE) ||
                                t.getTopicName().contains(JmqttConst.WILDCARD_MULTY))) {
                    LogUtil.warn(log, "[Subscribe] -> Wildcard Subscriptions not supported,clientId:{}", clientId);
                    Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0xA2);
                    return;
                }
            }
        }
        List<Integer> ackQos = getTopicQos(validTopicList);
        MqttMessage subAckMessage = MessageUtil.getSubAckMessage(variableHeader.messageId(), ackQos);
        ctx.writeAndFlush(subAckMessage);
        // send retain messages
        subscribe(clientSession, validTopicList);
    }

    private List<Integer> getTopicQos(List<Topic> topics) {
        List<Integer> qoss = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            qoss.add(Math.min(topic.getQos(), BrokerContext.getBrokerConfig().getMaximumQos()));
        }
        return qoss;
    }

    private void subscribe(ClientSession clientSession, List<Topic> validTopicList) {
        for (Topic topic : validTopicList) {
            String subTopic = topic.getTopicName();
            Subscription subscription = new Subscription(clientSession.getClientId(), subTopic, topic.getQos());
            SubscriptionOption option = topic.getOption();
            subscription.setOption(option);
            boolean subRs = this.subscriptionMatcher.subscribe(subscription);
            Collection<Message> retainMessages = messageStore.getRetainMsg(subTopic);
            retainMessages.forEach(retainMsg -> {
                int minQos = MessageUtil.getMinQos((int) retainMsg.getHeader(MessageHeader.QOS), topic.getQos());
                retainMsg.putHeader(MessageHeader.QOS, minQos);
                if (clientSession.isMqtt5()) {
                    if (MqttSubscriptionOption.RetainedHandlingPolicy.SEND_AT_SUBSCRIBE.value() == option.getRetainHandling() ||
                            (MqttSubscriptionOption.RetainedHandlingPolicy.SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS.value() == option.getRetainHandling() && subRs)) {
                        if (retainMsg.validity()) {
                            if (retainMsg.alive() > 0) {
                                MqttPublishMessage publishMessage = MessageUtil.getPubMessage(retainMsg, false, option, subscription.getClientId());
                                clientSession.getCtx().writeAndFlush(publishMessage);
                            } else {
                                // retain消息过期了，删除
                                log.info("[Subscribe] -> retain message expired, delete.");
                                messageStore.clearRetainMessage((String) retainMsg.getHeader(MessageHeader.TOPIC));
                            }
                        } else {
                            MqttPublishMessage publishMessage = MessageUtil.getPubMessage(retainMsg, false, option, subscription.getClientId());
                            clientSession.getCtx().writeAndFlush(publishMessage);
                        }
                    }
                } else {
                    MqttPublishMessage publishMessage = MessageUtil.getPubMessage(retainMsg, false, option, subscription.getClientId());
                    clientSession.getCtx().writeAndFlush(publishMessage);
                }
            });
            this.sessionStore.storeSubscription(clientSession.getClientId(), subscription);
        }
    }

    /**
     * 返回校验合法的topic
     */
    private List<Topic> validTopics(ClientSession clientSession, List<MqttTopicSubscription> subscriptions) {
        List<Topic> topicList = new ArrayList<>();
        for (MqttTopicSubscription subscription : subscriptions) {
            if (!authValid.subscribeVerify(clientSession.getClientId(), subscription.topicName())) {
                LogUtil.warn(log, "[SubPermission] this clientId:{} have no permission to subscribe this topic:{}", clientSession.getClientId(), subscription.topicName());
                Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x87);
                return null;
            }
            Topic topic = new Topic(subscription.topicName(), subscription.qualityOfService().value());
            if (clientSession.isMqtt5()) {
                MqttSubscriptionOption option = subscription.option();
                topic.setOption(new SubscriptionOption(option));
            }
            topicList.add(topic);
        }
        return topicList;
    }

}
