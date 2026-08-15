# HttpResponse 响应读取

`HttpResponse` 封装 HTTP 响应状态码, Header, Body 和调用元信息.

## 最小可运行示例

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;

public class HttpResponseDemo {

    public static void main(String[] args) {
        try (HttpResponse response = HttpRequest.get("https://httpbin.org/get").build().execute()) {
            int code = response.getCode();
            String contentType = response.getHeader("Content-Type");
            String body = response.getBodyAsString();

            // TODO: 按业务处理 code/contentType/body
        }
    }
}
```

- `getCode()`: 获取 HTTP 状态码.
- `getHeader(...)`: 读取响应 Header.
- `getBodyAsString()`: 读取字符串响应体.
- `getBodyAsBytes()`: 读取字节数组响应体.
- `getBodyAsStream()`: 获取响应体输入流.

## 大响应体

```java
try (HttpResponse response = HttpRequest.get("https://download.example.com/report.csv").build().execute()) {
    java.io.InputStream stream = response.getBodyAsStream();
    long contentLength = response.getContentLength();

    // TODO: 流式消费 stream/contentLength
}
```

- `getBodyAsStream()` 是一次性流.
- 如果需要多次读取内容, 先调用 `getBodyAsBytes()` 缓存.

## 常见坑与排查建议

| 异常/消息              | 原因                           | 解决方式                              |
|------------------------|--------------------------------|---------------------------------------|
| 连接未释放             | 没有关闭 `HttpResponse`        | 使用 `try-with-resources`             |
| 流重复读取失败         | `getBodyAsStream()` 是一次性流 | 需要重复读取时改用 `getBodyAsBytes()` |
| 只关闭了 `InputStream` | 底层响应对象仍未关闭           | 必须关闭 `HttpResponse`               |
