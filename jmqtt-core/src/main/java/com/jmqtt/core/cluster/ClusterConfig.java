package com.jmqtt.core.cluster;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ClusterConfig {

    private String clusterName;

    private String currentNodeName;

    private Integer port;

    private List<String> seedNodes;

}
