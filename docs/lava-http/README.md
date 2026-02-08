# lava-http

`lava-http` 提供统一的 HTTP 客户端 API，底层基于 OkHttp 实现。支持请求构建、响应解析、超时管理等常见 HTTP 操作。

## 引入依赖

如果你已经通过 BOM 管理版本，只需引入 `lava-http`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-http</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.http.HttpRequest;

public class HttpQuickStartDemo {

    public static void main(String[] args) {
        // 1) 构建请求
        String response = HttpClient.get("https://api.example.com/users")
                .header("Authorization", "Bearer token")
                .timeout(5000)
                .execute()
                .asString();

        // TODO: 处理响应结果
    }
}
```

## 常见操作

### GET 请求

```java
String result = HttpClient.get(url)
        .timeout(5000)
        .execute()
        .asString();
```

### POST 请求

```java
String result = HttpClient.post(url)
        .body(jsonData)
        .contentType("application/json")
        .execute()
        .asString();
```

### 自定义请求

通过 `HttpRequest` 构建复杂请求。

```java
HttpRequest request = HttpClient.request()
        .method("PUT")
        .url(url)
        .header("X-Custom-Header", "value")
        .timeout(10000)
        .execute();
```

## 常见坑与排查建议

- 超时设置过短可能导致正常请求失败，建议根据网络状况调整。
- 响应体大于内存时，应使用流式处理而非 `asString()`.
- 代理配置时务必确保代理地址和端口正确。

## 生产环境建议

- 配置合理的连接超时和读写超时。
- 对核心接口增加重试机制。
- 监控 HTTP 错误率和响应时间。
