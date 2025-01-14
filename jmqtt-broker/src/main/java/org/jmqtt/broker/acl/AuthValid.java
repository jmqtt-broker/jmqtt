package org.jmqtt.broker.acl;

/**
 * Connect permission manager
 */
public interface AuthValid {

    void start();

    void shutdown();

    /**
     * verify the clientId whether it meets the requirements or not
     * @param clientId  客户端clientId
     * @return  返回值
     */
    boolean clientIdVerify(String clientId);

    /**
     * if the client is on blacklist,is not allowed to connect
     * @param remoteAddr    远程IP
     * @param clientId      客户端clientId
     * @return  返回值
     */
    boolean onBlacklist(String remoteAddr,String clientId);

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
    boolean authentication(String clientId,String userName,byte[] password,
                           String defaultUser, String defaultPwd, boolean anonymousEnable);

    /**
     * verify the client's heartbeat time whether the compliance
     * @param clientId  客户端clientId
     * @param time      心跳周期
     * @return  返回值
     */
    boolean verifyHeartbeatTime(String clientId,int time);

    /**
     * verify the clientId whether can publish message to the topic
     * @param clientId  客户端clientId
     * @param topic     发布的主题
     * @return  返回值
     */
    boolean publishVerify(String clientId,String topic);

    /**
     * verify the clientId whether can subscribe the topic
     * @param clientId  客户端clientId
     * @param topic     订阅的主题
     * @return  返回值
     */
    boolean subscribeVerify(String clientId,String topic);
}
