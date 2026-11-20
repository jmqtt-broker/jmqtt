package com.jmqtt.boot.starter.config;

import com.jmqtt.core.JmqttBroker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class StarterBoot {

    @Bean
    @ConditionalOnMissingBean(JmqttBroker.class)
    @ConditionalOnBean(DataSource.class)
    public JmqttBroker startBrokerWithDataSource(StarterConfig config, DataSource dataSource) throws Exception {
        JmqttBroker broker = new JmqttBroker(config.getConfig(), dataSource);
        broker.startJmqtt();
        return broker;
    }

    @Bean
    @ConditionalOnMissingBean(value = {JmqttBroker.class, DataSource.class})
    public JmqttBroker startBroker(StarterConfig config) throws Exception {
        JmqttBroker broker = new JmqttBroker(config.getConfig());
        broker.startJmqtt();
        return broker;
    }

}
