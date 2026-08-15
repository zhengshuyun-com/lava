# HTTP 客户端概览

`lava-http` 提供统一同步 HTTP 调用能力, 覆盖请求构建, 响应读取, 客户端配置, Multipart 上传和 SSE 事件流.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-http`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-http</artifactId>
</dependency>
```

## 模块能力

| 能力           | 入口                           | 文档                                             |
|----------------|--------------------------------|--------------------------------------------------|
| 请求构建       | `HttpRequest`                  | [HttpRequest 请求构建](./http-request.md)        |
| 响应读取       | `HttpResponse`                 | [HttpResponse 响应读取](./http-response.md)      |
| 客户端配置     | `HttpClient`                   | [HttpClient 客户端配置](./http-client-config.md) |
| Multipart 上传 | `HttpRequest.MultipartBuilder` | [Multipart 上传](./multipart-upload.md)          |
| SSE 事件流     | `HttpSseListener`              | [SSE 事件流](./sse.md)                           |

## 快速示例

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;

public class HttpQuickStartDemo {

    public static void main(String[] args) {
        HttpRequest request = HttpRequest.get("https://httpbin.org/get")
                .setUserAgentBrowser()
                .setHeader("Accept", "application/json")
                .build();

        try (HttpResponse response = request.execute()) {
            int statusCode = response.getCode();
            String body = response.getBodyAsString();

            // TODO: 按业务处理 statusCode/body
        }
    }
}
```

## 使用建议

- `HttpResponse` 必须关闭, 推荐固定使用 `try-with-resources`.
- 对外部接口调用建议统一设置 `callTimeout`, 避免请求长期挂起.
- token, cookie, 代理密码不要打印日志, 生产环境建议使用配置中心或密文注入.
