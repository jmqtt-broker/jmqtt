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

    BROKER_STATE(4, "BROKER_STATE", "集群节点上下线通知"),

    SESSION_STATE(5, "SESSION_STATE", "集群客户端上下线"),
    SESSION_STATE_RESPONSE(6, "SESSION_STATE_RESPONSE", "客户端上下线，Keeper节点回复worker节点"),

    SUBSCRIPTION(7, "SUBSCRIPTION", "订阅"),
    SUBSCRIPTION_RESPONSE(8, "SUBSCRIPTION_RESPONSE", "Keeper订阅后返回保留消息"),
    UNSUBSCRIPTION(9, "UNSUBSCRIPTION", "取消订阅"),

    STORE_RETAIN_MSG(21, "STORE_RETAIN_MSG", "保存保留消息"),
    CLEAR_RETAIN_MSG(22, "CLEAR_RETAIN_MSG", "删除保留消息"),
    GET_RETAIN_MSG(23, "GET_RETAIN_MSG", "查询Keeper中的保留消息"),
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
