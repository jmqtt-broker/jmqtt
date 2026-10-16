# AGENTS.md — JMQTT 项目指南

> 本文档面向 AI Agent（IDE 助手、代码生成器等），帮助理解项目结构、约定和开发要点。

## 1. 项目概览

**JMQTT** 是一个**去中心化分布式 MQTT Broker**，Java 21 编写，不依赖 Spring Framework。

- **支持协议**：MQTT 3.1、MQTT 3.1.1、MQTT 5.0（统一入口，按版本分流处理）
- **传输层**：TCP、WebSocket、SSL/TCP、SSL/WebSocket（均支持）
- **集群**：基于 Hazelcast（已接入 seedNodes，集群消息转发逻辑待补全）
- **持久化**：内置 H2 文件数据库（通过 MyBatis-Plus）
- **消息模型**：遗嘱消息 (Will)、保留消息 (Retain)、离线消息、QoS 1/2 过程消息缓存

**运行方式**：
```bash
mvn compile                # 编译
java -cp jmqtt-core/target/jmqtt-core-1.0.0.jar com.jmqtt.core.JmqttStartup
```
入口类：`com.jmqtt.core.JmqttStartup`（jmqtt-core/src/main/java/com/jmqtt/core/JmqttStartup.java:92）

## 2. 技术栈

| 类别 | 依赖 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 网络 | Netty | 4.1.54.Final |
| 工具 | Hutool | 5.8.46 |
| 持久化 | MyBatis-Plus | 3.5.17（已排除 mybatis-plus-spring） |
| 数据库 | H2 | 2.5.252 |
| 集群 | Hazelcast | 5.7.0 |
| 配置 | SnakeYAML | 2.7 |
| 日志 | SLF4J + Logback | 1.7.30 / 1.2.13 |
| 简化 | Lombok | 1.18.40 |

## 3. 模块与目录结构

