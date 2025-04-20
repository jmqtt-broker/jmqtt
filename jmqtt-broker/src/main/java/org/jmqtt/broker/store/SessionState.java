
package org.jmqtt.broker.store;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;

import java.util.HashMap;
import java.util.Map;

/**
 * 会话状态
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionState {

    private String brokerId = BrokerContext.getBrokerId();

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

    public SessionState(SessionDO sessionDO) {
        try {
            MixAll.copyProperties(sessionDO, this);
            this.state = StateEnum.valueOf(sessionDO.getState());
            String property = sessionDO.getProperty();
            this.propertyMap = StringUtils.isNotBlank(property) ? new HashMap<Integer, Object>(JSONObject.parseObject(property, Map.class)) : null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
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
