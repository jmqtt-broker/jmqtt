package com.jmqtt.core.netty.event;

import cn.hutool.core.util.StrUtil;
import com.jmqtt.core.netty.helper.ChannelUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultChannelEventListener implements ChannelEventListener {

    public DefaultChannelEventListener() {

    }

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        log.info("onChannelConnect, remoteAddr:[{}],Channel:{{}}", remoteAddr, channel.toString());
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        log.info("onChannelClose, remoteAddr:[{}],Channel:{{}}", remoteAddr, channel.toString());
        String clientId = ChannelUtil.getClientId(channel);
        if (StrUtil.isNotEmpty(clientId)) {

        }
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        String clientId = ChannelUtil.getClientId(channel);
        log.debug("onChannelIdle, remoteAddr:[{}],ClientId:{{}}", remoteAddr, clientId);

    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        String clientId = ChannelUtil.getClientId(channel);
        log.warn("onChannelException, remoteAddr:[{}],ClientId:{{}}", remoteAddr, clientId);

    }

}
