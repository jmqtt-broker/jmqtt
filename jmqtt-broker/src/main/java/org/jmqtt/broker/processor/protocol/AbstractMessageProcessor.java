package org.jmqtt.broker.processor.protocol;

import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.HighPerformanceMessageHandler;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;
import org.jmqtt.broker.store.MessageStore;

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
        this.messageStore.retainHandle(message);
        // 2. 向集群中分发消息：第一阶段
        byte[] payload = message.getPayload();
        if (payload != null && payload.length > 0) {
            sendMessage2Cluster(message);
        }
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
