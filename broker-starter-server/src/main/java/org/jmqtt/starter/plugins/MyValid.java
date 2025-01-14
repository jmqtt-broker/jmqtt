package org.jmqtt.starter.plugins;

import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.acl.impl.DefaultAuthValid;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MyValid extends DefaultAuthValid {

    @Override
    public void start() {
        log.info("服务启动");
    }

    @Override
    public void shutdown() {
        log.info("服务停止");
    }

    @Override
    public boolean clientIdVerify(String clientId) {
        log.info("clientId校验: {}", clientId);
        return super.clientIdVerify(clientId);
    }

    @Override
    public boolean onBlacklist(String remoteAddr, String clientId) {
        log.info("黑名单校验: {}", remoteAddr + " | " + clientId);
        return false;
    }

    @Override
    public boolean authentication(String clientId, String userName, byte[] password,
                                  String defaultUser, String defaultPwd, boolean anonymousEnable) {
        log.info("登录校验, clientId:{}, userName: {}, password: {}, defaultUser: {}, defaultPwd: {}", clientId, userName, new String(password), defaultUser, defaultPwd);
        return super.authentication(clientId, userName, password, defaultUser, defaultPwd, anonymousEnable);
    }

    @Override
    public boolean verifyHeartbeatTime(String clientId, int time) {
        log.info("心跳校验: {}", clientId + " | " + time);
        return true;
    }

    @Override
    public boolean publishVerify(String clientId, String topic) {
        log.info("发布topic校验: {}", clientId + " | " + topic);
        return true;
    }

    @Override
    public boolean subscribeVerify(String clientId, String topic) {
        log.info("订阅topic校验: {}", clientId + " | " + topic);
        return true;
    }

}
