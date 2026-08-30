# 请求与响应

## 普通请求

```java
HttpRequest request = HttpRequest.get("/v1/users")
        .queryParam("page", "1")
        .header("X-Request-ID", requestId)
        .build();

HttpResponse response = client.send(request);
```

支持 `get`、`post`、`put`、`patch`、`delete` 和 `head` 快捷入口；其他方法使用 `HttpRequest.builder(url, method)`。

请求对象不可变，`withHeader(...)` 和 `withBody(...)` 返回新请求。

## JSON 请求

```java
ChatResponse result = client.sendJson(
        HttpRequest.post("/v1/chat").build(),
        requestBody,
        ChatResponse.class
);
```

客户端默认使用 `JsonCodec.defaultCodec()`，可以通过 `.jsonCodec(...)` 替换。泛型响应使用 `TypeReference<T>` 重载。

也可以手工构造正文：

```java
HttpRequest request = HttpRequest.post("/v1/chat")
        .jsonBody(requestBody)
        .build();

ChatResponse result = client.send(request)
        .requireSuccess()
        .bodyAs(ChatResponse.class);
```

## HTTP 状态语义

`send(...)` 会完整缓冲响应。4xx 和 5xx 默认作为普通 `HttpResponse` 返回：

```java
HttpResponse response = client.send(request);
if (!response.isSuccessful()) {
    handleStatus(response.statusCode(), response.bodyString());
}
```

需要非 2xx 直接失败时显式调用：

```java
HttpResponse response = client.send(request).requireSuccess();
```

此时抛出 `HttpStatusException`。缓冲响应正文默认上限为 16 MiB，即使服务端缺失或伪造 `Content-Length`，实际读取仍受限制。

## 单次调用选项

```java
RequestOptions options = RequestOptions.builder()
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(10))
        .callTimeout(Duration.ofSeconds(15))
        .maxBufferedResponseBytes(4 * 1024 * 1024)
        .build();

HttpResponse response = client.send(request, options);
```

未设置的选项继承客户端配置。超时为零表示不限制。
