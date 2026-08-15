# HttpClient 客户端配置

`HttpClient` 用于配置超时, 代理, Cookie, 拦截器和连接池. 全局客户端可通过 `HttpUtil.initHttpClient(...)` 初始化一次.

## 最小可运行示例

```java
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;
import com.zhengshuyun.lava.http.HttpUtil;

import java.time.Duration;

public class HttpClientConfigDemo {

    public static void main(String[] args) {
        HttpClient client = HttpUtil.httpClientBuilder()
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(10))
                .setWriteTimeout(Duration.ofSeconds(10))
                .setCallTimeout(Duration.ofSeconds(20))
                .build();
        HttpUtil.initHttpClient(client);

        try (HttpResponse response = HttpRequest.get("https://api.example.com/health").build().execute()) {
            // TODO: 按业务处理 response
        }
    }
}
```

- `initHttpClient(...)` 只能调用一次.
- 方法级临时配置可以通过 `request.execute(config)` 覆盖本次调用.
- `callTimeout` 建议始终设置.

## 单次请求覆盖配置

```java
HttpClient.Builder requestConfig = HttpClient.builder()
        .setReadTimeout(Duration.ofSeconds(30))
        .setCallTimeout(Duration.ofSeconds(30));

try (HttpResponse response = HttpRequest.get("https://api.example.com/slow-task")
        .build()
        .execute(requestConfig)) {
    // TODO: 按业务处理 response
}
```

## 常见坑与排查建议

| 异常/消息      | 原因                                      | 解决方式                              |
|----------------|-------------------------------------------|---------------------------------------|
| 重复初始化失败 | `HttpUtil.initHttpClient(...)` 调用了多次 | 在应用启动阶段集中初始化一次          |
| 请求长期挂起   | 未配置 `callTimeout`                      | 给全局或单次请求设置 `callTimeout`    |
| 代理鉴权失败   | 代理用户名密码或配置错误                  | 检查代理返回的 `407` 响应码和代理配置 |
