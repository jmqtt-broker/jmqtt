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

    public static void createInFlow(String clientId, Integer receiveMaximum) {
        destroyInFlow(clientId);
        RECEIVE_MAXIMUM_MAP_IN_FLOW.put(clientId, new Quota(clientId, receiveMaximum));
    }

    public static void createOutFlow(String clientId, Integer receiveMaximum) {
        destroyOutFlow(clientId);
        RECEIVE_MAXIMUM_MAP_OUT_FLOW.put(clientId, new OutboundQuota(clientId, receiveMaximum));
    }

    public static boolean acquireInFlow(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP_IN_FLOW.get(clientId);
        if (quota != null) {
            return quota.acquire();
        }
        return true;
    }

    public static boolean acquireOutFlow(String clientId, Subscription subscription, Message message) {
        OutboundQuota quota = RECEIVE_MAXIMUM_MAP_OUT_FLOW.get(clientId);
        if (quota != null) {
            return quota.acquire(QuotaMsg.of(subscription, message));
        }
        return true;
    }

    public static void releaseInFlow(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP_IN_FLOW.get(clientId);
        if (quota != null) {
            quota.release();
        }
    }

    public static void releaseOutFlow(String clientId) {
        OutboundQuota quota = RECEIVE_MAXIMUM_MAP_OUT_FLOW.get(clientId);
        if (quota != null) {
            quota.release();
        }
    }

    public static void destroyInFlow(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP_IN_FLOW.remove(clientId);
        if (quota != null) {
            quota.destroy();
        }
    }

    public static void destroyOutFlow(String clientId) {
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
                    try {
                        QuotaMsg message = waitQueue.poll(3000, TimeUnit.MILLISECONDS);
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
                    } catch (InterruptedException e) {
                        waiting.set(false);
                        Thread.currentThread().interrupt();
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
