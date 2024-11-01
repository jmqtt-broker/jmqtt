package org.jmqtt.starter.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.acl.impl.DefaultAuthValid;
import org.jmqtt.broker.client.ClientLifeCycleHookService;
import org.jmqtt.broker.processor.protocol.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/11/1 14:12
 */
@Configuration
@Slf4j
public class JmqttInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuthValid.class)
    public AuthValid authValid(BrokerController brokerController) {
        DefaultAuthValid defaultAuthValid = new DefaultAuthValid();
        brokerController.setAuthValid(defaultAuthValid);
        return defaultAuthValid;
    }

    @Bean
    @ConditionalOnMissingBean(ClientLifeCycleHookService.class)
    public ClientLifeCycleHookService clientLifeCycleHookService(BrokerController brokerController) {
        ClientLifeCycleHookService connctHook = new ClientLifeCycleHookService(brokerController.getMessageStore(), brokerController.getInnerMessageDispatcher());
        brokerController.setChannelEventListener(connctHook);
        return connctHook;
    }

    @Bean
    @ConditionalOnMissingBean(ConnectProcessor.class)
    public ConnectProcessor connectProcessor(BrokerController brokerController) {
        ConnectProcessor connectProcessor = new ConnectProcessor(brokerController);
        brokerController.addRequestProcessor(connectProcessor);
        return new ConnectProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(DisconnectProcessor.class)
    public DisconnectProcessor disconnectProcessor(BrokerController brokerController) {
        DisconnectProcessor disconnectProcessor = new DisconnectProcessor(brokerController);
        brokerController.addRequestProcessor(disconnectProcessor);
        return disconnectProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(PingProcessor.class)
    public PingProcessor pingProcessor(BrokerController brokerController) {
        PingProcessor pingProcessor = new PingProcessor();
        brokerController.addRequestProcessor(pingProcessor);
        return pingProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(PublishProcessor.class)
    public PublishProcessor publishProcessor(BrokerController brokerController) {
        PublishProcessor publishProcessor = new PublishProcessor(brokerController);
        brokerController.addRequestProcessor(publishProcessor);
        return publishProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(PubRelProcessor.class)
    public PubRelProcessor pubRelProcessor(BrokerController brokerController) {
        PubRelProcessor pubRelProcessor = new PubRelProcessor(brokerController);
        brokerController.addRequestProcessor(pubRelProcessor);
        return pubRelProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(SubscribeProcessor.class)
    public SubscribeProcessor subscribeProcessor(BrokerController brokerController) {
        SubscribeProcessor subscribeProcessor = new SubscribeProcessor(brokerController);
        brokerController.addRequestProcessor(subscribeProcessor);
        return subscribeProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(UnSubscribeProcessor.class)
    public UnSubscribeProcessor unSubscribeProcessor(BrokerController brokerController) {
        UnSubscribeProcessor unSubscribeProcessor = new UnSubscribeProcessor(brokerController.getSubscriptionMatcher(), brokerController.getSessionStore());
        brokerController.addRequestProcessor(unSubscribeProcessor);
        return unSubscribeProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(PubRecProcessor.class)
    public PubRecProcessor pubRecProcessor(BrokerController brokerController) {
        PubRecProcessor pubRecProcessor = new PubRecProcessor(brokerController);
        brokerController.addRequestProcessor(pubRecProcessor);
        return pubRecProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(PubAckProcessor.class)
    public PubAckProcessor pubAckProcessor(BrokerController brokerController) {
        PubAckProcessor pubAckProcessor = new PubAckProcessor(brokerController);
        brokerController.addRequestProcessor(pubAckProcessor);
        return pubAckProcessor;
    }

    @Bean
    @ConditionalOnMissingBean(PubCompProcessor.class)
    public PubCompProcessor pubCompProcessor(BrokerController brokerController) {
        PubCompProcessor pubCompProcessor = new PubCompProcessor(brokerController);
        brokerController.addRequestProcessor(pubCompProcessor);
        return pubCompProcessor;
    }

}
