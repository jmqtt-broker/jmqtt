package org.jmqtt.broker.processor.dispatcher.event;

/**
 * 集群消息事件码
 */
public enum EventCode {

    CLEAR_SESSION(1,"CLEAR_SESSION","清理本节点客户端会话缓存"),

    DISPATCHER_CLIENT_MESSAGE(2,"DISPATCHER_CLIENT_MESSAGE","向集群分发客户端发送的消息"),

    DISPATCHER_WILL_MESSAGE(3,"DISPATCHER_WILL_MESSAGE","向集群分发will消息"),

    CLUSTER_NODE_STATUS(4, "CLUSTER_NODE_STATUS", "集群节点上下线通知"),

    CLUSTER_NODE_KEEPALIVE(5, "CLUSTER_NODE_KEEPALIVE", "集群节点上报心跳"),

    CLUSTER_NODE_REPORT(6, "CLUSTER_NODE_REPORT", "上报当前集群节点信息"),
    ;

    private int code;
    private String value;
    private String desc;

    EventCode(int code,String value, String desc) {
        this.code = code;
        this.value = value;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }}
