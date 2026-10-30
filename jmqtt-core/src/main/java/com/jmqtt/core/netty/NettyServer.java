package com.jmqtt.core.netty;

import com.jmqtt.core.common.Control;
import com.jmqtt.core.common.JmqttConst;
import com.jmqtt.core.common.ThreadFactoryImpl;
import com.jmqtt.core.mqtt.control.BaseCtrl;
import com.jmqtt.core.netty.coder.WebSocketDecoder;
import com.jmqtt.core.netty.coder.WebSocketEncoder;
import com.jmqtt.core.netty.config.NettyConfig;
import com.jmqtt.core.netty.config.TLS;
import com.jmqtt.core.netty.event.ChannelEventListener;
import com.jmqtt.core.netty.helper.ChannelUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class NettyServer implements Control {

    private final NettyConfig nettyConfig;
    private int maxMsgSize = 256 * 1024;
    private final EventLoopGroup selectorGroup;
    private final EventLoopGroup ioGroup;
    private final Class<? extends ServerChannel> clazz;
    private final Map<MqttMessageType, BaseCtrl> ctrlTable;
    private final ChannelEventExecutor nettyEventExecutor;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public NettyServer(NettyConfig nettyConfig, ChannelEventListener listener) {
        TLS tls = nettyConfig.getTls();
        if (tls != null) {
            tls.checkTlsConfig();
        } else {
            nettyConfig.setTls(TLS.defaultConfig());
        }
        this.nettyConfig = nettyConfig;
        this.ctrlTable = new HashMap<>();
        this.nettyEventExecutor = new ChannelEventExecutor(listener);
        int coreThreadNum = Runtime.getRuntime().availableProcessors();
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux") && nettyConfig.getUseEpoll()) {
            selectorGroup = new EpollEventLoopGroup(coreThreadNum,
                    new ThreadFactoryImpl("SelectorEventGroup"));
            ioGroup = new EpollEventLoopGroup(coreThreadNum * 2,
                    new ThreadFactoryImpl("IOEventGroup"));
            clazz = EpollServerSocketChannel.class;
        } else if (osName.contains("mac")) {
            selectorGroup = new KQueueEventLoopGroup(coreThreadNum,
                    new ThreadFactoryImpl("SelectorEventGroup"));
            ioGroup = new KQueueEventLoopGroup(coreThreadNum * 2,
                    new ThreadFactoryImpl("IOEventGroup"));
            clazz = KQueueServerSocketChannel.class;
        } else {
            selectorGroup = new NioEventLoopGroup(coreThreadNum,
                    new ThreadFactoryImpl("SelectorEventGroup"));
            ioGroup = new NioEventLoopGroup(coreThreadNum * 2,
                    new ThreadFactoryImpl("IOEventGroup"));
            clazz = NioServerSocketChannel.class;
        }
    }

    @Override
    public void start() {
        //Netty event executor start
        this.nettyEventExecutor.start();
        // start TCP server
        if (nettyConfig.getEnableTcp()) {
            startTcpServer(false, nettyConfig.getTcpPort());
        }

        if (nettyConfig.getEnableSslTcp()) {
            startTcpServer(true, nettyConfig.getSslTcpPort());
        }

        if (nettyConfig.getEnableWebsocket()) {
            startWebsocketServer(false, nettyConfig.getWebsocketPort());
        }

        if (nettyConfig.getEnableSslWebsocket()) {
            startWebsocketServer(true, nettyConfig.getSslWebsocketPort());
        }
    }

    private void startWebsocketServer(boolean useSsl, Integer port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(selectorGroup, ioGroup)
                .channel(clazz)
                .option(ChannelOption.SO_BACKLOG, nettyConfig.getTcpBackLog())
                .childOption(ChannelOption.TCP_NODELAY, nettyConfig.getNoDelay())
                .childOption(ChannelOption.SO_SNDBUF, nettyConfig.getTcpSndBuf())
                .option(ChannelOption.SO_RCVBUF, nettyConfig.getTcpRcvBuf())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.getSoKeepAlive())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if (useSsl) {
                            pipeline.addLast("ssl", nettyConfig.getTls().getSslHandler(socketChannel));
                        }
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, 0))
                                .addLast("httpCodec", new HttpServerCodec())
                                .addLast("aggregator", new HttpObjectAggregator(65535))
                                .addLast("compressor", new HttpContentCompressor())
                                .addLast("webSocketHandler", new WebSocketServerProtocolHandler("/mqtt", JmqttConst.MQTT_VERSION_SUPPORT, false, 65536))
                                .addLast("webSocketDecoder", new WebSocketDecoder())
                                .addLast("webSocketEncoder", new WebSocketEncoder())
                                .addLast("mqttDecoder", new MqttDecoder(maxMsgSize))
                                .addLast("mqttEncoder", MqttEncoder.INSTANCE)
                                .addLast("nettyConnectionManager", new NettyConnectHandler(nettyEventExecutor))
                                .addLast("mqttHandler", new MqttHandler());
                    }
                });
        if (nettyConfig.getPooledByteBufAllocatorEnable()) {
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        try {
            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("Jmqtt webSocket server started, ssl = {}, listening port = {}", useSsl, port);
        } catch (InterruptedException ex) {
            log.info("Jmqtt webSocket server started failure, ssl = {}", useSsl, ex);
        }
    }

    private void startTcpServer(boolean useSsl, Integer port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(selectorGroup, ioGroup)
                .channel(clazz)
                .option(ChannelOption.SO_BACKLOG, nettyConfig.getTcpBackLog())
                .childOption(ChannelOption.TCP_NODELAY, nettyConfig.getNoDelay())
                .childOption(ChannelOption.SO_SNDBUF, nettyConfig.getTcpSndBuf())
                .option(ChannelOption.SO_RCVBUF, nettyConfig.getTcpRcvBuf())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.getSoKeepAlive())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        if (useSsl) {
                            pipeline.addLast("ssl", nettyConfig.getTls().getSslHandler(socketChannel));
                        }
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(60, 0, 60))
                                .addLast("mqttEncoder", MqttEncoder.INSTANCE)
                                .addLast("mqttDecoder", new MqttDecoder(maxMsgSize))
                                .addLast("nettyConnectionManager", new NettyConnectHandler(
                                        nettyEventExecutor))
                                .addLast("mqttHandler", new MqttHandler());
                    }
                });
        if (nettyConfig.getPooledByteBufAllocatorEnable()) {
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        try {
            bootstrap.bind(port).sync();
            log.info("Jmqtt tcp server started, ssl = {}, listening port = {}", useSsl, port);
        } catch (InterruptedException ex) {
            log.info("Jmqtt tcp server start failure, ssl = {}", useSsl, ex);
        }
    }


    @Override
    public void stop() {
        if (selectorGroup != null) {
            selectorGroup.shutdownGracefully();
        }
        if (ioGroup != null) {
            ioGroup.shutdownGracefully();
        }
        nettyEventExecutor.stop();
    }

    public void registerCtrl(MqttMessageType mqttType, BaseCtrl ctrl) {
        this.ctrlTable.put(mqttType, ctrl);
    }

    class MqttHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object obj) {
            MqttMessage mqttMessage = (MqttMessage) obj;
            if (mqttMessage != null && mqttMessage.decoderResult().isSuccess()) {
                MqttMessageType messageType = mqttMessage.fixedHeader().messageType();
                log.debug("clientId:{} receive mqtt code,type:{},name:{},payload:[{}]", ChannelUtil.getClientId(ctx.channel()), messageType.value(), messageType.name(), mqttMessage.payload());
                executorService.submit(() -> ctrlTable.get(messageType).processCtrl(ctx, mqttMessage));
            } else {
                log.warn("Mqtt解码失败。");
                ctx.close();
            }
        }
    }
}