```
jmqtt/                              # 父项目 (Maven pom packaging)
├── pom.xml                         # 父 POM：依赖版本管理、dependencyManagement
├── jmqtt-core/                     # 核心模块（唯一子模块）
│   ├── pom.xml                     # 子模块 POM：声明实际依赖
│   └── src/main/
│       ├── java/com/jmqtt/core/
│       │   ├── JmqttStartup.java                # ⭐ 启动类
│       │   ├── cluster/                         # Hazelcast 集群配置
│       │   │   ├── ClusterConfig.java           #   seedNodes、节点名
│       │   │   └── ClusterFactory.java          #   集群工厂
│       │   ├── common/                          # 通用工具
│       │   │   ├── JmqttConst.java              #   常量（通配符、共享订阅前缀等）
│       │   │   ├── CacheHelper.java             #   缓存辅助
│       │   │   ├── Control.java                 #   启停接口
│       │   │   └── ThreadFactoryImpl.java        #   线程工厂
│       │   ├── config/
│       │   │   └── JmqttConfig.java             # ⭐ 顶层配置对象（broker + netty + cluster）
│       │   ├── exception/
│       │   │   ├── BrokerException.java          #   Broker 内部异常
│       │   │   └── ConnectException.java         #   连接异常（含 MQTT 连接返回码）
│       │   ├── netty/                           # 网络层
│       │   │   ├── NettyServer.java              # ⭐ Netty 服务启动（TCP/WS/SSL 四端口）
│       │   │   ├── NettyConnectHandler.java      #   连接/断开事件处理
│       │   │   ├── NettySslHandler.java          #   SSL 配置
│       │   │   ├── ChannelEventExecutor.java     #   事件分发器
│       │   │   ├── coder/                        #   WebSocket 编解码
│       │   │   ├── config/NettyConfig.java       #   网络配置
│       │   │   ├── event/                        #   连接事件模型
│       │   │   ├── helper/                       #   Channel 工具
│       │   │   └── session/                      #   ClientSession、ClientManager
│       │   └── mqtt/                            # ⭐ MQTT 协议核心
│       │       ├── auth/JmqttAuth.java           #   认证接口（匿名/user/password）
│       │       ├── config/BrokerConfig.java       #   Broker 能力配置
│       │       ├── control/                      # ⭐ 报文处理（按 MQTT 报文类型分）
│       │       │   ├── BaseCtrl.java             #   基类：按版本分发 handler
│       │       │   ├── ConnectCtrl.java          #   CONNECT
│       │       │   ├── PublishCtrl.java          #   PUBLISH
│       │       │   ├── SubscribeCtrl.java        #   SUBSCRIBE
│       │       │   ├── UnSubscribeCtrl.java      #   UNSUBSCRIBE
│       │       │   ├── PubAckCtrl.java            #   PUBACK
│       │       │   ├── PubRecCtrl.java            #   PUBREC
│       │       │   ├── PubRelCtrl.java            #   PUBREL
│       │       │   ├── PubCompCtrl.java           #   PUBCOMP
│       │       │   ├── PingCtrl.java              #   PINGREQ
│       │       │   ├── DisconnectCtrl.java        #   DISCONNECT
│       │       │   ├── dispatcher/MessageDispatcher.java  # ⭐ 消息分发器（异步批量）
│       │       │   ├── handler/                   #   版本差异化处理
│       │       │   │   ├── Mqtt_3_4_connect.java
│       │       │   │   ├── Mqtt_5_connect.java
│       │       │   │   ├── Mqtt_3_4_message.java
│       │       │   │   ├── Mqtt_5_message.java
│       │       │   │   ├── Mqtt_3_4_subscription.java
│       │       │   │   ├── Mqtt_5_subscription.java
│       │       │   │   ├── ConnectHandler.java    #   Connect 链式校验接口
│       │       │   │   └── SubscriptionHandler.java
│       │       │   └── resend/ReSendMessageService.java  # 重连未确认消息重发
│       │       ├── helper/
│       │       │   ├── Reason.java               # ⭐ MQTT v5 原因码枚举（完整 43 个）
│       │       │   ├── MessageUtil.java          #   消息构建工具
│       │       │   └── TopicAliasManager.java     #   MQTT 5 主题别名管理
│       │       ├── model/                        # 领域模型
│       │       │   ├── Message.java              #   统一消息模型
│       │       │   ├── MessageHeader.java        #   Header Key 常量
│       │       │   ├── Subscription.java         #   订阅关系
│       │       │   ├── SubscriptionOption.java   #   MQTT 5 订阅选项
│       │       │   ├── SubTopic.java             #   订阅主题
│       │       │   └── ...DTO / Wrapper
│       │       ├── store/                       # 存储层
│       │       │   ├── SessionStore.java         # ⭐ 会话存储接口
│       │       │   ├── LocalSessionStore.java     #   内存 + H2 实现
│       │       │   ├── MessageStore.java          # ⭐ 遗嘱/保留消息接口
│       │       │   ├── LocalMessageStore.java    #   ConcurrentHashMap + H2 实现
│       │       │   ├── SessionState.java          #   会话状态枚举
│       │       │   └── persist/                  #   MyBatis-Plus 持久化
│       │       │       ├── LocalDB.java           #   H2 单例 + SqlSession
│       │       │       ├── mapper/                #   Mapper 接口
│       │       │       └── po/                    #   数据库实体
│       │       └── subscribe/
│       │           ├── SubscriptionMatcher.java  # ⭐ 订阅匹配接口
│       │           └── DefaultSubscriptionTreeMatcher.java # ⭐ 主题树匹配实现
│       └── resources/
│           ├── jmqtt.yml                         # ⭐ 唯一配置文件
│           ├── logback.xml                       #   日志配置
│           ├── sql/jmqtt_local.sql               #   H2 初始化 DDL
│           └── ssl/                              #   SSL 证书
└── logs/                                         # 运行日志（.gitignore 未忽略，注意敏感信息）
```

## 4. 核心架构

### 4.1 启动流程

```
main() → JmqttStartup() → 加载 jmqtt.yml → 初始化各组件 → LocalDB.startDB() → NettyServer.start()
```

初始化顺序（JmqttStartup.java:42-55）：
1. **加载配置** — `JmqttConfig`（broker + netty + cluster 三段）
2. **事件监听器** — `DefaultChannelEventListener`
3. **NettyServer** — 根据 OS 选 Epoll/KQueue/NIO，支持 TCP/WS/SSL 共 4 个端口
4. **SessionStore / MessageStore** — 本地内存 + H2 持久化实现
5. **SubscriptionMatcher** — 基于主题树的匹配器（支持 `+`/`#`/`$share`/`$SYS`）
6. **JmqttAuth** — 认证接口（当前空实现，走 broker 配置的 user/password）
7. **MessageDispatcher** — 异步批量消息分发器（LinkedBlockingQueue + VirtualThread）
8. **注册 Ctrl** — 按 MqttMessageType 注册 10 个报文处理器到 NettyServer.ctrlTable

