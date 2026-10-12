package com.jmqtt.core.netty;

import com.jmqtt.core.netty.event.ChannelEvent;
import com.jmqtt.core.netty.event.NettyEventType;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.netty.helper.ChannelHelper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyConnectHandler extends ChannelDuplexHandler {

    private final ChannelEventExecutor eventExecutor;

    public NettyConnectHandler(ChannelEventExecutor nettyEventExecutor) {
        this.eventExecutor = nettyEventExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final String remoteAddr = ChannelHelper.getRemoteAddr(ctx.channel());
        log.debug("Jmqtt netty server channelActive, remoteAddr = {}", remoteAddr);
        this.eventExecutor.putEvent(ChannelEvent.of(remoteAddr, NettyEventType.CONNECT, ctx.channel()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        final String remoteAddr = ChannelHelper.getRemoteAddr(ctx.channel());
        log.debug("Jmqtt netty server channelInactive, remoteAddr = {}", remoteAddr);
        this.eventExecutor.putEvent(ChannelEvent.of(remoteAddr, NettyEventType.CLOSE, ctx.channel()));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {
            log.debug("Jmqtt netty channel heartbeat timeout.");
            if (event.state().equals(IdleState.READER_IDLE)) {
                final String remoteAddr = ChannelHelper.getRemoteAddr(ctx.channel());
                this.eventExecutor.putEvent(ChannelEvent.of(remoteAddr, NettyEventType.IDLE, ctx.channel()));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String remoteAddr = ChannelHelper.getRemoteAddr(ctx.channel());
        log.warn("Jmqtt netty channel exception, clientId = {}, remoteAddr = {}", ChannelUtil.getClientId(ctx.channel()), remoteAddr, cause);
        this.eventExecutor.putEvent(ChannelEvent.of(remoteAddr, NettyEventType.EXCEPTION, ctx.channel()));
    }
}
