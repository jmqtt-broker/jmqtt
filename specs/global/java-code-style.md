# Java 编码规范

> 适用范围：JMQTT Broker 全部 Java 源码

---

## 1. 通用原则

### 1.1 简洁优于炫技

```java
// ✅ 好：清晰直观
if (message != null) {
    dispatcherMessage(message);
}

// ❌ 坏：过度函数式
Optional.ofNullable(message).ifPresent(this::dispatcherMessage);
```

项目面向 MQTT Broker 场景，**高并发、易维护** 优先。避免为了简洁牺牲可读性。

### 1.2 明确优于隐含

- 显式写出关键守卫条件（`if (clientId == null) throw ...`）
- 避免依赖隐式的自动拆箱、方法重载歧义
- 返回值尽量单一语义，不用同一个类型承载多种状态

### 1.3 防御式编程

所有来自外部的输入必须校验：

```java
// ✅ 好
public static void add(String timerId, TimerType type, long delaySec) {
    if (StrUtil.isBlank(timerId) || type == null || delaySec <= 0) {
        throw new IllegalArgumentException("timerId, type must not be blank, delaySec must > 0");
    }
    scheduler.create(timerId, type, delaySec);
}
```

---

## 2. 命名规范

### 2.1 类命名

| 类型 | 后缀/前缀 | 示例 |
|------|----------|------|
| Ctrl | 无后缀，按报文类型 | `ConnectCtrl`, `PubAckCtrl`, `SubscribeCtrl` |
| Handler | `Handler` 后缀 | `ConnectHandler`, `MessageHandler`, `SubscriptionHandler` |
| Store 接口 | `Store` 后缀 | `SessionStore`, `MessageStore` |
| Store 实现 | `Local` 前缀 | `LocalSessionStore`, `LocalMessageStore` |
| MQTT 版本差异化 | `Mqtt_版本_功能` | `Mqtt_3_4_message`, `Mqtt_5_connect` |
| PO 持久化对象 | 无后缀 | `Session`, `WillMessage`, `SubscriptionDO` |
| DTO | 无后缀 | `ClusterMessageRecord`, `ClusterSessionRecord` |
| Helper/Util | `Helper`/`Util` 后缀 | `MessageUtil`, `ChannelHelper` |
| 枚举 | 无后缀 | `Reason`, `TimerType`, `SessionState.StateEnum` |

### 2.2 方法命名

| 操作 | 动词 | 示例 |
|------|------|------|
| 获取 | `get` / `find` | `getSession(clientId)`, `findSubscription(topic)` |
| 判断 | `is` / `has` / `exist` | `existInboundMsg(clientId, msgId)`, `isCleanStart()` |
| 创建 | `create` / `add` | `createOutboundQuota(clientId, max)`, `add(timerId, type, delay)` |
| 移除 | `remove` / `cancel` / `destroy` | `removeSubscription(clientId, topic)`, `destroyOutbound(clientId)` |
| 更新 | `update` / `set` | `updateSessionState(clientId, state)` |
| 触发执行 | `dispatch` / `process` / `handle` | `dispatcherMessage(msg)`, `processCtrl(ctx, mqttMsg)` |

### 2.3 变量命名

```java
// 集合用复数
Map<String, Subscription> subscriptions;
List<Message> offlineMessages;

// 计数器用 count
int messageCount;
AtomicInteger quota;

// 布尔值用 is/has/can/should
boolean cleanStart;
boolean isActive;
boolean existInboundMsg;

// 临时变量尽量短
Message msg;
Subscription sub;
String clientId;
```

### 2.4 常量

```java
// 类内常量用 static final + 全大写下划线
public static final int DEFAULT_KEEPALIVE = 60;
private static final String DEFAULT_USERNAME = "jmqtt";

// 配置项默认值统一在 BrokerConfig / JmqttConfig 中定义
// 不要在业务代码中硬编码
```

---

## 3. 日志规范

### 3.1 框架

统一使用 **SLF4J + Logback**，通过 Lombok `@Slf4j` 注入。

```java
@Slf4j
public class SomeCtrl {
    // log 变量自动注入
}
```

**禁止**：`System.out.println`、`e.printStackTrace()`、直接使用 Logback Logger。

### 3.2 日志级别

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| `error` | 系统错误、必须人工介入 | 数据库连接失败、Hazelcast 节点异常 |
| `warn` | 业务异常但可自愈 | 消息过期丢弃、Hazelcast 操作返回 null |
| `info` | 关键业务状态变更 | 客户端连接/断开、定时任务创建/完成 |
| `debug` | 调试信息、高频路径 | 每次 PUBLISH 的 topic/qos、Timer dispatch 内部 |

### 3.3 日志格式

```java
// ✅ 好：用占位符，不用字符串拼接
log.info("session timeout, clientId: {}", clientId);

// ❌ 坏：字符串拼接
log.info("session timeout, clientId: " + clientId);

// ✅ 好：异常作为最后一个参数
log.error("publish failed, topic: {}", topic, e);

// ❌ 坏：手动 toString 或拼接 e.getMessage()
log.error("publish failed: " + e.getMessage());

// ✅ 好：多参数
log.info("Schedule task added: timerId={}, type={}, delayMs={}, cycle={}",
        task.getTimerId(), task.getType(), delayMs, task.getCycle());
```

