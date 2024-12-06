package org.jmqtt.broker.processor.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.processor.RequestProcessor;
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

    public SubscribeProcessor(BrokerController controller) {
        this.subscriptionMatcher = controller.getSubscriptionMatcher();
        this.authValid = controller.getAuthValid();
        this.sessionStore = controller.getSessionStore();
        this.messageStore = controller.getMessageStore();
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
            Optional.ofNullable(properties.getProperty(MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value())).ifPresent(v -> {
                int subscriptionIdentifier = (int) v.value();
                validTopicList.forEach(t -> t.getOption().setSubscriptionIdentifier(subscriptionIdentifier));
            });
        }
        List<Integer> ackQos = getTopicQos(validTopicList);
        MqttMessage subAckMessage = MessageUtil.getSubAckMessage(variableHeader.messageId(), ackQos);
        ctx.writeAndFlush(subAckMessage);
        // send retain messages
        List<Message> retainMessages = subscribe(clientSession, validTopicList);
        dispatcherRetainMessage(clientSession, retainMessages);
    }

    private List<Integer> getTopicQos(List<Topic> topics) {
        List<Integer> qoss = new ArrayList<>(topics.size());
        for (Topic topic : topics) {
            qoss.add(topic.getQos());
        }
        return qoss;
    }

    private List<Message> subscribe(ClientSession clientSession, List<Topic> validTopicList) {
        Collection<Message> retainMessages = null;
        List<Message> needDispatcher = new ArrayList<>();
        for (Topic topic : validTopicList) {
            Subscription subscription = new Subscription(clientSession.getClientId(), topic.getTopicName(), topic.getQos());
            SubscriptionOption option = topic.getOption();
            subscription.setOption(option);
            boolean subRs = this.subscriptionMatcher.subscribe(subscription);
            if (retainMessages == null) {
                retainMessages = messageStore.getAllRetainMsg(); // TODO 这里需要优化，不能一次获取所有retain消息，retain消息太多可能导致broker crash或者hang住
            }
            if (!MixAll.isEmpty(retainMessages)) {
                for (Message retainMsg : retainMessages) {
                    String pubTopic = (String) retainMsg.getHeader(MessageHeader.TOPIC);
                    if (subscriptionMatcher.isMatch(pubTopic, subscription.getTopic())) {
                        int minQos = MessageUtil.getMinQos((int) retainMsg.getHeader(MessageHeader.QOS), topic.getQos());
                        retainMsg.putHeader(MessageHeader.QOS, minQos);
                        if (MqttSubscriptionOption.RetainedHandlingPolicy.SEND_AT_SUBSCRIBE.value() == option.getRetainHandling() ||
                                (MqttSubscriptionOption.RetainedHandlingPolicy.SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS.value() == option.getRetainHandling() && subRs)) {
                            needDispatcher.add(retainMsg);
                        }
                    }
                }
            }
            this.sessionStore.storeSubscription(clientSession.getClientId(), subscription);
        }
        return needDispatcher;
    }

    /**
     * 返回校验合法的topic
     */
    private List<Topic> validTopics(ClientSession clientSession, List<MqttTopicSubscription> subscriptions) {
        List<Topic> topicList = new ArrayList<>();
        for (MqttTopicSubscription subscription : subscriptions) {
            if (!authValid.subscribeVerify(clientSession.getClientId(), subscription.topicName())) {
                LogUtil.warn(log, "[SubPermission] this clientId:{} have no permission to subscribe this topic:{}", clientSession.getClientId(), subscription.topicName());
                clientSession.getCtx().close();
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

    /**
     * 分发retain消息:
     * TODO 待优化，retain消息逻辑需要优化：1.性能优化；2.逻辑放到MessageDispatcher统一处理
     */
    private void dispatcherRetainMessage(ClientSession clientSession, List<Message> messages) {
        for (Message message : messages) {
            message.putHeader(MessageHeader.RETAIN, true);
            int qos = (int) message.getHeader(MessageHeader.QOS);
            if (qos > 0) {
                sessionStore.cacheInflowMsg(clientSession.getClientId(), message);
            }
            message.setMsgId(clientSession.generateMessageId());
            MqttPublishMessage publishMessage = MessageUtil.getPubMessage(message, false);
            clientSession.getCtx().writeAndFlush(publishMessage);
        }
    }

}
