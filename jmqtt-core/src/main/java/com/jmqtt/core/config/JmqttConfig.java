package com.jmqtt.core.config;

import cn.hutool.json.JSONObject;
import cn.hutool.setting.yaml.YamlUtil;
import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.netty.config.NettyConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JmqttConfig {

    private BrokerConfig broker;

    private NettyConfig netty;

    private ClusterConfig cluster;

    public static void main(String[] args) {
        JmqttConfig config = YamlUtil.load(JmqttConfig.class.getClassLoader().getResourceAsStream("jmqtt.yml"), JmqttConfig.class);
        System.out.println(new JSONObject(config));
    }

}
