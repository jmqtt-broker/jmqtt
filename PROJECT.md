# PROJECT.md — JMQTT 项目全景

> 面向 AI Agent 和新成员。读完本文后你应该能在 30 分钟内理解：项目要做什么、核心调用链怎么走、改代码时哪里最容易踩坑。

---

## 1. 项目目标

**JMQTT** 是一个**去中心化分布式 MQTT Broker**。

- **不依赖 Spring**：纯 Java 21 + Netty + Hazelcast + MyBatis-Plus
- **内置 H2**：启动即用，无需外部数据库
- **全 Virtual Thread**：Netty handler 层每报文一个 VT，消息分发用 VT Executor
- **协议覆盖**：MQTT 3.1 / 3.1.1 / 5.0，版本差异通过 `Mqtt_3_4_xxx` / `Mqtt_5_xxx` 继承体系处理

**当前完成度**：核心消息链路完整，集群部分 Handler 已注册，定时调度模块底层实现完毕但业务集成待做（Session Expiry、Will Delay、延迟消息）。

---

## 2. 启动时序（完整调用链）

```
JmqttBroker.main(String[] args)
  └─ JmqttBroker.start(args)
       ├─ [1] 加载 jmqtt.yml → JmqttConfig（broker / netty / cluster 三段）
       ├─ [2] LocalDB.startDB() → 连接 H2，按需执行 jmqtt_local.sql
       ├─ [3] 初始化三大核心组件：
       │      new DefaultSubscriptionTreeMatcher()
       │      new LocalSessionStore(matcher)
       │      new LocalMessageStore()
       ├─ [4] MessageDispatcher(sessionStore, messageStore, matcher)
       │      └─ 内部创建 LinkedBlockingQueue + VT Executor + pollThread
       ├─ [5] JmqttCluster.start(clusterConfig) → Hazelcast.newHazelcastInstance()
       │      └─ 设置 System.setProperty("hazelcast.logging.type", "slf4j")
       │      └─ 设置 System.setProperty("hazelcast.shutdownhook.enabled", "false")
       ├─ [6] TimerUtil.start() → ScheduleTaskScheduler
       │      └─ Netty HashedWheelTimer(tick=150ms, wheelSize=512)
       │      └─ LocalDBScheduleTaskStorage.reload() 从 schedule_task 表恢复
       ├─ [7] initCtrl() — 注册 10 个 Ctrl 到 JmqttContext 静态 Map
       │      MqttMessageType.CONNECT   → ConnectCtrl
       │      MqttMessageType.DISCONNECT → DisconnectCtrl
       │      MqttMessageType.PUBLISH   → PublishCtrl
       │      MqttMessageType.SUBSCRIBE → SubscribeCtrl
       │      MqttMessageType.UNSUBSCRIBE → UnSubscribeCtrl
       │      MqttMessageType.PUBACK    → PubAckCtrl
       │      MqttMessageType.PUBREC    → PubRecCtrl
       │      MqttMessageType.PUBREL    → PubRelCtrl
       │      MqttMessageType.PUBCOMP   → PubCompCtrl
       │      MqttMessageType.PINGREQ   → PingCtrl
       ├─ [8] JmqttContext.setBroker(this)
       └─ [9] NettyServer.start() → 启动 TCP(1883) / WS(1884) / SSL(8883) / SSL-WS(8884)
```

**关闭时序**（JVM shutdown hook 触发）：

```
JmqttBroker.shutdown()
  1. nettyServer.stop()       → 停止接收新连接，关闭 channel
  2. messageDispatcher.stop() → pollThread 退出
  3. TimerUtil.stop()         → scheduleTimer.stop()
  4. JmqttCluster.shutdown()  → HazelcastInstance.shutdown()（try-catch Throwable）
```

---

## 3. 报文处理调用链（以 PUBLISH 为例）

