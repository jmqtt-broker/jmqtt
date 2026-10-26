package com.jmqtt.core.cluster;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipAdapter;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.collection.IQueue;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.jmqtt.core.cluster.seria.KryoSerializer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class JmqttCluster {

    private static final String BROADCAST_TOPIC = "jmqtt-broadcast";

    private static final String INSTANCE_NAME = "instanceName";

    private static volatile HazelcastInstance CLUSTER;

    private static final Map<String, Member> memberMap = new ConcurrentHashMap<>();

    private static ClusterMessageDispatcher clusterMessageDispatcher;

    public static synchronized void start(ClusterConfig config) throws Exception {
        if (CLUSTER != null) {
            return;
        }
        if (config == null || !Boolean.TRUE.equals(config.getEnable())) {
            log.info("jmqtt broker run in local mode (cluster disabled).");
            return;
        }
        String localBrokerId = config.getCurrentNodeName();
        CLUSTER = createCluster(config);
        clusterMessageDispatcher = new ClusterMessageDispatcher(localBrokerId, CLUSTER.getTopic(JmqttCluster.BROADCAST_TOPIC), CLUSTER.getQueue(localBrokerId));
        CLUSTER.getCluster().getMembers().forEach(m -> {
            memberMap.put(m.getAttribute(INSTANCE_NAME), m);
        });
        CLUSTER.getCluster().addMembershipListener(new MembershipAdapter() {
            @Override
            public void memberAdded(MembershipEvent event) {
                Member m = event.getMember();
                log.warn("member {} joined to cluster.", m);
                memberMap.put(m.getAttribute(INSTANCE_NAME), m);
            }

            @Override
            public void memberRemoved(MembershipEvent event) {
                Member m = event.getMember();
                log.warn("member {} left cluster.", m);
                memberMap.remove(m.getAttribute(INSTANCE_NAME));
            }
        });
        log.info("jmqtt cluster initialized, localBrokerId = {}", localBrokerId);
    }

    private static HazelcastInstance createCluster(ClusterConfig config) throws Exception {
        if (CLUSTER == null) {
            Config hazelcastConfig = new Config();
            hazelcastConfig.setClusterName(config.getClusterName());
            hazelcastConfig.setInstanceName(config.getCurrentNodeName());

            hazelcastConfig.getMemberAttributeConfig().setAttribute(INSTANCE_NAME, config.getCurrentNodeName());

            // 使用Kryo序列化器
            hazelcastConfig.getSerializationConfig().setGlobalSerializerConfig(
                    new GlobalSerializerConfig().setImplementation(new KryoSerializer()).setOverrideJavaSerialization(true)
            );

            NetworkConfig network = hazelcastConfig.getNetworkConfig();
            network.setPort(config.getPort());

            JoinConfig join = network.getJoin();
            join.getAutoDetectionConfig().setEnabled(false);
            join.getMulticastConfig().setEnabled(false);

            TcpIpConfig tcpIpConfig = join.getTcpIpConfig().setEnabled(true);
            config.getSeedNodes().forEach(tcpIpConfig::addMember);

            // p2p消息
            QueueConfig queueConfig = new QueueConfig(config.getCurrentNodeName());
            queueConfig.setAsyncBackupCount(1);
            hazelcastConfig.addQueueConfig(queueConfig);

            // 广播消息
            TopicConfig broadcastTopic = new TopicConfig(BROADCAST_TOPIC);
            broadcastTopic.setMultiThreadingEnabled(true);
            hazelcastConfig.addTopicConfig(broadcastTopic);

            // MapConfig retainedMap = new MapConfig(RETAINED_MAP);
            // retainedMap.setAsyncBackupCount(1).setReadBackupData(true);
            // hazelcastConfig.addMapConfig(retainedMap);

            CLUSTER = Hazelcast.newHazelcastInstance(hazelcastConfig);
            log.info("Hazelcast cluster started, nodeName = {}, members = {}",
                    config.getCurrentNodeName(), CLUSTER.getCluster().getMembers());
        }
        return CLUSTER;
    }

    public static void shutdown() {
        if (CLUSTER != null) {
            CLUSTER.shutdown();
            CLUSTER = null;
            log.info("Hazelcast cluster shutdown.");
            clusterMessageDispatcher.stop();
        }
    }

    /**
     * 广播集群消息
     * @param message 要广播的消息对象
     */
    public static void broadcast(ClusterMessage<?> message) {
        if (CLUSTER != null) {
            clusterMessageDispatcher.broadcast(message);
        } else {
            log.warn("broadcast message failed, cluster not initialized.");
        }
    }

    /**
     * 单播集群消息
     * @param brokerId 目标broker ID
     * @param message 要单播的消息对象
     */
    public static void unicast(String brokerId, ClusterMessage<?> message) {
        if (CLUSTER != null) {
            if (!memberMap.containsKey(brokerId)) {
                log.warn("unicast message to {} failed, broker not found.", brokerId);
                return;
            }
            IQueue<ClusterMessage<?>> target = CLUSTER.getQueue(brokerId);
            clusterMessageDispatcher.unicast(target, message);
        } else {
            log.warn("unicast message to {} failed, cluster not initialized.", brokerId);
        }
    }

}
