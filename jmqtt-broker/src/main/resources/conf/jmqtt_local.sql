SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for jmqtt_offline_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jmqtt_offline_message`
(
    `id`         bigint(20)  NOT NULL,
    `client_id`  varchar(64) NOT NULL COMMENT '客户端id',
    `content`    text        NOT NULL COMMENT '消息体',
    `gmt_create` bigint(20)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `client_id_offline` (`client_id`),
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='离线消息表';

-- ----------------------------
-- Table structure for jmqtt_retain_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jmqtt_retain_message`
(
    `id`      bigint(20)   NOT NULL COMMENT '主键',
    `topic`   varchar(128) NOT NULL COMMENT '所属topic',
    `content` text         NOT NULL COMMENT '消息体',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uqe_topic` (`topic`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='保留消息表';

-- ----------------------------
-- Table structure for jmqtt_session
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jmqtt_session`
(
    `id`           bigint(20)   NOT NULL COMMENT '主键',
    `broker_id`    varchar(100) NOT NULL COMMENT 'brokerId',
    `client_id`    varchar(64)  NOT NULL COMMENT '客户端id',
    `state`        varchar(12)  NOT NULL COMMENT '状态：ONLINE,OFFLINE两种',
    `offline_time` bigint(20)   DEFAULT NULL COMMENT 'OFFLINE状态时对应的离线时间戳（只有cleanStart为0时候离线才有该数据）',
    `property`     varchar(500) DEFAULT NULL COMMENT 'mqtt5 client连接属性',
    `version`      int          DEFAULT NULL COMMENT 'mqtt客户端版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `s_client_id` (`client_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='客户端会话状态';

-- ----------------------------
-- Table structure for jmqtt_subscription
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jmqtt_subscription`
(
    `id`        bigint(20)   NOT NULL COMMENT '主键',
    `client_id` varchar(64)  NOT NULL COMMENT '客户端id',
    `topic`     varchar(128) NOT NULL COMMENT '订阅的topic',
    `qos`       tinyint(4)   NOT NULL COMMENT '对应的qos',
    `opt`       varchar(100) DEFAULT NULL COMMENT '订阅选项',
    PRIMARY KEY (`id`),
    UNIQUE KEY `sub_client_id_topic` (`client_id`, `topic`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='客户端订阅关系';

-- ----------------------------
-- Table structure for jmqtt_will_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS `jmqtt_will_message`
(
    `id`         bigint(20)  NOT NULL,
    `client_id`  varchar(64) NOT NULL COMMENT '客户端id',
    `content`    text        NOT NULL COMMENT '消息体',
    `gmt_create` bigint(20)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `wm_client_id` (`client_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='遗嘱消息表';

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS `jmqtt_broker`
(
    `id`           bigint(20)       NOT NULL COMMENT '主键',
    `broker_id`    varchar(100) NOT NULL COMMENT 'broker唯一标识',
    `ip`           varchar(20)  NOT NULL COMMENT 'ip地址',
    `tcp_port`     int          NOT NULL COMMENT 'tcp端口',
    `tcp_port_ssl` int          NOT NULL COMMENT 'tcp ssl端口',
    `ws_port`      int          NOT NULL COMMENT 'websocket端口',
    `ws_port_ssl`  int          NOT NULL COMMENT 'websocket ssl端口',
    `status`       tinyint      NULL COMMENT '是否在线',
    `online_at`    bigint(20)     NULL COMMENT '上线时间',
    `offline_at`   bigint(20)     NULL COMMENT '离线时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `IDX_BROKER_ID` (`broker_id`) USING BTREE
);

CREATE TABLE IF NOT EXISTS `broker_config`
(
    `id`        bigint(20)       NOT NULL COMMENT '主键',
    `broker_id` varchar(100) NOT NULL COMMENT 'broker唯一标识',
    `config`    json         NOT NULL COMMENT '配置详情',
    PRIMARY KEY (`id`)
);