### 4.2 消息处理链路

```
Netty MQTT Decoder
    ↓
NettyMqttHandler.channelRead()     ← 每个消息用 VirtualThread 处理
    ↓
ctrlTable.get(messageType).processCtrl()  ← BaseCtrl 子类
    ↓
  ┌───────────── version 分发 ─────────────┐
  │ MQTT 3.1/3.1.1 → Mqtt_3_4_xxx        │
  │ MQTT 5.0       → Mqtt_5_xxx (extends 3_4)  │
  └──────────────────────────────────────┘
    ↓
业务校验 → 存储 → MessageDispatcher.accept() → 异步批量分发
    ↓
订阅匹配 → 在线直接发 / 离线存 DB → 重连补发
```

关键点：
- **Virtual Thread 贯穿全链路**（`Thread.startVirtualThread`、`Executors.newVirtualThreadPerTaskExecutor()`）
- **版本差异通过继承实现**：`Mqtt_5_connect extends Mqtt_3_4_connect`，覆盖差异化逻辑
- **ConnectHandler 使用链式调用**：`versionVerify().clientIdVerify().authentication().keepAliveVerify().verifySuccess()`

### 4.3 订阅匹配（主题树）

`DefaultSubscriptionTreeMatcher` 使用多叉树结构存储订阅：
- 按 `/` 分割 topic 为 Token，逐层建 TreeNode
- `+` 匹配单层任意 Token，`#` 匹配后续所有
- `$share/groupName/topic` 共享订阅按组随机选一个客户端（策略可扩展）
- `$SYS/` 系统主题、`$FILE/` 文件主题作为特殊前缀

### 4.4 存储策略

| 数据 | 存储方式 | 说明 |
|------|----------|------|
| 会话状态 | ConcurrentHashMap + H2 | 先查内存缓存，miss 再查 DB |
| QoS 过程消息 (inflow/outflow) | ConcurrentHashMap | 纯内存，不持久化 |
| 离线消息 | H2 | 客户端上线前发送时落库 |
| 遗嘱消息 | H2 | CONNECT 时存，DISCONNECT/遗嘱触发时清 |
| 保留消息 | H2 | PUBLISH with retain=1 时存 |
| 订阅关系 | H2 | SUBSCRIBE/UNSUBSCRIBE 时维护 |

H2 连接串硬编码在 `LocalDB.java:29`：`jdbc:h2:file:/opt/jmqtt/jmqtt;AUTO_SERVER=true;MODE=MYSQL`
**开发时注意**：Windows 上 `/opt/jmqtt/` 路径不存在，需要改这个路径或建目录。

### 4.5 集群通信

当前状态：**部分接入，转发逻辑未完成**。
- Hazelcast 依赖已引入，`ClusterConfig.seedNodes` 已配置
- `MessageDispatcher.AsyncDispatcher` 中跨 broker 转发处是注释（`ClusterHelper.sendMessageToCluster`）
- `SessionState`、`Subscription` 均带 `brokerId` 字段用于集群路由

## 5. 配置体系

### 5.1 jmqtt.yml（唯一外部配置）

```yaml
broker:
  anonymous: false        # 匿名是否允许
  user: jmqtt             # 默认用户名
  password: jmqtt         # 默认密码

netty:
  tcpPort: 1883
  websocketPort: 1884
  sslTcpPort: 8883
  sslWebsocketPort: 8884

cluster:
  seedNodes: ["127.0.0.1:5701"]
```

BrokerConfig 还有大量硬编码默认值（jmqtt-core/src/main/java/com/jmqtt/core/mqtt/config/BrokerConfig.java）：
- `receiveMaximum: 65535`、`maximumPacketSize: 256MB`、`topicAliasMaximum: 65535`
- `defaultKeepalive: 60s`、`maxOfflineMsgNum: 10000`
- 功能开关：wildcardSubscriptionAvailable、subscriptionIdentifierAvailable、sharedSubscriptionAvailable、retainAvailable
- `maximumQos: 2`（EXACTLY_ONCE）

**注意**：BrokerConfig 的大量字段未暴露到 yml，修改需要改 Java 默认值。

### 5.2 配置传递

