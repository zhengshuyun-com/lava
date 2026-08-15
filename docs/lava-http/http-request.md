# HttpRequest 请求构建

`HttpRequest` 用于构建 HTTP 请求, 支持常见方法, Header, Bearer Token, JSON Body 和自定义请求体.

## 最小可运行示例

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;

public class HttpRequestDemo {

    public static void main(String[] args) {
        HttpRequest request = HttpRequest.post("https://api.example.com/orders")
                .setBearerToken("your-token")
                .setHeader("Accept", "application/json")
                .setJsonBody("{\"sku\":\"A100\",\"count\":1}")
                .build();

        try (HttpResponse response = request.execute()) {
            String body = response.getBodyAsString();
            // TODO: 按业务处理 body
        }
    }
}
```

- `HttpRequest.get(...)`: 构建 GET 请求.
- `HttpRequest.post(...)`: 构建 POST 请求.
- `setBearerToken(...)`: 设置 `Authorization: Bearer ...`.
- `setJsonBody(...)`: 设置 JSON 请求体.

## 常用方法

| 方法          | 说明             |
|---------------|------------------|
| `get(...)`    | 构建 GET 请求    |
| `post(...)`   | 构建 POST 请求   |
| `put(...)`    | 构建 PUT 请求    |
| `patch(...)`  | 构建 PATCH 请求  |
| `delete(...)` | 构建 DELETE 请求 |
| `head(...)`   | 构建 HEAD 请求   |

## 常见坑与排查建议

| 异常/消息            | 原因                          | 解决方式                              |
|----------------------|-------------------------------|---------------------------------------|
| GET 请求 body 没生效 | `GET/HEAD` 请求会忽略 body    | 按协议语义改用 POST 或把参数放到 URL  |
| 鉴权失败             | Header 或 token 设置不正确    | 检查 `Authorization` 是否符合下游要求 |
| 请求参数散落拼接     | 业务代码直接拼 Header 或 body | 统一通过 `HttpRequest` 构建请求       |
