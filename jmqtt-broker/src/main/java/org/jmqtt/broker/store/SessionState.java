
package org.jmqtt.broker.store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 会话状态
 */
@Getter
@Setter
@AllArgsConstructor
public class SessionState {


    private StateEnum state;

    private long offlineTime;

    private Map<Integer, Object> propertyMap;

    private Integer version;

    public SessionState(StateEnum state) {
        this.state = state;
    }

    public SessionState(StateEnum state, Integer version) {
        this.state = state;
        this.version = version;
    }

    public SessionState(StateEnum state, long offlineTime, Integer version) {
        this.state = state;
        this.offlineTime = offlineTime;
        this.version = version;
    }

    @Getter
    public enum StateEnum {
        /**
         * 从未连接过（之前 cleanStart为1 的也为为NULL）
         */
        NULL("NULL"),
        /**
         * 在线
         */
        ONLINE("ONLINE"),
        /**
         * cleanStart为0，且连接过Jmqtt集群，已离线，会返回offlineTime（离线时间）
         */
        OFFLINE("OFFLINE"),
        ;

        private String code;

        StateEnum(String code) {
            this.code = code;
        }

    }
}