```
jmqtt.yml → YamlUtil.load() → JmqttConfig → 拆分为 BrokerConfig / NettyConfig / ClusterConfig
                                            ↓
                                    JmqttStartup 构造器注入到各组件
                                            ↓
                                    BaseCtrl 构造器持有 brokerConfig / clusterConfig
```

## 6. 数据库表

初始化脚本：`jmqtt-core/src/main/resources/sql/jmqtt_local.sql`

| 表名 | 主键 | 说明 |
|------|------|------|
| jmqtt_session | client_id | 会话状态（online/offline/version/keepalive） |
| jmqtt_subscription | id（client_id+topic 唯一约束） | 订阅关系 |
| jmqtt_offline_message | id | 离线消息（JSON 存储完整 Message） |
| jmqtt_will_message | client_id | 遗嘱消息（JSON） |
| jmqtt_retain_message | topic | 保留消息（JSON） |
| jmqtt_broker | broker_id | 集群节点信息 |
| broker_config | broker_id | Broker 配置（JSON） |
| schedule_task | timer_id | 定时任务（session 过期等） |

## 7. 编码约定

### 7.1 命名

| 模式 | 示例 | 说明 |
|------|------|------|
| 版本差异化后缀 | `Mqtt_3_4_connect` / `Mqtt_5_connect` | `_` + 版本号，Mqtt_5 继承 Mqtt_3_4 |
| Ctrl 类 | `ConnectCtrl`、`PublishCtrl` | 按 MQTT 报文类型命名 |
| Handler 接口 | `ConnectHandler`、`MessageHandler` | 链式校验接口 |
| Store 接口 | `SessionStore`、`MessageStore` | 纯接口，带大量 default 方法 |
| Store 实现 | `LocalSessionStore`、`LocalMessageStore` | Local = 单机 |
| PO | `Session`、`SubscriptionDO`、`WillMessage` | MyBatis-Plus @TableName 实体 |
| DTO | `MessageDispatchDTO`、`SubscriptionSyncDTO` | 集群/内部传输 |
| Helper | `MessageUtil`、`ChannelUtil`、`ChannelHelper` | 静态工具 |

### 7.2 代码风格

- **Lombok 注解**：`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` 无处不在
- **日志**：`@Slf4j`（SLF4J），统一 `log.info/warn/error`，**不允许 System.out.println**
- **异常**：`BrokerException.of(Reason.XXX)` / `ConnectException.of(MqttConnectReturnCode.XXX, "msg")`
- **Message 模型**：统一使用 `Message`（msgId + headers + properties + clientId + payload），headers 用 `MessageHeader` 常量 key
- **版本判断**：通过 `MqttVersion.protocolLevel()`（byte 值 4 / 5）做 Map 查表
- **不允许写死 Spring 依赖**：项目**不使用 Spring**，不要引入 Spring Boot / Spring Framework
- **Virtual Thread**：新任务用 `Thread.startVirtualThread(...)` 或 `Executors.newVirtualThreadPerTaskExecutor()`

### 7.3 MQTT 版本兼容

| 特性 | 3.1 / 3.1.1 | 5.0 | 处理方式 |
|------|-------------|-----|----------|
| CONNECT 返回码 | MqttConnectReturnCode | MqttConnectReturnCode + Reason Properties | `Mqtt_5_connect.verifySuccess()` 扩展 |
| 消息属性 | 无 | MqttProperties | `MessageUtil.propertyMap()` 统一转换 |
| 原因码 | N/A | Reason Code (43 个) | `Reason` 枚举 |
| 主题别名 | 无 | TopicAlias (入站/出站) | `TopicAliasManager` |
| 订阅标识符 | 无 | SubscriptionIdentifier | `SubscriptionOption` |
| 过期消息 | 无 | PublicationExpiryInterval | `Message.validity()` / `Message.alive()` |

## 8. 常见问题 / 踩坑记录

### 8.1 MyBatis-Plus SPI 加载失败
**问题**：`ServiceConfigurationError: com.baomidou.mybatisplus.spring.spi.SpringCompatibleSet`
**原因**：`mybatis-plus` 聚合包传递引入了 `mybatis-plus-spring`，但项目没有 Spring
**修复**：在 `jmqtt-core/pom.xml` 中对 `mybatis-plus` 加 exclusion 排除 `mybatis-plus-spring`

### 8.2 SLF4J "Failed to load StaticLoggerBinder"
**问题**：SLF4J 退化为 NOP，日志全部丢失
**原因**：只有 `slf4j-api`，没有绑定器实现
**修复**：父 pom 引入 `ch.qos.logback:logback-classic:1.2.13`，classpath 放 `logback.xml`（不是 `logback-xxx.xml`）