```
┌─ Netty Server ─────────────────────────────────────────────────┐
│  MqttDecoder → MqttPublishMessage                               │
│       ↓                                                         │
│  NettyMqttHandler.channelRead0(ctx, msg)                        │
│       ↓                                                         │
│  Thread.startVirtualThread(() -> {                               │
│      JmqttContext.getCtrl(MqttMessageType.PUBLISH)              │
│          .processCtrl(ctx, msg)                                │
│  })                                                             │
└─────────────────────────────────────────────────────────────────┘
       ↓
┌─ Ctrl 层 ──────────────────────────────────────────────────────┐
│  PublishCtrl.processCtrl(ctx, msg)                              │
│    ├─ BaseCtrl.preCheck() 认证 + Keepalive + Topic 合法性        │
│    └─ handler.message(ctx, msg)                                 │
│          ↓                                                      │
│       MqttVersion.protocolLevel() → 查表                        │
│          v4 → new Mqtt_3_4_message().dispatcherMessage(...)    │
│          v5 → new Mqtt_5_message().dispatcherMessage(...)       │
│                ↑ extends Mqtt_3_4_message                       │
└─────────────────────────────────────────────────────────────────┘
       ↓
┌─ 版本差异化层 ─────────────────────────────────────────────────┐
│  Mqtt_3_4_message / Mqtt_5_message                              │
│    ├─ QoS 0：直接 dispatcherMessage → MessageDispatcher.accept   │
│    ├─ QoS 1：cacheInboundMsg(PUBACK msgId) → dispatcherMessage  │
│    │         → ReceiveMaximumManager.acquireInbound → write      │
│    └─ QoS 2：existInboundMsg 前置检查                           │
│              ├─ 新消息 → acquireInbound → cacheInbound → PUBREC  │
│              └─ 重发 → 只回 PUBREC（不 acquire）                 │
└─────────────────────────────────────────────────────────────────┘
       ↓
┌─ 消息分发层 ───────────────────────────────────────────────────┐
│  MessageDispatcher.accept(Message)                             │
│    ├─ sessionStore.getSubscriptions(topic)                     │
│    ├─ subscriptionMatcher.match(topic) → List<Subscription>     │
│    ├─ 多订阅者时 message.copy()                                 │
│    └─ 对每个 Subscription：                                    │
│         qos = Math.min(qos, subscription.maxQos)                │
│         if (qos > 0 && mqtt5) {                                 │
│           ReceiveMaximumManager.acquireOutbound → 失败入队 waitQueue│
│           sessionStore.cacheOutboundMsg(clientId, msg)          │
│         }                                                       │
│         clientSession.getCtx().writeAndFlush(message)           │
└─────────────────────────────────────────────────────────────────┘
       ↓
┌─ PUBACK / PUBREC / PUBREL / PUBCOMP 回调链 ────────────────────┐
│  PubAckCtrl → releaseOutboundMsg → receiveMaximum.releaseOutbound│
│  PubRecCtrl → releaseOutboundMsg → cacheOutboundSecMsgId → PUBREL│
│  PubRelCtrl → releaseInboundMsg → receiveMaximum.releaseInbound│
│  PubCompCtrl → releaseOutboundSecMsgId → receiveMaximum.releaseOutbound│
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 高频改动区（热路径）

| 区域 | 文件 | 什么时候改 | 风险 |
|------|------|----------|------|
| **报文处理 Ctrl** | `mqtt/control/PublishCtrl.java` 等 10 个 | 新增/修改 MQTT 报文处理逻辑 | 容易破坏版本分流 |
| **版本差异化** | `mqtt/control/handler/Mqtt_3_4_message.java` / `Mqtt_5_message.java` | 协议升级、QoS 流程变更 | 必须保持幂等和 acquire/release 配对 |
| **配额管理** | `mqtt/helper/ReceiveMaximumManager.java` | MQTT 5 Receive Maximum 变更 | CAS 原子性不能破坏，泄漏会静默累积 |
| **消息分发** | `mqtt/control/dispatcher/MessageDispatcher.java` | 路由规则、批量策略 | pollThread 退出条件必须覆盖所有路径 |
| **会话存储** | `mqtt/store/LocalSessionStore.java` | Session/Subscription 持久化 | ConcurrentHashMap 外层 entry 清理 |
| **订阅匹配** | `mqtt/subscribe/DefaultSubscriptionTreeMatcher.java` | Topic 树结构、共享订阅 | 改树结构会影响所有 match() 调用 |

---

## 5. 风险点（修改前必读）

### 🔴 P0 — 配额泄漏

**问题**：`ReceiveMaximumManager.acquire()` 执行后，所有退出路径必须有对应的 `release()`。

**泄漏位置清单**：

```
出站 release 位置（5 处，缺一不可）：
  1. PubAckCtrl — PUBACK 收到后
  2. PubCompCtrl — PUBCOMP 收到后
  3. write future 失败 — future listener 内
  4. channel inactive — write else 分支
  5. 消息过期 — alive() <= 0 分支

入站 release 位置（2 处）：
  1. Mqtt_3_4_message.handleQos1 — 每次处理末尾
  2. PubRelCtrl — PUBREL 收到后（existInboundMsg 前置拦截重发）
```

### 🔴 P0 — CAS 原子性

ReceiveMaximumManager 用 `AtomicInteger` + CAS 自旋。**禁止**写出两步非原子操作：

```java
// ❌ 错误
if (quota.intValue() == 0) return false;
return quota.decrementAndGet() >= 0;

