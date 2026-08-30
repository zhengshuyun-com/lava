# 流式响应

大文件或不应进入内存的响应使用 `openStream(...)`：

```java
try (HttpStream response = client.openStream(request)) {
    if (!response.isSuccessful()) {
        throw new IllegalStateException(
                "下载失败，status=" + response.statusCode()
        );
    }

    try (InputStream body = response.body()) {
        body.transferTo(destination);
    }
}
```

## 资源规则

- `HttpStream` 必须关闭；
- `body()` 只能获取一次；
- 关闭响应会释放底层调用；
- 关闭 `HttpClient` 会取消该客户端创建且仍活跃的调用；
- 流式响应不受缓冲响应正文上限约束，调用方应自行限制下载大小。

单次流式调用同样支持 `RequestOptions`：

```java
HttpStream response = client.openStream(
        request,
        RequestOptions.builder()
                .callTimeout(Duration.ofMinutes(5))
                .build()
);
```

建议先写临时文件，完整校验长度、摘要或业务格式后再原子发布目标文件，避免失败时留下看似完整的半文件。
