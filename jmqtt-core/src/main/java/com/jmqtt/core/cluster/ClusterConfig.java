package com.jmqtt.core.cluster;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ClusterConfig {

    private String clusterName = "JMQTT-CLUSTER";

    private String currentNodeName = "JMQTT-CLUSTER-NODE-" + IdWorker.getIdStr();

    private Integer port = 5701;

    private List<String> seedNodes = List.of("127.0.0.1:5701");

}
