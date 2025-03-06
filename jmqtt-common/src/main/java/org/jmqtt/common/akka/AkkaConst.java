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
    String SLAVE = "worker";

}
