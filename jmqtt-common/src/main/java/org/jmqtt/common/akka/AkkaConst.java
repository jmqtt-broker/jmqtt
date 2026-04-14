package org.jmqtt.common.akka;

public interface AkkaConst {

    String SYSTEM_PROTOCOL = "akka";

    /**
     * akka集群名称
     */
    String SYSTEM_NAME = "JMqttDispatcherSystem";

    /**
     * 集群广播，每个节点都能收到
     */
    String CLUSTER_EVENT = "ClusterEvent";

    /**
     * 集群广播订阅actor名称
     */
    String CLUSTER_EVENT_SUBSCRIBER = "ClusterEventSubscriber";

    /**
     * 集群事件监听actor
     */
    String CLUSTER_LISTENER = "ClusterListener";

    /**
     * 点对点通知actor
     */
    String AKKA_RECEIVER = "AkkaReceiver";

}
