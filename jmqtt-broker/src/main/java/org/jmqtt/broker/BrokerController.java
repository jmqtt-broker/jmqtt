package org.jmqtt.broker;

import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.Getter;
import lombok.Setter;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.helper.RejectHandler;
import org.jmqtt.broker.common.helper.ThreadFactoryImpl;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.DefaultDispatcherInnerMessage;
import org.jmqtt.broker.processor.dispatcher.EventConsumeHandler;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.processor.dispatcher.akka.AkkaClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.mem.MemEventHandler;
import org.jmqtt.broker.processor.dispatcher.rdb.RDBClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.redis.RedisClusterEventHandler;
import org.jmqtt.broker.processor.protocol.*;
import org.jmqtt.broker.processor.recover.ReSendMessageService;
import org.jmqtt.broker.remoting.netty.ChannelEventListener;
import org.jmqtt.broker.remoting.netty.NettyRemotingServer;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.mem.MemMessageStore;
import org.jmqtt.broker.store.mem.MemSessionStore;
import org.jmqtt.broker.store.rdb.RDBMessageStore;
import org.jmqtt.broker.store.rdb.RDBSessionStore;
import org.jmqtt.broker.store.redis.RedisMessageStore;
import org.jmqtt.broker.store.redis.RedisSessionStore;
import org.jmqtt.broker.subscribe.DefaultSubscriptionTreeMatcher;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 具体控制类：负责加载配置文件，初始化环境，启动服务等
 */
@Getter
@Setter
public class BrokerController {

    private static final Logger log = JmqttLogger.brokerlog;

    private BrokerConfig brokerConfig;
    private NettyConfig nettyConfig;

    private ExecutorService connectExecutor;
    private ExecutorService pubExecutor;
    private ExecutorService subExecutor;
    private ExecutorService pingExecutor;

    private LinkedBlockingQueue<Runnable> connectQueue;
    private LinkedBlockingQueue<Runnable> pubQueue;
    private LinkedBlockingQueue<Runnable> subQueue;
    private LinkedBlockingQueue<Runnable> pingQueue;

    private ChannelEventListener channelEventListener;
    private NettyRemotingServer remotingServer;

    private InnerMessageDispatcher innerMessageDispatcher;
    private SubscriptionMatcher subscriptionMatcher;
    private AuthValid authValid;
    private ReSendMessageService reSendMessageService;
    private SessionStore sessionStore;
    private MessageStore messageStore;
    private ClusterEventHandler clusterEventHandler;
    private EventConsumeHandler eventConsumeHandler;
    private String currentIp;
    private boolean akkaEnable = false;

    private Map<Class<?>, RequestProcessor> requestProcessorMap = new ConcurrentHashMap<>();

    public BrokerController(BrokerConfig brokerConfig, NettyConfig nettyConfig) {
        this(brokerConfig, nettyConfig, null, null, null,
                null, null, null, null);

    }

