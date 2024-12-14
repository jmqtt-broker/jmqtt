package org.jmqtt.starter.plugins;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.client.ClientLifeCycleHookService;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2021/8/2 20:25
 */
@Service
@Slf4j
public class ConnectCycleHookService extends ClientLifeCycleHookService {

    public ConnectCycleHookService(SubscriptionMatcher subscriptionMatcher,
                                   SessionStore sessionStore,
                                   MessageStore messageStore,
                                   InnerMessageDispatcher innerMessageDispatcher) {
        super(sessionStore, messageStore, subscriptionMatcher, innerMessageDispatcher);
    }

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        String clientId = NettyUtil.getClientId(channel);
        log.info("连接建立client：{}", clientId);
        super.onChannelConnect(remoteAddr, channel);
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        log.info("Read超时：{}", NettyUtil.getClientId(channel));
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        String clientId = NettyUtil.getClientId(channel);
        log.info("连接断开client：{}", clientId);
        super.onChannelClose(remoteAddr, channel);
    }

}
