package org.jmqtt.broker.processor.protocol;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.JmqttConst;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.processor.HighPerformanceMessageHandler;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.event.Event;
import org.jmqtt.broker.processor.dispatcher.event.EventCode;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.store.MessageStore;

import java.util.concurrent.CompletableFuture;

/**
 * 通用消息分发处理器
 * TODO mqtt5实现
 */
public abstract class AbstractMessageProcessor extends HighPerformanceMessageHandler {

    private MessageStore messageStore;
    private ClusterEventHandler clusterEventHandler;
    private String currentIp;

    public AbstractMessageProcessor(BrokerController brokerController) {
        super(brokerController.getBrokerConfig().isHighPerformance(), brokerController.getSessionStore());
        this.messageStore = brokerController.getMessageStore();
        this.clusterEventHandler = brokerController.getClusterEventHandler();
        this.currentIp = brokerController.getCurrentIp();
    }

    protected void processMessage(Message message) {
        // 1. retain消息逻辑
        if (BrokerContext.centerStore()) {
            // 中央存储条件下，可直接处理retain，内存存储条件下，需要在消息
            // 分发的时候处理retain消息（DefaultDispatcherInnerMessage中）
            // 因为每个节点都要保存完整的retain消息表
            this.messageStore.retainHandle(message);
        }
        // 2. 向集群中分发消息：第一阶段
        sendMessage2Cluster(message);
    }

    /**
     * 向集群分发消息:第一阶段
     */
    private void sendMessage2Cluster(Message message) {
        Event event = new Event(EventCode.DISPATCHER_CLIENT_MESSAGE.getCode(),
                JSONObject.toJSONString(message), System.currentTimeMillis(),
                BrokerContext.getBrokerId());
        this.clusterEventHandler.sendEvent(event);
    }

}