    public BrokerController(BrokerConfig brokerConfig,
                            NettyConfig nettyConfig,
                            SessionStore sessionStore,
                            MessageStore messageStore,
                            SubscriptionMatcher subscriptionMatcher,
                            ClusterEventHandler clusterEventHandler,
                            InnerMessageDispatcher innerMessageDispatcher,
                            ChannelEventListener channelEventListener,
                            AuthValid authValid) {
        this.brokerConfig = brokerConfig;
        this.nettyConfig = nettyConfig;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.subscriptionMatcher = subscriptionMatcher != null ? subscriptionMatcher : new DefaultSubscriptionTreeMatcher();
        this.clusterEventHandler = clusterEventHandler;
        this.innerMessageDispatcher = innerMessageDispatcher != null ? innerMessageDispatcher :new DefaultDispatcherInnerMessage(brokerConfig.isHighPerformance(),
                sessionStore, brokerConfig.getPollThreadNum(), this.subscriptionMatcher, this.clusterEventHandler);
        this.authValid = authValid != null ? authValid : MixAll.pluginInit(brokerConfig.getAuthValidClass());

        this.connectQueue = new LinkedBlockingQueue<>(100000);
        this.pubQueue = new LinkedBlockingQueue<>(100000);
        this.subQueue = new LinkedBlockingQueue<>(100000);
        this.pingQueue = new LinkedBlockingQueue<>(10000);
        this.currentIp = MixAll.getLocalIp();

        this.channelEventListener = channelEventListener != null ? channelEventListener : MixAll.pluginInit(brokerConfig.getChannelEventListener(),
                new Class[]{MessageStore.class, InnerMessageDispatcher.class}, new Object[]{messageStore, innerMessageDispatcher});
        this.remotingServer = new NettyRemotingServer(brokerConfig, nettyConfig, channelEventListener);

        {
            AkkaConfig akkaConfig = brokerConfig.getAkka();
            if (this.akkaEnable = (akkaConfig != null && akkaConfig.getEnable() || brokerConfig.isAkkaEnable())) {
                this.clusterEventHandler = MixAll.pluginInit(AkkaClusterEventHandler.class);
            }
            String store = brokerConfig.getStore();
            if (SessionStore.RDB.equals(store)) {
                if (this.sessionStore == null) {
                    this.sessionStore = MixAll.pluginInit(RDBSessionStore.class);
                }
                if (this.messageStore == null) {
                    this.messageStore = MixAll.pluginInit(RDBMessageStore.class);
                }
                if (this.clusterEventHandler == null) {
                    this.clusterEventHandler = MixAll.pluginInit(RDBClusterEventHandler.class);
                }
            } else if (SessionStore.REDIS.equals(store)) {
                if (this.sessionStore == null) {
                    this.sessionStore = MixAll.pluginInit(RedisSessionStore.class);
                }
                if (this.messageStore == null) {
                    this.messageStore = MixAll.pluginInit(RedisMessageStore.class);
                }
                if (this.clusterEventHandler == null) {
                    this.clusterEventHandler = MixAll.pluginInit(RedisClusterEventHandler.class);
                }
            } else {
                if (this.sessionStore == null) {
                    this.sessionStore = MixAll.pluginInit(MemSessionStore.class);
                }
                if (this.messageStore == null) {
                    this.messageStore = MixAll.pluginInit(MemMessageStore.class);
                }
                if (this.clusterEventHandler == null) {
                    this.clusterEventHandler = MixAll.pluginInit(MemEventHandler.class);
                }
            }
        }

        this.eventConsumeHandler = new EventConsumeHandler(this);
        this.reSendMessageService = new ReSendMessageService(this);

        int coreThreadNum = Runtime.getRuntime().availableProcessors();
        this.connectExecutor = new ThreadPoolExecutor(coreThreadNum * 2,
                coreThreadNum * 2,
                60000,
                TimeUnit.MILLISECONDS,
                connectQueue,
                new ThreadFactoryImpl("ConnectThread"),
                new RejectHandler("connect", 100000));
        this.pubExecutor = new ThreadPoolExecutor(coreThreadNum * 2,
                coreThreadNum * 2,
                60000,
                TimeUnit.MILLISECONDS,
                pubQueue,
                new ThreadFactoryImpl("PubThread"),
                new RejectHandler("pub", 100000));
        this.subExecutor = new ThreadPoolExecutor(coreThreadNum * 2,
                coreThreadNum * 2,
                60000,
                TimeUnit.MILLISECONDS,
                subQueue,
                new ThreadFactoryImpl("SubThread"),
                new RejectHandler("sub", 100000));
        this.pingExecutor = new ThreadPoolExecutor(coreThreadNum,
                coreThreadNum,
                60000,
                TimeUnit.MILLISECONDS,
                pingQueue,
                new ThreadFactoryImpl("PingThread"),
                new RejectHandler("heartbeat", 100000));
    }


