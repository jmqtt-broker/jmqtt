package com.jmqtt.boot.starter.config;

import com.jmqtt.core.JmqttBroker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StarterBoot {

    @Bean
    public JmqttBroker startBroker(StarterConfig config) throws Exception {
        JmqttBroker broker = new JmqttBroker(config.getConfig());
        broker.startJmqtt();
        return broker;
    }

}
