package org.jmqtt.starter.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.processor.protocol.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/11/1 14:12
 */
@Slf4j
public class JmqttProcessorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConnectProcessor.class)
    public ConnectProcessor connectProcessor(BrokerController brokerController) {
        return new ConnectProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(DisconnectProcessor.class)
    public DisconnectProcessor disconnectProcessor(BrokerController brokerController) {
        return new DisconnectProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(PingProcessor.class)
    public PingProcessor pingProcessor() {
        return new PingProcessor();
    }

    @Bean
    @ConditionalOnMissingBean(PublishProcessor.class)
    public PublishProcessor publishProcessor(BrokerController brokerController) {
        return new PublishProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(PubRelProcessor.class)
    public PubRelProcessor pubRelProcessor(BrokerController brokerController) {
        return new PubRelProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(SubscribeProcessor.class)
    public SubscribeProcessor subscribeProcessor(BrokerController brokerController) {
        return new SubscribeProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(UnSubscribeProcessor.class)
    public UnSubscribeProcessor unSubscribeProcessor(BrokerController brokerController) {
        return new UnSubscribeProcessor(brokerController.getSubscriptionMatcher(), brokerController.getSessionStore());
    }

    @Bean
    @ConditionalOnMissingBean(PubRecProcessor.class)
    public PubRecProcessor pubRecProcessor(BrokerController brokerController) {
        return new PubRecProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(PubAckProcessor.class)
    public PubAckProcessor pubAckProcessor(BrokerController brokerController) {
        return new PubAckProcessor(brokerController);
    }

    @Bean
    @ConditionalOnMissingBean(PubCompProcessor.class)
    public PubCompProcessor pubCompProcessor(BrokerController brokerController) {
        return new PubCompProcessor(brokerController);
    }

}
