package org.jmqtt.broker.store.mem;

import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.remoting.util.IdWorker;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.local.LocalDB;
import org.jmqtt.broker.store.local.mapper.LocalRetainMessageMapper;
import org.jmqtt.broker.store.local.mapper.LocalWillMessageMapper;
import org.jmqtt.broker.store.rdb.daoobject.RetainMessageDO;
import org.jmqtt.broker.store.rdb.daoobject.WillMessageDO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class MemMessageStore extends AbstractMemStore implements MessageStore {
    /**
     * 遗嘱消息
     **/
    private Map<String /*clientId*/, Message> willTable = new ConcurrentHashMap<>();
    /**
     * 保持消息
     */
    private Map<String/*Topic*/, Message> retainTable = new ConcurrentHashMap<>();

    @Override
    public void start(BrokerConfig brokerConfig) {
        super.start(brokerConfig);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public boolean storeWillMessage(String clientId, Message message) {
        LocalDB.getInstance().operate(sqlSession -> {
            WillMessageDO willMessageDO = new WillMessageDO();
            willMessageDO.setId(IdWorker.getId());
            willMessageDO.setClientId(clientId);
            willMessageDO.setContent(JSONObject.toJSONString(message));
            willMessageDO.setGmtCreate(message.getStoreTime());
            return sqlSession.getMapper(LocalWillMessageMapper.class).storeWillMessage(willMessageDO);
        });
        // willTable.put(clientId,message);
        return true;
    }

    @Override
    public boolean clearWillMessage(String clientId) {
        LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalWillMessageMapper.class).delWillMessage(clientId)
        );
        // willTable.remove(clientId);
        return true;
    }

    @Override
    public Message getWillMessage(String clientId) {
        WillMessageDO msg = (WillMessageDO) LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalWillMessageMapper.class).getWillMessage(clientId)
        );
        if (msg != null) {
            return JSONObject.parseObject(msg.getContent(), Message.class);
        }
        return null;
    }

    @Override
    public boolean storeRetainMessage(String topic, Message message) {
        // retain消息存放在H2中，缓解内存压力
        LocalDB.getInstance().operate(sqlSession -> {
            RetainMessageDO retainMessageDO = new RetainMessageDO();
            retainMessageDO.setId(IdWorker.getId());
            retainMessageDO.setTopic(topic);
            retainMessageDO.setContent(JSONObject.toJSONString(message));
            return sqlSession.getMapper(LocalRetainMessageMapper.class).storeRetainMessage(retainMessageDO);
        });
        // retainTable.put(topic, message);
        return true;
    }

    @Override
    public boolean clearRetainMessage(String topic) {
        LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalRetainMessageMapper.class).delRetainMessage(topic)
        );
        // retainTable.remove(topic);
        return true;
    }

    @Override
    public Collection<Message> getAllRetainMsg() {
        List<RetainMessageDO> messageList = (List<RetainMessageDO>) LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalRetainMessageMapper.class).getAllRetainMessage()
        );
        List<Message> mqttMessages = new ArrayList<>(messageList.size());
        for (RetainMessageDO retainMessageDO : messageList) {
            Message message = JSONObject.parseObject(retainMessageDO.getContent(), Message.class);
            mqttMessages.add(message);
        }
        // return retainTable.values();
        return mqttMessages;
    }

    @Override
    public Collection<Message> getRetainMsg(String topic) {
        List<RetainMessageDO> messageList = (List<RetainMessageDO>) LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(LocalRetainMessageMapper.class).getRetainMessage(topic.replace("+", "%").replace("#", "%"))
        );
        return messageList.stream().map(messageDo -> JSONObject.parseObject(messageDo.getContent(), Message.class)).collect(Collectors.toList());
    }
}