// ✅ 正确
while (true) {
    int current = quota.get();
    if (current <= 0) return false;
    if (quota.compareAndSet(current, current - 1)) return true;
}
```

### 🔴 P0 — Hazelcast 关闭竞态

Hazelcast 5.x 在 shutdown 期间仍会向 consumer 线程投递消息，处理不当会抛 `HazelcastInstanceNotActiveException`。

必须做：
1. 启动时禁用自动 hook：`System.setProperty("hazelcast.shutdownhook.enabled", "false")`
2. JmqttCluster.shutdown() 中 `AtomicBoolean` 守卫并发关闭
3. 所有 Hazelcast 分布式对象操作 `try-catch Throwable`

### 🟡 P1 — ConcurrentHashMap 外层 entry 泄漏

SessionStore 的缓存结构是 `Map<clientId, Map<msgId, Message>>`。内层 Map 会被清理，但外层 `clientId → 内层Map` 的 entry 如果不在 logout / session expiry 时 remove，会永久累积。

正确写法：`release*(clientId, null)` 时同时 `outerMap.remove(clientId)`。

### 🟡 P1 — cache 先于网络发送

QoS 过程消息的业务状态（cache、release）必须先于网络回执发送。回执发送失败不应该回滚业务状态——MQTT 协议本身靠客户端重发来保证一致性。

### 🟡 P1 — MyBatis-Plus 排除 spring

`jmqtt-core/pom.xml` 中 mybatis-plus 必须排除 mybatis-plus-spring。否则会在非 Spring 环境下 SPI 加载失败。

### 🟢 P2 — H2 路径硬编码

`LocalDB.java:29` 硬编码 `/opt/jmqtt/jmqtt`。Windows 开发需要手动改路径。生产部署时建议通过环境变量传递。

---

## 6. 关键数据结构

### 6.1 SessionStore 内部缓存（内存态）

```java
// QoS 1 出站：PUBLISH 已发出，等 PUBACK
Map<String, ConcurrentHashMap<Integer, Message>> sendCache;

// QoS 2 出站第二阶段：PUBREL 已发出，等 PUBCOMP
Map<String, ConcurrentHashMap<Integer, Object>> secTwoCache;

// QoS 2 入站：PUBREC 已发出，等 PUBREL
Map<String, ConcurrentHashMap<Integer, Message>> recCache;
```

### 6.2 ReceiveMaximumManager 配额结构

```java
// 每个 client 两个独立配额
Map<String, OutboundQuota> OUT_FLOW_MAP;  // 出站配额 + waitQueue
Map<String, Quota> IN_FLOW_MAP;           // 入站配额

// Quota 基类
class Quota {
    AtomicInteger quota;  // 剩余配额
    int receiveMaximum;   // 上限
}

// OutboundQuota 子类（额外持有 waitQueue + 时间轮驱动 consumer）
class OutboundQuota extends Quota {
    LinkedBlockingQueue<Message> waitQueue;
}
```

### 6.3 主题树节点结构

```java
class TreeNode {
    String token;                        // 当前层级 token，`+` / `#` / 具体字符串
    Map<String, TreeNode> children;      // 子节点
    Set<Subscription> subscriptions;     // 订阅到此节点的客户端
    boolean isWildcardPlus;              // 是否是 + 通配
    boolean isWildcardHash;              // 是否是 # 通配
}
```

---

## 7. 存储层

H2 文件数据库 + MyBatis-Plus，共 8 张表：

| 表 | 说明 | 何时读写 |
|----|------|---------|
| jmqtt_session | 会话状态 | CONNECT / DISCONNECT / shutdown 恢复 |
| jmqtt_subscription | 订阅关系 | SUBSCRIBE / UNSUBSCRIBE / CONNECT 恢复 |
| jmqtt_offline_message | 离线消息 | PUBLISH 时对方离线 / CONNECT 时拉取 |
| jmqtt_will_message | 遗嘱消息 | CONNECT 时注册 / DISCONNECT clean 时删除 / 异常离线触发 |
| jmqtt_retain_message | 保留消息 | PUBLISH retain=true / SUBSCRIBE 时推送 |
| jmqtt_broker | 集群节点信息 | 集群 join 时注册 |
| broker_config | Broker 配置 | 预留 |
| schedule_task | 定时任务 | TimerUtil 创建/取消/重启恢复 |

DDL：`jmqtt-core/src/main/resources/sql/jmqtt_local.sql`

---

## 8. 与 AGENTS.md、README.md 的关系

| 文档 | 读者 | 内容侧重 |
|------|------|---------|
| README.md | 所有人 | 怎么启动、怎么构建、常见报错 |
| PROJECT.md | AI / 新成员（本文档） | 为什么这样设计、调用链怎么走、哪里不能碰 |
| AGENTS.md | AI / 协作者 | 禁止行为清单、编码红线、修改后检查清单 |
| specs/* | 所有人 | 详细规范（MQTT 消息处理、并发编程、Java 编码风格） |
