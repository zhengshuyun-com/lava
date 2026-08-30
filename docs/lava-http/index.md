# lava-http

`lava-http` 是线程安全的 HTTP、流式响应和 SSE 客户端。常规 API 只暴露 Lava 与 JDK 类型，高级 OkHttp 定制集中在 `OkHttpInterop`。

## 添加依赖

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-http</artifactId>
</dependency>
```

版本推荐由 [lava-bom](../lava-bom/) 管理。

## 能力概览

| 能力 | 入口 | 资源要求 |
| --- | --- | --- |
| 缓冲请求 | `send(...)`、`sendJson(...)` | 响应无需关闭 |
| 流式响应 | `openStream(...)` | 必须关闭 `HttpStream` |
| SSE | `openSse(...)` | 终止或关闭 `SseSession` |
| URL 构建 | `HttpUrlBuilder` | 可变构建器，不保证线程安全 |
| 高级传输 | `OkHttpInterop` | 显式选择借用或接管资源 |

```java
try (HttpClient client = HttpClient.builder()
        .baseUrl("https://api.example.com/")
        .bearerToken(token)
        .build()) {
    HttpResponse response = client.send(
            HttpRequest.get("/v1/status").build()
    );
}
```

从 [请求与响应](./requests) 开始，按需查看 [请求体与 URL](./body-url)、[流式响应](./streaming) 或 [SSE](./sse)。
