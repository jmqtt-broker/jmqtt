package org.jmqtt.common.event;

import lombok.Getter;

/**
 * 集群消息事件码
 */
@Getter
public enum EventCode {

    CLEAR_SESSION(1,"CLEAR_SESSION","清理本节点客户端会话缓存"),

    DISPATCHER_CLIENT_MESSAGE(2,"DISPATCHER_CLIENT_MESSAGE","向集群分发客户端发送的消息"),

    DISPATCHER_WILL_MESSAGE(3,"DISPATCHER_WILL_MESSAGE","向集群分发will消息"),

    BROKER_STATE(4, "CLUSTER_NODE_STATUS", "集群节点上下线通知"),

    SESSION_STATE(5, "CLUSTER_SESSION_STATE", "集群客户端上下线"),

    SUBSCRIPTION_STATE(6, "CLUSTER_SUBSCRIPTION", "订阅/取消订阅"),
    ;

    private int code;
    private String value;
    private String desc;

    EventCode(int code,String value, String desc) {
        this.code = code;
        this.value = value;
        this.desc = desc;
    }

}
