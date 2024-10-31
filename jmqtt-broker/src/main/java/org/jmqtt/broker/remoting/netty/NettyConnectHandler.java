package org.jmqtt.broker.remoting.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.remoting.util.RemotingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NettyConnectHandler extends ChannelDuplexHandler {

    private static final Logger log = JmqttLogger.remotingLog;

    private NettyEventExecutor eventExecutor;

    private Map<String, Integer> mapTimes = new HashMap<>();

    public NettyConnectHandler(NettyEventExecutor nettyEventExecutor) {
        this.eventExecutor = nettyEventExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final String remoteAddr = RemotingHelper.getRemoteAddr(ctx.channel());
        LogUtil.info(log, "[ChannelActive] -> addr = {}", remoteAddr);
        this.eventExecutor.putNettyEvent(new NettyEvent(remoteAddr, NettyEventType.CONNECT, ctx.channel()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        final String remoteAddr = RemotingHelper.getRemoteAddr(ctx.channel());
        LogUtil.info(log, "[ChannelInactive] -> addr = {}", remoteAddr);
        this.eventExecutor.putNettyEvent(new NettyEvent(remoteAddr, NettyEventType.CLOSE, ctx.channel()));
        String clientId = NettyUtil.getClientId(ctx.channel());
        mapTimes.remove(clientId);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        LogUtil.debug(log, "[HEART_BEAT]->已经在规定时间内没有收到消息了");
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                String clientId = NettyUtil.getClientId(ctx.channel());
                Integer time = mapTimes.get(clientId);
                time = time == null ? 1 : time + 1;
                mapTimes.put(clientId, time);
                LogUtil.debug(log, "[HEART_BEAT]->channelId:{{}},times:{{}}", clientId, time);
                if (time > 3) {
                    final String remoteAddr = RemotingHelper.getRemoteAddr(ctx.channel());

                    LogUtil.warn(log, "[HEART_BEAT] -> IDLE exception, addr = {}", remoteAddr);
                    RemotingHelper.closeChannel(ctx.channel());
                    this.eventExecutor.putNettyEvent(new NettyEvent(remoteAddr, NettyEventType.IDLE, ctx.channel()));
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        String remoteAddr = RemotingHelper.getRemoteAddr(ctx.channel());
//        LogUtil.warn(log, "Channel->clientId:{} caught Exception remotingAddr:{},cause:{}",NettyUtil.getClientId(ctx.channel()), remoteAddr, cause);
//        RemotingHelper.closeChannel(ctx.channel());
//        this.eventExecutor.putNettyEvent(new NettyEvent(remoteAddr, NettyEventType.EXCEPTION, ctx.channel()));
    }
}
