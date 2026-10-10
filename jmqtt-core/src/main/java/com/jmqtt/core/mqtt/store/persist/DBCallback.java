package com.jmqtt.core.mqtt.store.persist;

import org.apache.ibatis.session.SqlSession;

public interface DBCallback<R> {
    R operate(SqlSession sqlSession);
}
