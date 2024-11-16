-- ----------------------------
-- Table structure for jmqtt_event
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_event"
(
    "id" int8 NOT NULL,
    "content"  text COLLATE "pg_catalog"."default"        NOT NULL,
    "gmt_create" int8 NOT NULL,
    "jmqtt_ip" varchar(24) COLLATE "pg_catalog"."default" NOT NULL,
    "event_code" int4 NOT NULL,
    CONSTRAINT "jmqtt_event_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_event"."id" IS '主键：也是集群节点批量拉消息的offset';
COMMENT ON COLUMN "jmqtt_event"."content" IS '消息体';
COMMENT ON COLUMN "jmqtt_event"."gmt_create" IS '创建时间';
COMMENT ON COLUMN "jmqtt_event"."jmqtt_ip" IS 'jmqtt服务器ip，发送该消息到集群中的broker ip';
COMMENT ON COLUMN "jmqtt_event"."event_code" IS '事件码';
COMMENT ON TABLE "jmqtt_event" IS 'jmqtt 集群事件转发表：由发送端将消息发送到该表中，其他节点批量拉取该表中的事件进行处理';

-- ----------------------------
-- Table structure for jmqtt_inflow_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_inflow_message"
(
    "id" int8 NOT NULL,
    "client_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "msg_id" int4 NOT NULL,
    "content"   text COLLATE "pg_catalog"."default"        NOT NULL,
    "gmt_create" int8 NOT NULL,
    CONSTRAINT "jmqtt_inflow_message_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_inflow_message"."id" IS '主键id';
COMMENT ON COLUMN "jmqtt_inflow_message"."client_id" IS '设备id';
COMMENT ON COLUMN "jmqtt_inflow_message"."msg_id" IS '消息id';
COMMENT ON COLUMN "jmqtt_inflow_message"."content" IS '消息体内容';
COMMENT ON COLUMN "jmqtt_inflow_message"."gmt_create" IS '消息保存时间（对应消息接收时间）';
COMMENT ON TABLE "jmqtt_inflow_message" IS '入栈消息表';

-- ----------------------------
-- Table structure for jmqtt_offline_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_offline_message"
(
    "id" int8 NOT NULL,
    "client_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "content"   text COLLATE "pg_catalog"."default"        NOT NULL,
    "gmt_create" int8 NOT NULL,
    CONSTRAINT "jmqtt_offline_message_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_offline_message"."client_id" IS '客户端id';
COMMENT ON COLUMN "jmqtt_offline_message"."content" IS '消息体';
COMMENT ON COLUMN "jmqtt_offline_message"."gmt_create" IS '创建时间';
COMMENT ON TABLE "jmqtt_offline_message" IS '离线消息表';

-- ----------------------------
-- Table structure for jmqtt_outflow_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_outflow_message"
(
    "id" int8 NOT NULL,
    "client_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "msg_id" int4 NOT NULL,
    "content"   text COLLATE "pg_catalog"."default"        NOT NULL,
    "gmt_create" int8 NOT NULL,
    CONSTRAINT "jmqtt_outflow_message_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_outflow_message"."id" IS '主键';
COMMENT ON COLUMN "jmqtt_outflow_message"."client_id" IS '目标客户端id';
COMMENT ON COLUMN "jmqtt_outflow_message"."msg_id" IS '消息id';
COMMENT ON COLUMN "jmqtt_outflow_message"."content" IS '消息内容';
COMMENT ON COLUMN "jmqtt_outflow_message"."gmt_create" IS '消息缓存时间';
COMMENT ON TABLE "jmqtt_outflow_message" IS '出栈消息表';

-- ----------------------------
-- Table structure for jmqtt_outflow_sec_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_outflow_sec_message"
(
    "id" int8 NOT NULL,
    "client_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "msg_id" int4 NOT NULL,
    "gmt_create" int8 NOT NULL,
    CONSTRAINT "jmqtt_outflow_sec_message_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_outflow_sec_message"."id" IS '主键';
COMMENT ON COLUMN "jmqtt_outflow_sec_message"."client_id" IS '目标客户端id';
COMMENT ON COLUMN "jmqtt_outflow_sec_message"."msg_id" IS '消息id';
COMMENT ON COLUMN "jmqtt_outflow_sec_message"."gmt_create" IS '消息缓存时间';
COMMENT ON TABLE "jmqtt_outflow_sec_message" IS '发送qos2消息后的第二阶段的消息缓存表：接收pubRec后保留clientId，msgId等报文。设备重连时候进行重发';

-- ----------------------------
-- Table structure for jmqtt_retain_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_retain_message"
(
    "id" int8 NOT NULL,
    "topic"   varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
    "content" text COLLATE "pg_catalog"."default"         NOT NULL,
    CONSTRAINT "jmqtt_retain_message_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_retain_message"."id" IS '主键';
COMMENT ON COLUMN "jmqtt_retain_message"."topic" IS '所属topic';
COMMENT ON COLUMN "jmqtt_retain_message"."content" IS '消息体';
COMMENT ON TABLE "jmqtt_retain_message" IS '保留消息表';

-- ----------------------------
-- Table structure for jmqtt_session
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_session"
(
    "id" int8 NOT NULL,
    "client_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "state"     varchar(12) COLLATE "pg_catalog"."default" NOT NULL,
    "offline_time" int8,
    CONSTRAINT "jmqtt_session_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_session"."id" IS '主键';
COMMENT ON COLUMN "jmqtt_session"."client_id" IS '客户端id';
COMMENT ON COLUMN "jmqtt_session"."state" IS '状态：ONLINE,OFFLINE两种';
COMMENT ON COLUMN "jmqtt_session"."offline_time" IS 'OFFLINE状态时对应的离线时间戳（只有cleanStart为0时候离线才有该数据）';
COMMENT ON TABLE "jmqtt_session" IS '客户端会话状态';

-- ----------------------------
-- Table structure for jmqtt_subscription
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_subscription"
(
    "id" int8 NOT NULL,
    "client_id" varchar(64) COLLATE "pg_catalog"."default"  NOT NULL,
    "topic"     varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
    "qos" int2 NOT NULL,
    CONSTRAINT "jmqtt_subscription_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_subscription"."id" IS '主键';
COMMENT ON COLUMN "jmqtt_subscription"."client_id" IS '客户端id';
COMMENT ON COLUMN "jmqtt_subscription"."topic" IS '订阅的topic';
COMMENT ON COLUMN "jmqtt_subscription"."qos" IS '对应的qos';
COMMENT ON TABLE "jmqtt_subscription" IS '客户端订阅关系';

-- ----------------------------
-- Table structure for jmqtt_will_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS "jmqtt_will_message"
(
    "id" int8 NOT NULL,
    "client_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
    "content"   text COLLATE "pg_catalog"."default"        NOT NULL,
    "gmt_create" int8 NOT NULL,
    CONSTRAINT "jmqtt_will_message_pkey" PRIMARY KEY ("id")
)
;
COMMENT ON COLUMN "jmqtt_will_message"."client_id" IS '客户端id';
COMMENT ON COLUMN "jmqtt_will_message"."content" IS '消息体';
COMMENT ON COLUMN "jmqtt_will_message"."gmt_create" IS '创建时间';
COMMENT ON TABLE "jmqtt_will_message" IS '遗嘱消息表';

-- ----------------------------
-- Indexes structure for table jmqtt_inflow_message
-- ----------------------------
CREATE INDEX IF NOT EXISTS "idx_client_id" ON "jmqtt_inflow_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE UNIQUE INDEX IF NOT EXISTS "uqe_client_id_msg_id" ON "jmqtt_inflow_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
    "msg_id" "pg_catalog"."int4_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Indexes structure for table jmqtt_offline_message
-- ----------------------------
CREATE INDEX IF NOT EXISTS "idx_client_id_offline" ON "jmqtt_offline_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX IF NOT EXISTS "idx_gmt_create_offline" ON "jmqtt_offline_message" USING btree (
    "gmt_create" "pg_catalog"."int8_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Indexes structure for table jmqtt_outflow_message
-- ----------------------------
CREATE INDEX IF NOT EXISTS "idx_client_id_outflow" ON "jmqtt_outflow_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE UNIQUE INDEX IF NOT EXISTS "uqe_client_id_msg_id_outflow" ON "jmqtt_outflow_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
    "msg_id" "pg_catalog"."int4_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Indexes structure for table jmqtt_outflow_sec_message
-- ----------------------------
CREATE INDEX IF NOT EXISTS "idx_client_id_sec" ON "jmqtt_outflow_sec_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE UNIQUE INDEX IF NOT EXISTS "uqe_client_id_msg_id_sec" ON "jmqtt_outflow_sec_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
    "msg_id" "pg_catalog"."int4_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Indexes structure for table jmqtt_retain_message
-- ----------------------------
CREATE UNIQUE INDEX IF NOT EXISTS "uqe_topic" ON "jmqtt_retain_message" USING btree (
    "topic" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Indexes structure for table jmqtt_session
-- ----------------------------
CREATE UNIQUE INDEX IF NOT EXISTS "uqe_client_id_session" ON "jmqtt_session" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Indexes structure for table jmqtt_subscription
-- ----------------------------
CREATE INDEX IF NOT EXISTS "idx_client_id_sub" ON "jmqtt_subscription" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX IF NOT EXISTS "idx_topic" ON "jmqtt_subscription" USING btree (
    "topic" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Indexes structure for table jmqtt_will_message
-- ----------------------------
CREATE UNIQUE INDEX IF NOT EXISTS "uqe_client_id_will" ON "jmqtt_will_message" USING btree (
    "client_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
