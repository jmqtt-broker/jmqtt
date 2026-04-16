package org.jmqtt.common.akka;

public interface AkkaConst {

    String SYSTEM_PROTOCOL = "akka";

    /**
     * akka集群名称
     */
    String SYSTEM_NAME = "JMqttDispatcherSystem";

    /**
     * akka集群中角色，keeper为中央节点，存储集群中所有数据，一个集群中最好只设置1~2个keeper节点
     */
    String KEEPER = "keeper";

    /**
     * akka集群中普通工作节点，只保存本节点连接的客户端数据
     */
    String WORKER = "worker";

    /**
     * 集群广播，每个节点都能收到
     */
    String CLUSTER_EVENT = "ClusterEvent";

    /**
     * 集群广播订阅actor名称
     */
    String CLUSTER_EVENT_SUBSCRIBER = "ClusterEventSubscriber";

    /**
     * 只广播给keeper角色的节点
     */
    String KEEPER_TOPIC = "KeeperTopic";

    /**
     * keeper节点接收消息的actor名称
     */
    String KEEPER_SUBSCRIBER = "KeeperSubscriber";

    /**
     * keeper路由，消息只发送给一个keeper
     */
    String KEEPER_ROUTER = "KeeperRouter";


    /**
     * 集群事件监听actor
     */
    String CLUSTER_LISTENER = "ClusterListener";

    /**
     * 点对点通知actor
     */
    String AKKA_RECEIVER = "AkkaReceiver";

}
