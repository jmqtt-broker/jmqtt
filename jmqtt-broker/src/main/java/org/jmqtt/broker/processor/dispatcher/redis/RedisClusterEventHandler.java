package org.jmqtt.broker.processor.dispatcher.redis;

import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.EventConsumeHandler;
import org.jmqtt.common.event.Event;
import org.jmqtt.broker.store.redis.support.RedisKeySupport;
import org.jmqtt.broker.store.redis.support.RedisOperator;
import org.jmqtt.broker.store.redis.support.RedisUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public class RedisClusterEventHandler implements ClusterEventHandler {

    private static final Logger log = JmqttLogger.storeLog;

    private static final String INSTANCE_CHANNEL_ID = RedisKeySupport.PREFIX + "_CLUSTER_CHANNEL_" + UUID.randomUUID();
    private static final String INSTANCE_CHANNEL_PATTERN = RedisKeySupport.PREFIX + "_CLUSTER_CHANNEL_*";
    private EventConsumeHandler eventConsumeHandler;
    private RedisOperator redisOperator;

    @Override
    public void start(BrokerConfig brokerConfig) {
        this.redisOperator = RedisUtils.getInstance().createSupport(brokerConfig);
        new Thread(() -> {
            this.redisOperator.subscribe(INSTANCE_CHANNEL_PATTERN, (channel, message) -> {
                try {
                    Event event = JSONObject.parseObject(message, Event.class);
                    JSONObject body = (JSONObject) event.getBody();
                    String className = body.getString("type");
                    Object data = body.get("data");
                    if (className != null && data != null) {
                        Object content;
                        if (data instanceof String) {
                            content = data;
                        } else {
                            content = ((JSONObject) data).toJavaObject(Class.forName(className));
                        }
                        event.setBody(content);
                        eventConsumeHandler.consumeEvent(event);
                    } else {
                        log.warn("event exception.");
                    }
                } catch (Exception e) {
                    LogUtil.error(log,"Receive redis event error,e:{}",e);
                }
            });
        }).start();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean sendEvent(Event event) {
        Object body = event.getBody();
        JSONObject desc = new JSONObject();
        desc.put("type", body.getClass().getName());
        desc.put("data", body);
        event.setBody(desc);
        return redisOperator.publish(INSTANCE_CHANNEL_ID, JSONObject.toJSONString(event));
    }

    @Override
    public void setEventConsumeHandler(EventConsumeHandler eventConsumeHandler) {
        this.eventConsumeHandler = eventConsumeHandler;
    }

    @Override
    public List<Event> pollEvent(int maxPollNum) {
        return null;
    }
}
