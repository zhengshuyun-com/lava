# HTTP 客户端教程

`lava-http` 提供统一同步 HTTP 调用 API, 底层基于 OkHttp, 内置请求构建, 响应解析, 超时和代理配置能力.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-http`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-http</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;

public class HttpQuickStartDemo {

    public static void main(String[] args) {
        // 1) 构建 GET 请求
        HttpRequest request = HttpRequest.get("https://httpbin.org/get")
                .setUserAgentBrowser()
                .setHeader("Accept", "application/json")
                .build();

        // 2) 执行请求并及时关闭响应
        try (HttpResponse response = request.execute()) {
            int statusCode = response.getCode();
            String body = response.getBodyAsString();

            // TODO: 按业务处理 statusCode/body
        }
    }
}
```

- `HttpRequest.get(...)`: 创建请求构建器.
- `HttpRequest.execute()`: 实例入口, 内部使用全局单例 `HttpClient` 执行请求.
- `HttpUtil.execute(...)`: 静态入口, 与 `request.execute()` 等价.
- `HttpResponse` 必须关闭, 推荐固定用 `try-with-resources`.

## 请求构建

### JSON POST

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;
import com.zhengshuyun.lava.http.HttpUtil;

HttpRequest request = HttpRequest.post("https://api.example.com/orders")
        .setBearerToken(token)
        .setJsonBody("{\"sku\":\"A100\",\"count\":1}")
        .build();

try (HttpResponse response = HttpUtil.execute(request)) {
    String body = response.getBodyAsString();
    // TODO: 按业务处理 body
}
```

- `setBearerToken(...)`: 自动写入 `Authorization: Bearer ...`.
- `setJsonBody(...)`: 自动使用 `application/json`.

### Multipart 上传

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;
import com.zhengshuyun.lava.http.HttpUtil;

import java.nio.file.Path;

HttpRequest.MultipartBuilder multipart = HttpRequest.MultipartBuilder.builder()
        .addFormField("bizType", "avatar")
        .addFile("file", Path.of("/tmp/avatar.png"), "image/png");

HttpRequest request = HttpRequest.post("https://upload.example.com/files")
        .setMultipartBody(multipart)
        .build();

try (HttpResponse response = HttpUtil.execute(request)) {
    int statusCode = response.getCode();
    // TODO: 按业务处理 statusCode
}
```

- `addFile(...)` 会校验文件是否存在.
- 上传大文件时建议通过网关和服务端同步配置超时.

## 客户端与超时配置

```java
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.http.HttpUtil;

import java.time.Duration;

// 1) 在应用启动阶段初始化全局客户端(只能初始化一次)
HttpClient client = HttpUtil.httpClientBuilder()
        .setConnectTimeout(Duration.ofSeconds(3))
        .setReadTimeout(Duration.ofSeconds(10))
        .setWriteTimeout(Duration.ofSeconds(10))
        .setCallTimeout(Duration.ofSeconds(20))
        .build();
HttpUtil.initHttpClient(client);
```

- `callTimeout` 建议始终设置, 避免请求长时间挂起.
- `initHttpClient(...)` 只能调用一次, 需要在首次请求前完成.

## 生命周期管理与高级用法

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;
import com.zhengshuyun.lava.http.HttpUtil;

import java.io.InputStream;

HttpRequest request = HttpRequest.get("https://download.example.com/report.csv").build();

try (HttpResponse response = HttpUtil.execute(request)) {
    // 1) 大响应优先流式读取
    InputStream stream = response.getBodyAsStream();

    // 2) 读取元信息
    long contentLength = response.getContentLength();
    String traceId = response.getHeader("X-Trace-Id");

    // TODO: 流式消费 stream, 并处理 contentLength/traceId
}
```

- `getBodyAsStream()` 是一次性流, 重复调用会抛异常.
- 需要多次读取内容时, 先调用 `getBodyAsBytes()` 缓存.

## 常见坑与排查建议

- 仅关闭 `InputStream` 不会释放底层连接, 必须关闭 `HttpResponse`.
- `GET/HEAD` 请求即使设置 body 也会被框架忽略, 属于协议语义限制.
- `HttpUtil.initHttpClient(...)` 重复调用会抛 `IllegalArgumentException`.
- 代理鉴权失败时优先检查用户名密码和代理返回的 `407` 响应码.

## 实践建议

- 统一通过 `HttpRequest` 组装请求, 禁止字符串拼接 Header.
- 在网关调用场景开启请求追踪头, 并结合 `response.getMetadata()` 落日志.
- token, cookie, 代理密码不要打印日志, 生产环境建议使用配置中心或密文注入.
