-- ----------------------------
-- Table structure for jmqtt_offline_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS jmqtt_offline_message
(
    id         BIGINT       NOT NULL,
    client_id  VARCHAR(64)  NOT NULL,
    content    TEXT         NOT NULL,
    gmt_create BIGINT       NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE jmqtt_offline_message IS '离线消息表';
COMMENT ON COLUMN jmqtt_offline_message.client_id IS '客户端id';
COMMENT ON COLUMN jmqtt_offline_message.content IS '消息体';
COMMENT ON COLUMN jmqtt_offline_message.gmt_create IS '创建时间';

CREATE INDEX IF NOT EXISTS client_id_offline ON jmqtt_offline_message (client_id);
CREATE INDEX IF NOT EXISTS idx_gmt_create ON jmqtt_offline_message (gmt_create);


-- ----------------------------
-- Table structure for jmqtt_retain_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS jmqtt_retain_message
(
    topic   VARCHAR(200) NOT NULL,
    content TEXT         NOT NULL,
    PRIMARY KEY (topic)
);

COMMENT ON TABLE jmqtt_retain_message IS '保留消息表';
COMMENT ON COLUMN jmqtt_retain_message.topic IS '所属topic，主键';
COMMENT ON COLUMN jmqtt_retain_message.content IS '消息体';


-- ----------------------------
-- Table structure for jmqtt_session
-- ----------------------------
CREATE TABLE IF NOT EXISTS jmqtt_session
(
    client_id    VARCHAR(64)  NOT NULL,
    broker_id    VARCHAR(100) NOT NULL,
    state        VARCHAR(20)  NOT NULL,
    online_time  BIGINT       DEFAULT NULL,
    offline_time BIGINT       DEFAULT NULL,
    property     VARCHAR(500) DEFAULT NULL,
    version      INTEGER      DEFAULT NULL,
    address      VARCHAR(50)  DEFAULT NULL,
    clean_start  SMALLINT     DEFAULT NULL,
    keepalive    INTEGER      DEFAULT NULL,
    PRIMARY KEY (client_id)
);

COMMENT ON TABLE jmqtt_session IS '客户端会话状态';
COMMENT ON COLUMN jmqtt_session.client_id IS '客户端id';
COMMENT ON COLUMN jmqtt_session.broker_id IS 'brokerId';
COMMENT ON COLUMN jmqtt_session.state IS '状态：ONLINE,OFFLINE两种';
COMMENT ON COLUMN jmqtt_session.online_time IS '上线时间';
COMMENT ON COLUMN jmqtt_session.offline_time IS 'OFFLINE状态时对应的离线时间戳（只有cleanStart为0时候离线才有该数据）';
COMMENT ON COLUMN jmqtt_session.property IS 'mqtt5 client连接属性';
COMMENT ON COLUMN jmqtt_session.version IS 'mqtt客户端版本';
COMMENT ON COLUMN jmqtt_session.address IS '客户端地址';
COMMENT ON COLUMN jmqtt_session.clean_start IS '断开后是否清空会话';
COMMENT ON COLUMN jmqtt_session.keepalive IS '心跳周期';


-- ----------------------------
-- Table structure for jmqtt_subscription
-- ----------------------------
CREATE TABLE IF NOT EXISTS jmqtt_subscription
(
    id        BIGINT       NOT NULL,
    broker_id VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    topic     VARCHAR(128) NOT NULL,
    qos       SMALLINT     NOT NULL,
    sub_res   BOOLEAN,
    opt       VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT sub_client_id_topic UNIQUE (client_id, topic)
);

COMMENT ON TABLE jmqtt_subscription IS '客户端订阅关系';
COMMENT ON COLUMN jmqtt_subscription.id IS '主键';
COMMENT ON COLUMN jmqtt_subscription.broker_id IS 'brokerId';
COMMENT ON COLUMN jmqtt_subscription.client_id IS '客户端id';
COMMENT ON COLUMN jmqtt_subscription.topic IS '订阅的topic';
COMMENT ON COLUMN jmqtt_subscription.qos IS '对应的qos';
COMMENT ON COLUMN jmqtt_subscription.sub_res IS '订阅结果, true新订阅，false订阅已存在，重复订阅';
COMMENT ON COLUMN jmqtt_subscription.opt IS '订阅选项';


-- ----------------------------
-- Table structure for jmqtt_will_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS jmqtt_will_message
(
    client_id  VARCHAR(64) NOT NULL,
    content    TEXT        NOT NULL,
    gmt_create BIGINT      NOT NULL,
    PRIMARY KEY (client_id)
);

COMMENT ON TABLE jmqtt_will_message IS '遗嘱消息表';
COMMENT ON COLUMN jmqtt_will_message.client_id IS '客户端id';
COMMENT ON COLUMN jmqtt_will_message.content IS '消息体';
COMMENT ON COLUMN jmqtt_will_message.gmt_create IS '创建时间';


-- ----------------------------
-- Table structure for jmqtt_broker
-- ----------------------------
CREATE TABLE IF NOT EXISTS jmqtt_broker
(
    broker_id    VARCHAR(100) NOT NULL,
    ip           VARCHAR(20)  NOT NULL,
    tcp_port     INTEGER      NOT NULL,
    tcp_port_ssl INTEGER      NOT NULL,
    ws_port      INTEGER      NOT NULL,
    ws_port_ssl  INTEGER      NOT NULL,
    status       SMALLINT     NULL,
    online_at    BIGINT       NULL,
    offline_at   BIGINT       NULL,
    PRIMARY KEY (broker_id)
);

COMMENT ON TABLE jmqtt_broker IS NULL;
COMMENT ON COLUMN jmqtt_broker.broker_id IS 'broker唯一标识';
COMMENT ON COLUMN jmqtt_broker.ip IS 'ip地址';
COMMENT ON COLUMN jmqtt_broker.tcp_port IS 'tcp端口';
COMMENT ON COLUMN jmqtt_broker.tcp_port_ssl IS 'tcp ssl端口';
COMMENT ON COLUMN jmqtt_broker.ws_port IS 'websocket端口';
COMMENT ON COLUMN jmqtt_broker.ws_port_ssl IS 'websocket ssl端口';
COMMENT ON COLUMN jmqtt_broker.status IS '是否在线';
COMMENT ON COLUMN jmqtt_broker.online_at IS '上线时间';
COMMENT ON COLUMN jmqtt_broker.offline_at IS '离线时间';


-- ----------------------------
-- Table structure for broker_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS broker_config
(
    broker_id VARCHAR(100) NOT NULL,
    config    JSONB        NOT NULL,
    PRIMARY KEY (broker_id)
);

COMMENT ON COLUMN broker_config.broker_id IS 'broker唯一标识';
COMMENT ON COLUMN broker_config.config IS '配置详情';


-- ----------------------------
-- Table structure for schedule_task
-- ----------------------------
CREATE TABLE IF NOT EXISTS schedule_task
(
    timer_id  VARCHAR(64) NOT NULL,
    type      VARCHAR(20) NOT NULL,
    expire_at BIGINT      NOT NULL,
    expire    BIGINT      NOT NULL,
    data      TEXT        DEFAULT NULL,
    cycle     SMALLINT    NOT NULL,
    exec      SMALLINT    NOT NULL,
    PRIMARY KEY (timer_id)
);

COMMENT ON COLUMN schedule_task.timer_id IS 'timerId，与任务类型组合唯一';
COMMENT ON COLUMN schedule_task.type IS '任务类型，如session过期任务';
COMMENT ON COLUMN schedule_task.expire_at IS '过期时间点（时间戳）';
COMMENT ON COLUMN schedule_task.expire IS '有效时间（秒）';
COMMENT ON COLUMN schedule_task.data IS '关联数据';
COMMENT ON COLUMN schedule_task.cycle IS '是否为周期任务，1循环任务，0一次性延时任务';
COMMENT ON COLUMN schedule_task.exec IS 'reload后，如果过期了，是否需要执行';
