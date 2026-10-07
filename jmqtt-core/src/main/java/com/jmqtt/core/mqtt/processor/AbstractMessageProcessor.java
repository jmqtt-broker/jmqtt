package com.jmqtt.core.mqtt.processor;

/**
 * 通用消息分发处理器
 */
public abstract class AbstractMessageProcessor {
/*
    private MessageStore messageStore;
    private ClusterEventHandler clusterEventHandler;
    private InnerMessageDispatcher dispatcherInnerMessage;

    public AbstractMessageProcessor(BrokerController brokerController) {
        super(brokerController.getBrokerConfig().isHighPerformance(), brokerController.getSessionStore());
        this.messageStore = brokerController.getMessageStore();
        this.clusterEventHandler = brokerController.getClusterEventHandler();
        this.dispatcherInnerMessage = brokerController.getInnerMessageDispatcher();
    }

    protected void processMessage(Message message) {
        messageStore.retainHandle(message);
        byte[] payload = message.getPayload();
        if (payload != null && payload.length > 0) {
            dispatcherInnerMessage.appendMessage(message);
        }
    }*/

    /**
     * 向集群分发消息:第一阶段
     */
    /*private void sendMessage2Cluster(Message message) {
        this.clusterEventHandler.sendEvent(ClusterHelper.getEvent(EventCode.DISPATCHER_CLIENT_MESSAGE, message));
    }*/

}
