package org.jmqtt.broker.store.highperformance;

import org.jmqtt.broker.common.model.Message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 出栈过程消息处理
 * 高性能配置下：利用内存进行缓存，不持久化
 */
public class OutflowMessageHandler {

    private static Map<String /* clientId */, Map<Integer /* msgId */, Message>> outflowMsgCache = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    public static boolean cacheOutflowMsg(String clientId, Message message) {
        Map<Integer,Message> msgCache = outflowMsgCache.get(clientId);
        if (msgCache == null) {
            synchronized (lock) {
                msgCache = outflowMsgCache.get(clientId);
                if (msgCache == null) {
                    msgCache = new ConcurrentHashMap<>();
                    outflowMsgCache.put(clientId,msgCache);
                }
            }
        }
        msgCache.put(message.getMsgId(),message);
        return true;
    }

    public static Collection<Message> getAllOutflowMsg(String clientId) {
        Map<Integer,Message> msgCache = outflowMsgCache.get(clientId);
        if (msgCache == null || msgCache.size() == 0) {
            return Collections.EMPTY_LIST;
        }
        Collection<Message> values = msgCache.values();
        Collection<Message> queue = new PriorityQueue<Message>(new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                if (o1.getStoreTime() < o2.getStoreTime()) {
                    return -1;
                } else if (o1.getStoreTime() == o2.getStoreTime()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        for (Message value : values) {
            queue.add(value);
        }
        return queue;
    }

    public static Message releaseOutflowMsg(String clientId, Integer msgId) {
        Map<Integer,Message> msgCache = outflowMsgCache.get(clientId);
        if (msgId == null && msgCache != null) {
            msgCache.clear();
            return null;
        } else {
            if (msgCache == null) {
                return null;
            }
            return msgCache.remove(msgId);
        }
    }

}
