package org.jmqtt.broker.store.cluster;

import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.ScheduleManager;
import org.jmqtt.broker.common.helper.TimerBO;
import org.jmqtt.broker.common.helper.TimerManager;
import org.jmqtt.broker.common.model.ClusterNodeInfo;
import org.jmqtt.broker.processor.dispatcher.event.Event;
import org.jmqtt.broker.processor.dispatcher.event.EventCode;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClusterManager {

    private static final Map<String, ClusterNodeInfo> NODE_MAP = new ConcurrentHashMap<>();

    private static final AtomicBoolean READY = new AtomicBoolean(false);

    private static final Integer BROKER_EXPIRE = 20;

    public static void start() {
        if (READY.compareAndSet(false, true)) {
            String brokerId = BrokerContext.getBrokerId();
            // 定时广播本节点的keepalive消息
            ScheduleManager.addScheduled(new TimerBO(brokerId, TimerManager.TimerType.BROKER_KEEPALIVE,
                    brokerId, BROKER_EXPIRE, timerBO -> {
                Event event = new Event(EventCode.CLUSTER_NODE_KEEPALIVE.getCode(),
                        brokerId, System.currentTimeMillis(), brokerId);
                BrokerContext.sendEvent(event);
            }));
            // 定时检查所有节点的keepalive状态，如果超过keepalive 1.5倍时间未收到keepalive广播，认为该节点离线
            int timeout = (int) (BROKER_EXPIRE * 1.5);
            ScheduleManager.addScheduled(new TimerBO(brokerId, TimerManager.TimerType.BROKER_KEEPALIVE_DETECT,
                    brokerId, timeout, timerBO -> {
                NODE_MAP.forEach((k, v) -> {
                    long curr = System.currentTimeMillis();
                    if ((v.getLastKeepaliveTime() + BROKER_EXPIRE * 1500) < curr) {
                        v.setOffLineAt(curr);
                        v.setStatus(false);
                        log.info("cluster node offline: {}", k);
                    }
                });
            }));
        }
    }

    public static ClusterNodeInfo getCurrentNode() {
        return Optional.ofNullable(NODE_MAP.get(BrokerContext.getBrokerId())).orElse(new ClusterNodeInfo(BrokerContext.getBrokerId(), true));
    }

    public static void saveNode(ClusterNodeInfo node) {
        String nodeId = node.getNodeId();
        Boolean status = node.getStatus();
        ClusterNodeInfo existNode = NODE_MAP.get(nodeId);
        long curr = System.currentTimeMillis();
        if (existNode != null) {
            if (status) {
                existNode.setOnlineAt(curr);
                existNode.setLastKeepaliveTime(curr);
            } else {
                existNode.setOffLineAt(curr);
            }
        } else {
            if (status) {
                if (node.getOnlineAt() == null) {
                    node.setOnlineAt(curr);
                }
                if (node.getLastKeepaliveTime() == null) {
                    node.setLastKeepaliveTime(curr);
                }
            } else {
                if (node.getOffLineAt() == null) {
                    node.setOffLineAt(curr);
                }
            }
            NODE_MAP.put(nodeId, node);
        }
    }

    public static void keepalive(String nodeId) {
        ClusterNodeInfo existNode = NODE_MAP.get(nodeId);
        if (existNode != null) {
            if (!existNode.getStatus()) {
                existNode.setStatus(true);
            }
            existNode.setLastKeepaliveTime(System.currentTimeMillis());
        } else if (BrokerContext.getBrokerId().equals(nodeId)) {
            saveNode(new ClusterNodeInfo(BrokerContext.getBrokerId(), true));
        } else {
            log.warn("cluster node not exist: {}", nodeId);
        }
    }

}
