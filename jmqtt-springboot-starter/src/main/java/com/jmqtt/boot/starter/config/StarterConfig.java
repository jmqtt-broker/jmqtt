package com.jmqtt.boot.starter.config;

import com.jmqtt.core.config.JmqttConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "jmqtt")
public class StarterConfig {

    private JmqttConfig config;

}
