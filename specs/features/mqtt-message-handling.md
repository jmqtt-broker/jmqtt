# MQTT 消息处理规范

> 适用范围：JMQTT Broker 全部 MQTT 报文处理、QoS 过程管理、配额管理、幂等设计

---

## 1. QoS 过程消息缓存

### 1.1 缓存表结构

| 缓存 | 数据结构 | 含义 | 创建条件 | 清理条件 |
|------|----------|------|----------|----------|
| recCache | `Map<clientId, Map<msgId, Message>>` | QoS 2 入站：PUBREC 已发出 | PUBREC 发送前 | PUBREL 收到（releaseInboundMsg） |
| sendCache | `Map<clientId, Map<msgId, Message>>` | QoS 1/2 出站：PUBLISH 已发出 | PUBLISH 发送前 | PUBACK / PUBREC 收到 |
| secTwoCache | `Map<clientId, Map<msgId, Object>>` | QoS 2 出站第二阶段：PUBREL 已发出 | PUBREL 发送前 | PUBCOMP 收到 |

### 1.2 缓存外层 Map 必须定期清理

内层 `Map<msgId, Message>` 正常会被 remove，但外层 `clientId → 内层 Map` 的 entry **永不 remove 会导致内存泄漏**。

**清理时机**：
- `releaseXxxMsg(clientId, null)` 传 null 表示清理整个 clientId 的缓存
- `clearSession(clientId, true)` 内部调三个 release 方法
- Session Expiry 定时任务触发

```java
// ✅ 好：外层 entry 一并 remove
@Override
public Message releaseOutboundMsg(String clientId, Integer msgId) {
    ConcurrentHashMap<Integer, Message> v = outboundCache.get(clientId);
    if (msgId == null) {
        if (v != null) v.clear();
        outboundCache.remove(clientId);  // ← 关键：外层也要 remove
        return null;
    }
    return v != null ? v.remove(msgId) : null;
}
```

### 1.3 缓存时机与网络发送解耦

**核心原则**：业务状态变更（cache、release）先于网络回执发送。

```java
// ✅ 好：先 cache，发送失败不影响状态
sessionStore.cacheInboundMsg(clientId, message);
clientSession.getCtx().writeAndFlush(pubRecMessage);

// ❌ 坏：cache 绑死发送成功
clientSession.getCtx().writeAndFlush(pubRecMessage).addListener(future -> {
    if (future.isSuccess()) {
        sessionStore.cacheInboundMsg(clientId, message);  // 网络失败 → cache 丢了！
    }
});
```

**原因**：MQTT 过程消息的语义是"Broker 已处理"，网络回执只是告诉客户端当前状态。回执发出去就算了，失败了客户端会重发，Broker 靠缓存幂等拦截。

---

## 2. 配额管理（Receive Maximum）

### 2.1 acquire / release 必须配对

每一个 `acquireOutbound` / `acquireInbound` 必须有且只有一个对应的 `release`。以下是所有必须 release 的场景：

**出站 release 位置**（5 个）：
```
send() 正常 → PubAckCtrl (QoS 1) 或 PubCompCtrl (QoS 2)
send() write future 失败 → future listener 内 release
send() channel inactive → write else 分支 release
send() 消息过期 → alive() <= 0 分支 release
consumer 从 waitQueue 取出后 acquire 失败 → 消息重新入队，不 release
```

**入站 release 位置**：
```
QoS 1: handleQos1 末尾（每次都 release，重发也 acquire+release 配对）
QoS 2: PubRelCtrl 内 release（existInboundMsg 前置检查，重发不 acquire）
```

### 2.2 配额获取必须先于缓存

