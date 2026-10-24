package com.jmqtt.core.cluster;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ClusterConfig {

    private Boolean enable = false;

    private String clusterName = "JMQTT-CLUSTER";

    private String currentNodeName;

    private Integer port = 5701;

    private List<String> seedNodes = List.of("127.0.0.1:5701");

    public String getCurrentNodeName() {
        if (StrUtil.isBlank(currentNodeName)) {
            try {
                this.currentNodeName = "JMQTT-CLUSTER-NODE-" + InetAddress.getLocalHost().getHostAddress() + ":" + port;
            } catch (UnknownHostException e) {
                this.currentNodeName = "JMQTT-CLUSTER-NODE-UNKNOWN" + IdWorker.getIdStr();
            }
        }
        return currentNodeName;
    }

}
