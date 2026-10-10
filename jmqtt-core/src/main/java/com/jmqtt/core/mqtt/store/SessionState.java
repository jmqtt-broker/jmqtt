
package com.jmqtt.core.mqtt.store;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jmqtt.core.mqtt.store.persist.po.Session;
import lombok.*;

import java.util.Map;

/**
 * 会话状态
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    public SessionState(Session session) {
        BeanUtil.copyProperties(session, this);
        this.propertyMap = MapUtil.newHashMap();
        JSONUtil.parseObj(session.getProperty()).forEach((k, v) -> {
            this.propertyMap.put(Integer.parseInt(k), v);
        });
    }

    public SessionState(StateEnum state, long offlineTime, Integer version) {
        this.state = state;
        this.offlineTime = offlineTime;
        this.version = version;
    }

    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
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
