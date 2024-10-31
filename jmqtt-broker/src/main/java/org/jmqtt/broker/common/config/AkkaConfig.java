package org.jmqtt.broker.common.config;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/10/31 11:25
 */
@Getter
@Setter
public class AkkaConfig {

    private Boolean enable = false;
    private String systemName = "JMqttDispatcherSystem";
    private String host;
    private String port;
    private List<String> clusterNodes;

    public String configStr() {
        JSONObject wrap = new JSONObject();
        JSONObject akka = new JSONObject();
        wrap.put("akka", akka);
        JSONObject remote = new JSONObject();
        JSONObject artery = new JSONObject();
        JSONObject canonical = new JSONObject();
        remote.put("artery", artery);
        artery.put("canonical", canonical);
        canonical.put("hostname", this.host);
        canonical.put("port", this.port);
        JSONObject cluster = new JSONObject();
        if (this.clusterNodes != null) {
            cluster.put("seed-nodes", this.clusterNodes.stream()
                    .map(n -> "akka://" + this.systemName + "@" + n)
                    .collect(Collectors.toList()));
        }
        akka.put("remote", remote);
        akka.put("cluster", cluster);
        return wrap.toJSONString();
    }

}
