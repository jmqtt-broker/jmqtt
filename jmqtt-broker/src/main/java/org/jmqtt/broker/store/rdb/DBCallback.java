package org.jmqtt.broker.store.rdb;

import org.apache.ibatis.session.SqlSession;

public interface DBCallback<R> {
    R operate(SqlSession sqlSession);
}
