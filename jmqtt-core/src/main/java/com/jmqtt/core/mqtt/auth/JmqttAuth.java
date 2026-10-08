package com.jmqtt.core.mqtt.auth;

/**
 * Connect permission manager
 */
public interface JmqttAuth {

    /**
     * 校验某个客户端的mqtt版本是否符合要求
     * @param version  mqtt版本
     * @return  返回值
     */
    default boolean versionVerify(int version) {
        return true;
    }

    /**
     * 校验某个客户端的clientId是否符合要求
     * @param clientId  客户端clientId
     * @return  返回值
     */
    default boolean clientIdVerify(String clientId) {
        return true;
    }

    /**
     * 校验某个客户端是否在黑名单中
     * @param remoteAddr    远程IP
     * @param clientId      客户端clientId
     * @return  返回值
     */
    default boolean blacklistVerify(String remoteAddr,String clientId) {
        return true;
    }

    /**
     * verify the clientId,username,password whether true or not
     * @param clientId      客户端clientId
     * @param userName      用户名
     * @param password      密码
     * @param defaultUser   默认用户
     * @param defaultPwd    默认密码
     * @param anonymousEnable   是否允许匿名登录
     * @return  返回值
     */
    default boolean authentication(String clientId,String userName,byte[] password,
                           String defaultUser, String defaultPwd, boolean anonymousEnable) {
        return anonymousEnable || defaultUser.equals(userName) && defaultPwd.equals(new String(password));
    }

    /**
     * verify the client's heartbeat time whether the compliance
     * @param clientId  客户端clientId
     * @param time      心跳周期
     * @return  返回值
     */
    default boolean heartbeatTimeVerify(String clientId,int time) {
        return true;
    }

    /**
     * 校验某个客户端是否可以发布消息到指定主题上
     * @param clientId  客户端clientId
     * @param topic     发布的主题
     * @return  返回值
     */
    default boolean publishVerify(String clientId,String topic) {
        return true;
    }

    /**
     * 校验某个客户端是否可以订阅指定主题
     * @param clientId  客户端clientId
     * @param topic     订阅的主题
     * @return  返回值
     */
    default boolean subscribeVerify(String clientId,String topic) {
        return true;
    }
}