### 8.3 H2 数据库路径硬编码
`LocalDB.java:29` 硬编码 `jdbc:h2:file:/opt/jmqtt/jmqtt`，Windows 上不存在 `/opt/jmqtt/` 导致启动失败。开发时需手动修改。

### 8.4 pom.xml 依赖管理结构
- 父 pom：`dependencyManagement` 里定义版本，`dependencies` 里放公共依赖（slf4j-api、lombok）
- 子 pom：声明具体依赖，**不加 version**（继承父 pom）
- 聚合包 `mybatis-plus` 会同时拉 mybatis-plus-core + mybatis-plus-spring，需要 exclusion

### 8.5 配置文件是唯一外部入口
所有可调参数集中在 `jmqtt.yml`，但 `BrokerConfig` 里大量字段有 Java 默认值且未暴露到 yml。**添加新配置项要同时改三处**：`BrokerConfig` 加字段 → `jmqtt.yml` 加键 → `JmqttStartup` 传递。

## 9. 数据库实体继承关系

```
Broker / BrokerConfig          ← 集群节点
Session                         ← 会话状态
  └── SessionState              ← 内存态 + StateEnum (ONLINE/OFFLINE/NEVER_CONNECTED)
SubscriptionDO                  ← DB 存储
  └── Subscription              ← 运行态 (有 Qos、Option、BrokerId)
OfflineMessage                  ← 离线消息（JSON content）
WillMessage                     ← 遗嘱消息（JSON content）
RetainMessage                   ← 保留消息（JSON content）
ScheduleTask                    ← 定时任务（session 过期等）
```

注意区别：`Session`（PO，持久化）vs `SessionState`（运行态，含内存缓存 + properties）

## 10. Virtual Thread 使用模式

项目大量使用 Java 21 Virtual Thread，模式固定：

```java
// 模式1：单任务
Thread.startVirtualThread(() -> ctrlTable.get(type).processCtrl(ctx, msg));

// 模式2：Executor（MessageDispatcher）
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(new AsyncDispatcher(batch));
```

**注意**：Virtual Thread 上不要做长时间阻塞的 IO 操作（如同步 DB 调用），避免阻塞 Carrier Thread 线程池。当前 `LocalDB.operate()` 用的是 MyBatis 同步调用，会阻塞 VT，但因为 `AUTO_SERVER=true` H2 连接本身很快，问题不大。

## 11. 日志输出模式

```xml
<!-- logback.xml 中配置了三个 appender -->
<!-- STDOUT: 控制台，INFO 及以上 -->
<!-- FILE: 滚动文件，logs/jmqtt-info.log，INFO 及以上 -->
<!-- ERROR: 滚动文件，logs/jmqtt-error.log，ERROR 仅 -->
<!-- logger com.github.jmqtt: INFO, STDOUT + FILE + ERROR -->
<!-- logger io.netty: WARN -->
```

## 12. 待实现 / TODO 清单（代码中已标记）

1. **集群消息转发** — `MessageDispatcher.AsyncDispatcher` 中跨 broker 发送处是注释
2. **共享订阅均衡策略** — 目前只有 random，hash 策略待实现
3. **Session 过期定时器** — `TimerUtils` 相关调用被注释
4. **Will 消息延迟发送** — `stopSessionAndWillMessageTimeout()` 是空实现
5. **Subscription 从 DB 恢复到内存树** — `LocalSessionStore.clearSubscriptionAndSubTree()` 中注释了 tree 操作
6. **BrokerConfig 全量暴露到 jmqtt.yml** — 大量参数仍硬编码默认值
7. **测试用例** — 项目当前没有 src/test 目录

## 13. 本地开发 Checklist

1. Java 21 环境
2. `mvn compile` 无报错
3. 启动前确保 `/opt/jmqtt/` 目录存在（或改 LocalDB 的 H2 路径）
4. 查看 `jmqtt-info.log` 确认端口监听成功
5. 用 MQTTX / mosquitto_pub 等工具连接测试：
   ```bash
   mosquitto_sub -h 127.0.0.1 -p 1883 -t "test" -u jmqtt -P jmqtt
   mosquitto_pub -h 127.0.0.1 -p 1883 -t "test" -m "hello" -u jmqtt -P jmqtt
   ```