```java
// ✅ 好：先 acquire，成功后才 cache + 发 PUBLISH
if (qos > 0 && mqtt5) {
    if (!ReceiveMaximumManager.acquireOutbound(...)) {
        return;  // 配额不足，不缓存
    }
    sessionStore.cacheOutboundMsg(clientId, message);
}
write(...);

// ❌ 坏：先 cache 再 acquire
sessionStore.cacheOutboundMsg(clientId, message);
if (qos > 0 && mqtt5) {
    if (!ReceiveMaximumManager.acquireOutbound(...)) {
        return;  // ❌ 消息已缓存但没 acquire，后续永远不会 release
    }
}
```

### 2.3 CAS 原子性模式

配额计数器用 CAS 自旋保证原子性，**禁止**用 synchronized 或 ReentrantLock：

```java
// ✅ 好：CAS 自旋
public boolean acquire() {
    while (true) {
        int current = quota.get();
        if (current <= 0) return false;
        if (quota.compareAndSet(current, current - 1)) return true;
    }
}

// ❌ 坏：分两步非原子操作
public boolean acquire() {
    if (quota.intValue() == 0) return false;  // 步骤 1
    return quota.decrementAndGet() >= 0;      // 步骤 2 —— 并发时可能变负数
}
```

### 2.4 QoS 0 消息不消耗配额

```java
// ✅ 好：只有 qos > 0 才 acquire
if (qos > 0) {
    if (mqtt5) {
        if (!ReceiveMaximumManager.acquireOutbound(...)) return;
    }
    sessionStore.cacheOutboundMsg(clientId, message);
}
// ❌ 坏：对所有消息（含 QoS 0）都 acquire —— QoS 0 不会 release，配额泄漏
```

---

## 3. 幂等处理

### 3.1 MQTT 协议本身的幂等语义

MQTT 协议保证客户端**重复发送同一 msgId 的控制报文内容不变**。Broker 必须利用这个特性做幂等拦截。

| 重发场景 | Broker 响应 | 幂等方式 |
|----------|------------|----------|
| PUBLISH(QoS 1) DUP | 重发 PUBACK | 每次 handle 都 acquire+release，允许重复 dispatcher |
| PUBLISH(QoS 2) DUP | 重发 PUBREC | existInboundMsg 前置检查，只回 PUBREC |
| PUBREC 重发 | 重发 PUBREL | releaseOutboundMsg 返回 null → 查 secTwoCache |
| PUBREL 重发 | 重发 PUBCOMP | releaseInboundMsg 返回 null → 直接发 PUBCOMP |
| PUBCOMP 重发 | 重发 PUBCOMP | releaseOutboundSecMsgId 返回 false → 跳过 releaseOutbound |

### 3.2 幂等拦截的三个层次

```
层次 1：缓存查找拦截（最常用）
  if (sessionStore.existInboundMsg(clientId, msgId)) {
      // 重发，只回 PUBREC
      return;
  }

层次 2：release 返回 null 守卫（Ctrl 入口）
  Message message = sessionStore.releaseOutboundMsg(clientId, msgId);
  if (message == null) {
      // 已处理过，跳过后续逻辑，只回复
      ctx.writeAndFlush(pubRelMessage);
      return;
  }

层次 3：release 返回值判断（避免重复 release 配额）
  boolean flag = sessionStore.releaseOutboundSecMsgId(clientId, msgId);
  if (flag) {
      ReceiveMaximumManager.releaseOutbound(clientId);
  }
```

### 3.3 QoS 2 完整幂等处理流程

