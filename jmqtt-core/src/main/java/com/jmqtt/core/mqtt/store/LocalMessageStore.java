package com.jmqtt.core.mqtt.store;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.store.persist.LocalDB;
import com.jmqtt.core.mqtt.store.persist.mapper.RetainMessageMapper;
import com.jmqtt.core.mqtt.store.persist.mapper.WillMessageMapper;
import com.jmqtt.core.mqtt.store.persist.po.RetainMessage;
import com.jmqtt.core.mqtt.store.persist.po.WillMessage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class LocalMessageStore implements MessageStore {
    /**
     * 遗嘱消息
     **/
    private Map<String, Message> willTable = new ConcurrentHashMap<>();
    /**
     * 保留消息
     */
    private Map<String, Message> retainTable = new ConcurrentHashMap<>();

    @Override
    public boolean storeWillMessage(String clientId, Message message) {
        return LocalDB.getInstance().operate(sqlSession -> {
            WillMessage willMessage = new WillMessage();
            willMessage.setClientId(clientId);
            willMessage.setContent(JSONUtil.toJsonStr(message));
            willMessage.setGmtCreate(message.getStoreTime());
            return sqlSession.getMapper(WillMessageMapper.class).insertOrUpdate(willMessage);
        });
        // willTable.put(clientId,message);
    }

    @Override
    public boolean clearWillMessage(String clientId) {
        return LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(WillMessageMapper.class).deleteById(clientId)
        ) > 0;
        // willTable.remove(clientId);
    }

    @Override
    public Message getWillMessage(String clientId) {
        WillMessage msg = LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(WillMessageMapper.class).selectById(clientId)
        );
        return Optional.ofNullable(msg).map(w -> JSONUtil.toBean(w.getContent(), Message.class)).orElse(null);
    }

    @Override
    public boolean storeRetainMessage(String topic, Message message) {
        // retain消息存放在H2中，缓解内存压力
        return LocalDB.getInstance().operate(sqlSession -> {
            RetainMessage retainMessage = new RetainMessage();
            retainMessage.setTopic(topic);
            retainMessage.setContent(JSONUtil.toJsonStr(message));
            return sqlSession.getMapper(RetainMessageMapper.class).insertOrUpdate(retainMessage);
        });
        // retainTable.put(topic, message);
    }

    @Override
    public boolean clearRetainMessage(String topic) {
        return LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(RetainMessageMapper.class).deleteById(topic)
        ) > 0;
        // retainTable.remove(topic);
    }

    @Override
    public Collection<Message> getAllRetainMsg() {
        List<RetainMessage> messageList = LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(RetainMessageMapper.class).selectList(Wrappers.lambdaQuery(RetainMessage.class))
        );
        return messageList.stream().map(retainMessage ->
                JSONUtil.toBean(retainMessage.getContent(), Message.class)).collect(Collectors.toList());
    }

    @Override
    public Collection<Message> getRetainMsg(String topic) {
        List<RetainMessage> messageList = LocalDB.getInstance().operate(sqlSession ->
                sqlSession.getMapper(RetainMessageMapper.class).selectList(Wrappers.lambdaQuery(RetainMessage.class)
                        .like(RetainMessage::getTopic, topic.replace("+", "%").replace("#", "%")))
        );
        return messageList.stream().map(messageDo -> JSONUtil.toBean(messageDo.getContent(), Message.class)).collect(Collectors.toList());
    }
}