    public void start() {

        MixAll.printProperties(log, brokerConfig);
        MixAll.printProperties(log, nettyConfig);

        // 1. start store
        this.sessionStore.start(brokerConfig);
        this.messageStore.start(brokerConfig);

        // 2. start cluster
        if (!this.akkaEnable) {
            this.eventConsumeHandler.start();
        }
        this.clusterEventHandler.start(brokerConfig);

        // 3. start message service
        if (this.innerMessageDispatcher != null) {
            this.innerMessageDispatcher.start();
        }
        if (this.reSendMessageService != null) {
            this.reSendMessageService.start();
        }

        {
            // 4. init and register mqtt protocol processor
            RequestProcessor connectProcessor = Optional.ofNullable(getProcessor(ConnectProcessor.class)).orElse(MixAll.pluginInit(brokerConfig.getConnectProcessorClass(),
                    new Class[]{this.getClass()}, this));
            RequestProcessor disconnectProcessor = Optional.ofNullable(getProcessor(DisconnectProcessor.class)).orElse(MixAll.pluginInit(brokerConfig.getDisconnectProcessorClass(),
                    new Class[]{this.getClass()}, this));
            RequestProcessor pingProcessor = Optional.ofNullable(getProcessor(PingProcessor.class)).orElse(new PingProcessor());
            RequestProcessor publishProcessor = Optional.ofNullable(getProcessor(PublishProcessor.class)).orElse(MixAll.pluginInit(brokerConfig.getPublishProcessorClass(),
                    new Class[]{this.getClass()}, this));
            RequestProcessor pubRelProcessor = Optional.ofNullable(getProcessor(PubRelProcessor.class)).orElse(new PubRelProcessor(this));
            RequestProcessor subscribeProcessor = Optional.ofNullable(getProcessor(SubscribeProcessor.class)).orElse(new SubscribeProcessor(this));
            RequestProcessor unSubscribeProcessor = Optional.ofNullable(getProcessor(UnSubscribeProcessor.class)).orElse(new UnSubscribeProcessor(subscriptionMatcher, sessionStore));
            RequestProcessor pubRecProcessor = Optional.ofNullable(getProcessor(PubRecProcessor.class)).orElse(new PubRecProcessor(this));
            RequestProcessor pubAckProcessor = Optional.ofNullable(getProcessor(PubAckProcessor.class)).orElse(new PubAckProcessor(this));
            RequestProcessor pubCompProcessor = Optional.ofNullable(getProcessor(PubCompProcessor.class)).orElse(new PubCompProcessor(this));

            this.remotingServer.registerProcessor(MqttMessageType.CONNECT, connectProcessor, connectExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.DISCONNECT, disconnectProcessor, connectExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.PINGREQ, pingProcessor, pingExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.PUBLISH, publishProcessor, pubExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.PUBACK, pubAckProcessor, pubExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.PUBREL, pubRelProcessor, pubExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.SUBSCRIBE, subscribeProcessor, subExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.UNSUBSCRIBE, unSubscribeProcessor, subExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.PUBREC, pubRecProcessor, subExecutor);
            this.remotingServer.registerProcessor(MqttMessageType.PUBCOMP, pubCompProcessor, subExecutor);
        }

        // 5. start auth
        if (this.authValid != null) {
            this.authValid.start();
        }

        // 6. start remoting
        if (this.remotingServer != null) {
            this.remotingServer.start();
        }
        LogUtil.info(log, "JMqtt Server start success and version = {}", brokerConfig.getVersion());

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        }));
    }

    public void shutdown() {
        if (this.remotingServer != null) {
            this.remotingServer.shutdown();
        }
        if (this.connectExecutor != null) {
            this.connectExecutor.shutdown();
        }
        if (this.pubExecutor != null) {
            this.pubExecutor.shutdown();
        }
        if (this.subExecutor != null) {
            this.subExecutor.shutdown();
        }
        if (this.pingExecutor != null) {
            this.pingExecutor.shutdown();
        }
        if (this.innerMessageDispatcher != null) {
            this.innerMessageDispatcher.shutdown();
        }
        if (this.reSendMessageService != null) {
            this.reSendMessageService.shutdown();
        }
        if (this.clusterEventHandler != null) {
            this.clusterEventHandler.shutdown();
        }
        if (this.eventConsumeHandler != null) {
            this.eventConsumeHandler.shutdown();
        }
        if (this.sessionStore != null) {
            this.sessionStore.shutdown();
        }
        if (this.messageStore != null) {
            this.messageStore.shutdown();
        }
        if (this.authValid != null) {
            this.authValid.shutdown();
        }

    }

    public void addRequestProcessor(RequestProcessor processor) {
        Class<? extends RequestProcessor> clazz = processor.getClass();
        if (Arrays.asList(clazz.getGenericInterfaces()).contains(RequestProcessor.class)) {
            this.requestProcessorMap.put(processor.getClass(), processor);
        } else {
            this.requestProcessorMap.put(clazz.getSuperclass(), processor);
        }
    }

    public RequestProcessor getProcessor(Class<? extends RequestProcessor> clazz) {
        return this.requestProcessorMap.get(clazz);
    }

}
