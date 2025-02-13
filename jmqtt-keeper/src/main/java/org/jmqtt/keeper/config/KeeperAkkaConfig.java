package org.jmqtt.keeper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "jmqtt.keeper.akka")
@Getter
@Setter
public class KeeperAkkaConfig {

    private String systemName = "JMqttKeeperSystem";
    private String host;
    private String port;
    private List<String> clusterNodes;

}
