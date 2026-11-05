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

    private final static Map<String, Quota> RECEIVE_MAXIMUM_MAP = new ConcurrentHashMap<>();

    public static void create(String clientId, Integer receiveMaximum) {
        RECEIVE_MAXIMUM_MAP.put(clientId, new Quota(clientId, receiveMaximum));
    }

    public static void release(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP.get(clientId);
        if (quota != null) {
            quota.release();
        }
    }

    public static boolean acquire(String clientId, Subscription subscription, Message message) {
        Quota quota = RECEIVE_MAXIMUM_MAP.get(clientId);
        if (quota != null) {
            return quota.acquire(QuotaMsg.of(subscription, message));
        }
        return true;
    }

    public static boolean acquire(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP.get(clientId);
        if (quota != null) {
            return quota.acquire();
        }
        return true;
    }

    public static void destroy(String clientId) {
        Quota quota = RECEIVE_MAXIMUM_MAP.remove(clientId);
        if (quota != null) {
            quota.destroy();
        }
    }

    @Getter
    static class Quota {

        private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        private final String clientId;

        private final int receiveMaximum;

        private final AtomicInteger quota;

        private final BlockingQueue<QuotaMsg> waitQueue;

        private final AtomicBoolean waiting;

        public Quota(String clientId, Integer receiveMaximum) {
            this.clientId = clientId;
            if (receiveMaximum <= 0) {
                throw new IllegalArgumentException("receiveMaximum must be greater than 0.");
            }
            this.receiveMaximum = receiveMaximum;
            this.quota = new AtomicInteger(receiveMaximum);
            waiting = new AtomicBoolean(false);
            waitQueue = new LinkedBlockingQueue<>(1024);
        }

        public boolean acquire(QuotaMsg message) {
            boolean acquired = acquire();
            if (!acquired) {
                if (waitQueue.offer(message)) {
                    if (waiting.compareAndSet(false, true)) {
                        consumer();
                    }
                } else {
                    log.warn("[ReceiveMaximumManager] -> the client {} is over the receive maximum queue, the message {} is rejected.", clientId, message);
                }
            }
            return acquired;
        }

        public boolean acquire() {
            boolean acquired;
            if (quota.intValue() == 0) {
                acquired = false;
            } else {
                acquired = quota.decrementAndGet() >= 0;
            }
            return acquired;
        }

        private void consumer() {
            executor.submit(() -> {
                while (quota.intValue() > 0 && !waitQueue.isEmpty()) {
                    QuotaMsg message = waitQueue.poll();
                    if (message != null) {
                        ClientSession session = ClientManager.getClient(clientId);
                        if (session != null && session.getCtx().channel().isWritable()) {
                            JmqttContext.getMessageDispatcher().send(message.getSubscription(), message.getMessage());
                        } else {
                            destroy();
                        }
                    } else {
                        waiting.set(false);
                    }
                }
            });
        }

        public void release() {
            if (quota.intValue() < receiveMaximum) {
                quota.incrementAndGet();
            }
        }

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
