package org.jmqtt.broker.acl;

/**
 * Connect permission manager
 */
public interface AuthValid {

    default void start() {

    }

    default void shutdown() {

    }

    /**
     * verify the clientId whether it meets the requirements or not
     * @param clientId  客户端clientId
     * @return  返回值
     */
    default boolean clientIdVerify(String clientId) {
        return true;
    }

    /**
     * if the client is on blacklist,is not allowed to connect
     * @param remoteAddr    远程IP
     * @param clientId      客户端clientId
     * @return  返回值
     */
    default boolean onBlacklist(String remoteAddr,String clientId) {
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
        return true;
    }

    /**
     * verify the client's heartbeat time whether the compliance
     * @param clientId  客户端clientId
     * @param time      心跳周期
     * @return  返回值
     */
    default boolean verifyHeartbeatTime(String clientId,int time) {
        return true;
    }

    /**
     * verify the clientId whether can publish message to the topic
     * @param clientId  客户端clientId
     * @param topic     发布的主题
     * @return  返回值
     */
    default boolean publishVerify(String clientId,String topic) {
        return true;
    }

    /**
     * verify the clientId whether can subscribe the topic
     * @param clientId  客户端clientId
     * @param topic     订阅的主题
     * @return  返回值
     */
    default boolean subscribeVerify(String clientId,String topic) {
        return true;
    }
}
