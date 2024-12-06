package org.jmqtt.broker.store.highperformance;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 出栈过程消息处理-qos2第二阶段
 * 高性能配置下：利用内存进行缓存，不持久化
 */
public class OutflowSecMessageHandler {

    private static Map<String, Queue<Integer>> outflowSecCache = new HashMap<>();

    private static final Object lock = new Object();

    public static boolean cacheOutflowSecMsgId(String clientId, int msgId) {
        Queue<Integer> msgCache = outflowSecCache.get(clientId);
        if (msgCache == null) {
            synchronized (lock) {
                msgCache = outflowSecCache.get(clientId);
                if (msgCache == null) {
                    msgCache = new LinkedBlockingQueue<>();
                    outflowSecCache.put(clientId,msgCache);
                }
            }
        }
        msgCache.offer(msgId);
        return true;
    }

    public static boolean releaseOutflowSecMsgId(String clientId, int msgId) {
        Queue<Integer> msgCache = outflowSecCache.get(clientId);
        if (msgCache == null) {
            return false;
        }
        return msgCache.remove(msgId);
    }

    public static Collection<Integer> getAllOutflowSecMsgId(String clientId) {
        Queue<Integer> msgCache = outflowSecCache.get(clientId);
        if (msgCache == null) {
            return Collections.EMPTY_LIST;
        }
        // just return snapshot
        return new ArrayList<>(msgCache);
    }
}
