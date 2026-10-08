
package com.jmqtt.core.mqtt.store;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;

/**
 * 会话状态
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionState {

    private String brokerId;

    private String clientId;

    private StateEnum state;

    private Long onlineTime;

    private Long offlineTime;

    private Map<Integer, Object> propertyMap;

    private Integer version;

    private String address;

    private Boolean cleanStart;

    private Integer keepalive;

    public SessionState(String clientId, StateEnum state) {
        this.clientId = clientId;
        this.state = state;
    }

    public SessionState(StateEnum state, String clientId, Integer version,
                        String address, long onlineTime, Boolean cleanStart,
                        Integer keepalive) {
        this.state = state;
        this.clientId = clientId;
        this.onlineTime = onlineTime;
        this.version = version;
        this.address = address;
        this.cleanStart = cleanStart;
        this.keepalive = keepalive;
    }

    public SessionState(StateEnum state, long offlineTime, Integer version) {
        this.state = state;
        this.offlineTime = offlineTime;
        this.version = version;
    }

    // public SessionState(SessionDO sessionDO) {
    //     try {
    //         MixAll.copyProperties(sessionDO, this);
    //         this.state = StateEnum.valueOf(sessionDO.getState());
    //         String property = sessionDO.getProperty();
    //         this.propertyMap = StringUtils.isNotBlank(property) ? new HashMap<Integer, Object>(JSONObject.parseObject(property, Map.class)) : null;
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    @Override
    public String toString() {
        return new JSONObject(this).toString();
    }

    @Getter
    public enum StateEnum {
        /**
         * 从未连接过或之前连接过但cleanStart=1的
         */
        NEVER_CONNECTED("NEVER_CONNECTED"),
        /**
         * 在线
         */
        ONLINE("ONLINE"),
        /**
         * cleanStart=0，已离线的
         */
        OFFLINE("OFFLINE"),
        ;

        private String code;

        StateEnum(String code) {
            this.code = code;
        }

    }
}
