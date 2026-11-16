package com.jmqtt.core.mqtt.helper;

import com.jmqtt.core.mqtt.JmqttContext;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReceiveMaximumManager {

    private final static Map<String, OutboundQuota> RECEIVE_MAXIMUM_MAP_OUT_FLOW = new ConcurrentHashMap<>(64);

    private final static Map<String, Quota> RECEIVE_MAXIMUM_MAP_IN_FLOW = new ConcurrentHashMap<>(64);

    public static void createInbound(String clientId, Integer receiveMaximum) {
        destroyInbound(clientId);
        RECEIVE_MAXIMUM_MAP_IN_FLOW.put(clientId, new Quota(clientId, receiveMaximum));
    }

    public static void createOutbound(String clientId, Integer receiveMaximum) {
        destroyOutbound(clientId);
        RECEIVE_MAXIMUM_MAP_OUT_FLOW.put(clientId, new OutboundQuota(clientId, receiveMaximum));
    }

    public static boolean acquireInbound(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP_IN_FLOW.get(clientId);
        if (quota != null) {
            return quota.acquire();
        }
        return true;
    }

    public static boolean acquireOutbound(String clientId, Subscription subscription, Message message) {
        OutboundQuota quota = RECEIVE_MAXIMUM_MAP_OUT_FLOW.get(clientId);
        if (quota != null) {
            return quota.acquire(QuotaMsg.of(subscription, message));
        }
        return true;
    }

    public static void releaseInbound(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP_IN_FLOW.get(clientId);
        if (quota != null) {
            quota.release();
        }
    }

    public static void releaseOutbound(String clientId) {
        OutboundQuota quota = RECEIVE_MAXIMUM_MAP_OUT_FLOW.get(clientId);
        if (quota != null) {
            quota.release();
        }
    }

    public static void destroyInbound(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP_IN_FLOW.remove(clientId);
        if (quota != null) {
            quota.destroy();
        }
    }

    public static void destroyOutbound(String clientId) {
        OutboundQuota quota = RECEIVE_MAXIMUM_MAP_OUT_FLOW.remove(clientId);
        if (quota != null) {
            quota.destroy();
        }
    }

    @Getter
    static class Quota {

        protected final String clientId;

        protected final int receiveMaximum;

        protected final AtomicInteger quota;

        public Quota(String clientId, Integer receiveMaximum) {
            this.clientId = clientId;
            if (receiveMaximum <= 0) {
                throw new IllegalArgumentException("receiveMaximum must be greater than 0.");
            }
            this.receiveMaximum = receiveMaximum;
            this.quota = new AtomicInteger(receiveMaximum);
        }

        public boolean acquire() {
            while (true) {
                int current = quota.get();
                if (current <= 0) {
                    return false;
                }
                if (quota.compareAndSet(current, current - 1)) {
                    return true;
                }
            }
        }

        public void release() {
            while (true) {
                int current = quota.get();
                if (current >= receiveMaximum) {
                    return;
                }
                if (quota.compareAndSet(current, current + 1)) {
                    return;
                }
            }
        }

        public void destroy() {
        }

    }

    static class OutboundQuota extends Quota {

        private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        private final BlockingQueue<QuotaMsg> waitQueue;

        private final AtomicBoolean waiting;

        public OutboundQuota(String clientId, Integer receiveMaximum) {
            super(clientId, receiveMaximum);
            waiting = new AtomicBoolean(false);
            waitQueue = new LinkedBlockingQueue<>(1024);
        }

        public boolean acquire(QuotaMsg quotaMsg) {
            boolean acquired = acquire();
            if (!acquired) {
                if (waitQueue.offer(quotaMsg)) {
                    if (waiting.compareAndSet(false, true)) {
                        consumer();
                    }
                } else {
                    log.warn("[ReceiveMaximumManager] -> the client {} is over the receive maximum queue, the message {} is rejected.", clientId, quotaMsg);
                }
            }
            return acquired;
        }

        private void consumer() {
            executor.submit(() -> {
                while (quota.intValue() > 0 && !waitQueue.isEmpty()) {
                    QuotaMsg message = waitQueue.poll();
                    if (message != null) {
                        ClientSession session = ClientManager.getClient(clientId);
                        if (session != null && session.getCtx().channel().isActive()) {
                            JmqttContext.getMessageDispatcher().send(message.getSubscription(), message.getMessage());
                        } else {
                            destroy();
                        }
                    } else {
                        waiting.set(false);
                    }
                }
                waiting.set(false);
            });
        }

        @Override
        public void release() {
            super.release();
            if (waiting.compareAndSet(false, true) && !waitQueue.isEmpty()) {
                consumer();
            }
        }

        @Override
        public void destroy() {
            waiting.set(false);
            waitQueue.clear();
        }

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class QuotaMsg {

        private Subscription subscription;

        private Message message;

        public static QuotaMsg of(Subscription subscription, Message message) {
            return new QuotaMsg(subscription, message);
        }

    }

}
