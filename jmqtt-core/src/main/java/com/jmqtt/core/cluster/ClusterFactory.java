package com.jmqtt.core.cluster;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClusterFactory {

    private static HazelcastInstance CLUSTER;

    public static void createCluster(ClusterConfig clusterConfig) {
        if (CLUSTER == null) {
            Config config = new Config();
            config.setClusterName(clusterConfig.getClusterName());
            config.setInstanceName(clusterConfig.getCurrentNodeName());

            NetworkConfig network = config.getNetworkConfig();
            network.setPort(clusterConfig.getPort());
            // network.getPortConfig().setAutoIncrement(true);
            // network.getPortConfig().setPortCount(100);

            JoinConfig join = network.getJoin();
            join.getAutoDetectionConfig().setEnabled(false);
            join.getMulticastConfig().setEnabled(false);

            TcpIpConfig tcpIpConfig = join.getTcpIpConfig().setEnabled(true);
            clusterConfig.getSeedNodes().forEach(tcpIpConfig::addMember);

            MapConfig sessionMap = new MapConfig("jmqtt-sessions");
            sessionMap.setAsyncBackupCount(1).setReadBackupData(true);

            MapConfig subscriptionMap = new MapConfig("jmqtt-subscriptions");
            subscriptionMap.setAsyncBackupCount(1).setReadBackupData(true);

            MapConfig retainedMap = new MapConfig("jmqtt-retained");
            retainedMap.setAsyncBackupCount(1).setReadBackupData(true);

            config.addMapConfig(sessionMap);
            config.addMapConfig(subscriptionMap);
            config.addMapConfig(retainedMap);
            CLUSTER = Hazelcast.newHazelcastInstance(config);
        }
    }

}