```java
// Mqtt_5_message.handleQos2 — 入站 PUBLISH
if (!sessionStore.existInboundMsg(clientId, msgId)) {
    if (ReceiveMaximumManager.acquireInbound(clientId)) {
        sessionStore.cacheInboundMsg(clientId, message);
        ctx.writeAndFlush(pubRecMessage);
    } else {
        MessageUtil.sendDisconnect(ctx, Reason.RECEIVE_MAXIMUM_EXCEEDED);
    }
} else {
    ctx.writeAndFlush(pubRecMessage);  // 重发，只回 PUBREC
}

// PubRecCtrl — 出站 PUBREC
Message message = sessionStore.releaseOutboundMsg(clientId, msgId);
if (message != null) {
    sessionStore.cacheOutboundSecMsgId(clientId, msgId);  // 先缓存 secTwoCache
    ctx.writeAndFlush(pubRelMessage);
} else {
    // sendCache 没了，但 secTwoCache 可能有（上次已进第二阶段，PUBREL 丢了）
    if (sessionStore.existOutboundSecMsgId(clientId, msgId)) {
        ctx.writeAndFlush(pubRelMessage);  // 只重发 PUBREL
    } else {
        log.warn("pubRec message not found...");
    }
}

// PubRelCtrl — 入站 PUBREL
Message message = sessionStore.releaseInboundMsg(clientId, msgId);
if (message != null) {
    ReceiveMaximumManager.releaseInbound(clientId);  // 先释放配额
    if (handler != null) {
        handler.dispatcherMessage(message);
    }
}
ctx.writeAndFlush(pubComMessage);  // 无论如何都回 PUBCOMP
```

---

## 4. 消息分发规范

### 4.1 消息对象不可变

`Message` 对象在多订阅者分发场景下必须拷贝：

```java
// ✅ 好：多订阅者时 copy，避免覆盖 QOS/msgId
for (Subscription subscription : subscriptions) {
    if (subscriptions.size() > 1) {
        Message copy = message.copy();
        send(subscription, copy);
    } else {
        send(subscription, message);  // 单订阅者无需 copy
    }
}
```

### 4.2 qos 计算在 acquire 之前

```java
// ✅ 好：先算 qos，只在 qos > 0 时 acquire
int minQos = Math.min(publisherQos, subscription.getMaxQos());
if (minQos > 0) {
    if (mqtt5) {
        if (!ReceiveMaximumManager.acquireOutbound(...)) return;
    }
    sessionStore.cacheOutboundMsg(clientId, message);
}
message.putHeader(MessageHeader.QOS, minQos);
```

### 4.3 消息过期处理

出站消息发送前检查 `alive()`：

```java
if (message.validity() && message.alive() > 0) {
    write(...);
} else {
    log.warn("message expired. {}", JSONUtil.toJsonStr(message));
    if (mqtt5 && qos > 0) {
        ReceiveMaximumManager.releaseOutbound(clientId);        // 必须释放配额
        sessionStore.releaseOutboundMsg(clientId, msgId);      // 必须清缓存
    }
}
```

---

## 5. 常见陷阱 Checklist

### 5.1 配额泄漏

- [ ] 所有 acquire 都有对应的 release 吗？（5 个出站 + 2 个入站位置）
- [ ] write future 失败时 release 了吗？
- [ ] channel inactive 时 release 了吗？
- [ ] 消息过期时 release 了吗？
- [ ] cache/release 是否绑死在 future.success 里？（**绝对禁止**）

### 5.2 缓存泄漏

- [ ] releaseXxxMsg(clientId, null) 是否同时 remove 外层 Map entry？
- [ ] 是否有 Session Expiry 定时清理？
- [ ] 是否有孤儿消息兜底扫描？

### 5.3 并发安全

- [ ] CAS acquire/release 是否有竞态窗口（非原子 get + decrement）？
- [ ] waitQueue consumer 的 waiting 标志在所有退出路径都重置了吗？
- [ ] Hazelcast destroy 是否有 try-catch？

### 5.4 幂等完整性

- [ ] QoS 2 PUBLISH 重发 → existInboundMsg 拦截，不重复 acquire？
- [ ] PUBREC 重发 → 查 secTwoCache，重发 PUBREL？
- [ ] PUBREL 重发 → 直接发 PUBCOMP，不重复 release？
- [ ] PUBCOMP 重发 → 不重复 releaseOutbound？
- [ ] PUBACK 重发 → 不重复 releaseOutbound？
