package org.jmqtt.starter.plugins;

import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.acl.impl.DefaultAuthValid;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/3/27 14:08
 */
@Slf4j
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
        log.info("客户端ID: {}", clientId);
        return true;
    }

    @Override
    public boolean onBlacklist(String remoteAddr, String clientId) {
        log.info("是否在白名单: {}", remoteAddr + " | " + clientId);
        return false;
    }

    @Override
    public boolean authentication(String clientId, String userName, byte[] password) {
        log.info("登录校验: {}", clientId + " | " + userName + " | " + new String(password));
        return true;
    }

    @Override
    public boolean verifyHeartbeatTime(String clientId, int time) {
        log.info("心跳检测: {}", clientId + " | " + time);
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
