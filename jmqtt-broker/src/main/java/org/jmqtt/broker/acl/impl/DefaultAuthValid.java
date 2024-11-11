package org.jmqtt.broker.acl.impl;

import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.slf4j.Logger;


public class DefaultAuthValid implements AuthValid {

    private static final Logger log = JmqttLogger.remotingLog;

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean clientIdVerify(String clientId) {
        return true;
    }

    @Override
    public boolean onBlacklist(String remoteAddr, String clientId) {
        return false;
    }

    @Override
    public boolean authentication(String clientId, String userName, byte[] password,
                                  String defaultUser, String defaultPwd, boolean anonymousEnable) {
        LogUtil.info(log, "clientId:{}", clientId);
        return anonymousEnable || defaultUser.equals(userName) && defaultPwd.equals(new String(password));
    }

    @Override
    public boolean verifyHeartbeatTime(String clientId, int time) {
        return true;
    }

    @Override
    public boolean publishVerify(String clientId, String topic) {
        return true;
    }

    @Override
    public boolean subscribeVerify(String clientId, String topic) {
        return true;
    }
}
