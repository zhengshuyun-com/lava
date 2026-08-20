# lava-ai

`lava-ai` 是建立在 `lava-http` 之上的协议中立 AI 请求便利层。它不绑定 OpenAI、Anthropic 或其他供应商字段，只负责默认地址、鉴权、JSON
body 和可插拔 SSE chunk decoder。

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-ai</artifactId>
</dependency>
```

```java
try (AiClient ai = AiClient.builder()
        .baseUrl("https://api.example.com/")
        .bearerToken(token)
        .build()) {
    ChatResponse response = ai.sendJson(
            HttpRequest.post("/v1/chat").build(), requestBody, ChatResponse.class);

    ai.openJsonStream(HttpRequest.post("/v1/chat").build(), requestBody,
            event -> event.data().equals("[DONE]")
                    ? Optional.empty() : Optional.of(event.data()),
            new AiStreamListener<>() {
                @Override
                public void onChunk(SseSession session, String chunk) {
                    consume(chunk);
                }
            });
}
```

默认不重试、不自动重连；长时间 SSE 使用空闲超时而不是总调用超时。需要供应商特定字段时，应在 `AiChunkDecoder`
中解析，而不是让通用客户端依赖供应商模型。