### 3.4 关键节点必须打日志

| 节点 | 级别 | 必填字段 |
|------|------|----------|
| 客户端 CONNECT 成功 | info | clientId, protocol, keepalive, cleanStart |
| 客户端 DISCONNECT | info | clientId, reasonCode |
| 定时任务创建 | info | timerId, type, delay |
| 定时任务 dispatch 异常 | error | timerId, type, exception |
| QoS 过程消息异常（release 返回 null） | warn | clientId, msgId, 操作名 |
| 消息过期丢弃 | warn | topic, msgId, qos |

---

## 4. 异常规范

### 4.1 异常类型

| 类型 | 场景 | 使用方式 |
|------|------|----------|
| `BrokerException` | Broker 内部逻辑异常 | `BrokerException.of(Reason.XXX)` |
| `ConnectException` | 连接校验失败 | `ConnectException.of(MqttConnectReturnCode.XXX, "msg")` |
| `ConfigException` | 配置加载失败 | 直接 throw，带描述 |

### 4.2 使用原则

```java
// ✅ 好：携带 MQTT 原因码，客户端可感知
throw BrokerException.of(Reason.PAYLOAD_FORMAT_INVALID);

// ✅ 好：连接拒绝，返回码明确
throw ConnectException.of(MqttConnectReturnCode.USERNAME_PASSWORD_NOT_AUTHORIZED, "invalid credential");

// ✅ 好：底层异常兜底
try {
    LocalDB.operate(...);
} catch (Exception e) {
    log.error("db operation failed", e);
    throw BrokerException.of(Reason.UNSPECIFIED_ERROR);
}

// ❌ 坏：直接抛 RuntimeException 不带上下文
throw new RuntimeException("db failed");
```

### 4.3 Hazelcast 分布式操作

**所有 Hazelcast 分布式对象操作必须 try-catch**：

```java
try {
    unicastQueue.destroy();
} catch (Exception ignored) {
    // HazelcastInstanceNotActiveException 在 shutdown 时可能出现，忽略
}
```

---

## 5. 注释规范

### 5.1 什么时候写注释

| 必须写 | 不要写 |
|--------|--------|
| 公共 API / 接口的方法说明 | 自解释的代码 |
| 复杂算法、状态机 | 简单的 getter/setter |
| 非显而易见的设计决策 | 语法级注释（如 `// i++ 自增`） |
| 外部依赖的使用前提 | Lombok 注解说明 |

### 5.2 注释风格

```java
/**
 * 处理 CONNECT 报文的版本差异化校验。
 * 链式调用: versionVerify → clientIdVerify → authentication → keepAliveVerify
 *
 * @param clientSession 当前客户端会话
 * @param message CONNECT 报文内容
 * @return 链式校验器本身，支持继续链式调用
 */
public ConnectHandler versionVerify(ClientSession clientSession, MqttConnectMessage message) {
    // ...
    return this;
}
```

### 5.3 设计决策注释

当代码看起来"奇怪"但有正当理由时，必须加注释：

```java
// acquire 先于 cacheInboundMsg 执行 —— 保证配额与缓存的原子性关联
// 如果 cache 先于 acquire，后续 release 找不到匹配的 acquire
if (ReceiveMaximumManager.acquireInbound(clientId)) {
    sessionStore.cacheInboundMsg(clientId, message);
}
```

---

## 6. Lombok 使用

### 6.1 允许的注解

| 注解 | 场景 |
|------|------|
| `@Getter` / `@Setter` | POJO 字段访问 |
| `@NoArgsConstructor` / `@AllArgsConstructor` | PO 类构造器 |
| `@Builder` | 复杂 DTO 构建 |
| `@Slf4j` | 所有 Ctrl/Handler/Service 类 |
| `@Data` | 纯 DTO（同时需要 getter/setter/equals/hashCode/toString） |

### 6.2 不允许

```java
// ❌ 不允许 @Synchronized —— 项目用 CAS / ConcurrentHashMap 保证并发安全
// ❌ 不允许 @Value —— 会让类 final，且需要 Spring（项目不用 Spring）
// ❌ 不允许 @RequiredArgsConstructor —— 除非有明确依赖注入场景
```

---

## 7. 代码组织

### 7.1 单文件行数上限

**建议 300 行以内**。超过时考虑拆分：
- 工具类：拆分为多个工具类
- Ctrl：如果有大量 QoS 处理逻辑，抽到 `dispatcher/helper`
- Store 实现：拆出内部 Manager 类

### 7.2 import 顺序

```java
// 1. JDK 标准库
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// 2. 第三方依赖
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

// 3. 项目内部
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.netty.session.ClientSession;
```

### 7.3 字段顺序

```java
public class OutboundQuota extends Quota {

    // 1. static final 常量
    private static final int DEFAULT_QUEUE_CAPACITY = 1024;

    // 2. 实例字段（按使用顺序）
    private final LinkedBlockingQueue<QuotaMsg> waitQueue;
    private final AtomicBoolean waiting = new AtomicBoolean(false);

    // 3. 构造器
    public OutboundQuota(String clientId, int receiveMaximum) { ... }

    // 4. 公共方法
    @Override
    public boolean acquire(QuotaMsg msg) { ... }

    // 5. 私有方法
    private void consumer() { ... }

    // 6. 内部类
    public static class QuotaMsg { ... }
}
```
