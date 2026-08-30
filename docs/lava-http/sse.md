# SSE

## 打开事件流

```java
SseSession session = client.openSse(request, new SseListener() {
    @Override
    public void onEvent(SseSession current, SseEvent event) {
        consume(event.id(), event.type(), event.data());
    }

    @Override
    public void onTerminal(SseSession current, SseTerminal terminal) {
        record(terminal.termination(), terminal.failure());
    }
});
```

监听器回调运行在传输回调线程，应快速返回。耗时业务应转交应用自己的有界执行器。

## 超时与恢复

SSE 默认没有总调用超时，默认事件空闲超时为 2 分钟：

```java
SseOptions options = SseOptions.builder()
        .idleTimeout(Duration.ofMinutes(5))
        .lastEventId(lastProcessedEventId)
        .build();

SseSession session = client.openSse(request, options, listener);
```

空闲超时设为零表示不限制。`lastEventId(...)` 会作为恢复游标发送给服务端。

## 终态

每个会话恰好收到一次终态：

| 终态 | 含义 |
| --- | --- |
| `CANCELLED` | 调用方关闭或取消 |
| `REMOTE_CLOSED` | 服务端正常关闭事件流 |
| `FAILED` | 握手、传输、协议或空闲超时失败 |

```java
session.cancel();
session.close(); // 等价于取消

Optional<SseTerminal> terminal = session.terminal();
```

客户端不会自动重连。需要恢复时，由业务根据终态、退避策略和最后成功处理的事件 ID 显式创建新会话。
