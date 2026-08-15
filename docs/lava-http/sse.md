# SSE 事件流

`lava-http` 支持通过 `executeSse(...)` 建立 SSE 连接, 适合服务端事件推送场景.

## 最小可运行示例

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpSseEvent;
import com.zhengshuyun.lava.http.HttpSseListener;
import com.zhengshuyun.lava.http.HttpSseSession;

public class SseDemo {

    public static void main(String[] args) {
        HttpSseSession session = HttpRequest.get("https://api.example.com/events")
                .build()
                .executeSse(new HttpSseListener() {
                    @Override
                    public void onEvent(HttpSseSession session, HttpSseEvent event) {
                        // TODO: 按业务处理 event
                    }
                });

        // TODO: 在业务生命周期结束时关闭 session
    }
}
```

- `executeSse(...)`: 建立 SSE 连接.
- `HttpSseListener`: 监听打开, 事件, 关闭和失败.
- `HttpSseSession`: 表示当前 SSE 会话.

## 常见坑与排查建议

| 异常/消息      | 原因                          | 解决方式                             |
|----------------|-------------------------------|--------------------------------------|
| 连接长期占用   | SSE 是长连接                  | 按业务生命周期关闭 `HttpSseSession`  |
| 收不到事件     | 服务端不是 SSE 格式或网关缓冲 | 检查响应头和网关流式转发配置         |
| 失败后没有处理 | 未实现 `onFailure(...)`       | 在监听器中记录脱敏日志并触发重连策略 |
