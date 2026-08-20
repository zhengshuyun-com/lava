# lava-http

`lava-http` 是线程安全的 HTTP、流式响应和通用 SSE 客户端。普通入口不暴露 OkHttp 类型；高级传输定制集中在 `OkHttpInterop`
。缓冲响应不需要关闭，只有流式响应和 SSE 会话持有网络资源。

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-http</artifactId>
</dependency>
```

## 普通 JSON 请求

```java
try (HttpClient client = HttpClient.builder()
        .baseUrl("https://api.example.com/")
        .bearerToken(token)
        .jsonCodec(JsonCodec.defaultCodec())
        .build()) {
    ChatResponse result = client.sendJson(
            HttpRequest.post("/v1/chat").build(),
            requestBody,
            ChatResponse.class);
}
```

`send` 完整缓冲响应，默认上限 16 MiB，即使服务端省略或伪造 `Content-Length` 也会执行实际读取限制。4xx/5xx 默认作为普通响应返回；只有显式调用
`requireSuccess()` 才抛出 `HttpStatusException`。请求可以使用绝对 URL，也可以相对于客户端 `baseUrl`。

客户端默认 header、bearer token 和 JSON 编解码器都可以被单个请求覆盖。请求体也可以使用 `HttpBodyUtils.bytes`、`text`、
`form`、`file`、`stream` 和 `multipart`。

## 流式响应

```java
try (HttpStream response = client.openStream(request);
     InputStream body = response.body()) {
    body.transferTo(destination);
}
```

流式响应的 body 只能获取一次；关闭响应会释放底层 call。客户端关闭时会取消本实例创建且仍活跃的调用。

单次调用的超时和缓冲上限使用 `RequestOptions` 覆盖客户端配置：

```java
HttpResponse response = client.send(request, RequestOptions.builder()
        .callTimeout(Duration.ofSeconds(10))
        .maxBufferedResponseBytes(4 * 1024 * 1024)
        .build());
```

## SSE

```java
SseSession session = client.openSse(request, new SseListener() {
    @Override
    public void onEvent(SseSession current, SseEvent event) {
        consume(event.data());
    }

    @Override
    public void onTerminal(SseSession current, SseTerminal terminal) {
        record(terminal.termination());
    }
});
```

SSE 回调运行在传输回调线程，监听器应快速返回。终态 `CANCELLED`、`REMOTE_CLOSED`、`FAILED` 恰好通知一次；`session.close()`
等价于取消。SSE 默认没有总调用时限，默认空闲超时为 2 分钟，可用 `SseOptions` 覆盖或设为零禁用。不会自动重连，需要恢复时显式再次调用
`openSse` 并可传递 `lastEventId`。

## 资源所有权

| 创建方式                               | `close()` 的责任                                       |
|----------------------------------------|--------------------------------------------------------|
| `HttpClient.builder().build()`         | owned：取消本实例调用并关闭 dispatcher、连接池和 cache |
| `OkHttpInterop.borrowed(okHttpClient)` | borrowed：只取消本包装器创建的调用，不关闭共享资源     |
| `OkHttpInterop.owned(okHttpClient)`    | owned：接管外部客户端资源的关闭责任                    |

关闭后的同步、流式和 SSE 请求都会被拒绝。拦截器、Cookie、代理、TLS 和原生 OkHttp body 只应通过 `OkHttpInterop` 这个高级
escape hatch 接入。

新代码统一使用 `send`、`sendJson`、`openStream` 和 `openSse`；SSE 事件、失败和终态使用简洁命名的 `Sse*` 类型。

## 失败与安全

`HttpException.getKind()` 区分 DNS、连接、TLS、超时、取消、协议、IO 和响应过大。异常 URL、metadata、header 和 `toString()`
会脱敏认证头、Cookie 及常见 token/secret 参数；原始传输异常不会作为 cause 保留。响应正文、SSE failure body
和不常见参数仍可能包含秘密，不能直接写日志。
