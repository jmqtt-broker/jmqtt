package org.jmqtt.broker.processor.protocol;

import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.HighPerformanceMessageHandler;
import org.jmqtt.broker.processor.dispatcher.DefaultDispatcherInnerMessage;
import org.jmqtt.broker.processor.dispatcher.akka.ClusterHelper;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.common.event.EventCode;

import java.util.concurrent.CompletableFuture;

/**
 * 通用消息分发处理器
 */
public abstract class AbstractMessageProcessor extends HighPerformanceMessageHandler {

    private MessageStore messageStore;
    private ClusterEventHandler clusterEventHandler;
    private DefaultDispatcherInnerMessage dispatcherInnerMessage;

    public AbstractMessageProcessor(BrokerController brokerController) {
        super(brokerController.getBrokerConfig().isHighPerformance(), brokerController.getSessionStore());
        this.messageStore = brokerController.getMessageStore();
        this.clusterEventHandler = brokerController.getClusterEventHandler();
    }

    protected void processMessage(Message message) {
        messageStore.retainHandle(message);
        byte[] payload = message.getPayload();
        if (payload != null && payload.length > 0) {
            dispatcherInnerMessage.appendMessage(message);
        }
    }

    /**
     * 向集群分发消息:第一阶段
     */
    /*private void sendMessage2Cluster(Message message) {
        this.clusterEventHandler.sendEvent(ClusterHelper.getEvent(EventCode.DISPATCHER_CLIENT_MESSAGE, message));
    }*/

}
