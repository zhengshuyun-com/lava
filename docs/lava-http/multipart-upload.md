# Multipart 上传

`HttpRequest.MultipartBuilder` 用于构建 `multipart/form-data` 请求体, 适合上传文件和普通表单字段.

## 最小可运行示例

```java
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;

import java.nio.file.Path;

public class MultipartUploadDemo {

    public static void main(String[] args) {
        HttpRequest.MultipartBuilder multipart = HttpRequest.MultipartBuilder.builder()
                .addFormField("bizType", "avatar")
                .addFile("file", Path.of("/tmp/avatar.png"), "image/png");

        HttpRequest request = HttpRequest.post("https://upload.example.com/files")
                .setMultipartBody(multipart)
                .build();

        try (HttpResponse response = request.execute()) {
            int statusCode = response.getCode();
            // TODO: 按业务处理 statusCode
        }
    }
}
```

- `addFormField(...)`: 添加普通表单字段.
- `addFile(...)`: 添加文件字段.
- `setMultipartBody(...)`: 设置 Multipart 请求体.

## 常见坑与排查建议

| 异常/消息        | 原因                            | 解决方式                         |
|------------------|---------------------------------|----------------------------------|
| 文件不存在       | `addFile(...)` 指向不存在的路径 | 上传前检查本地文件路径           |
| 大文件上传超时   | 客户端或网关超时太短            | 同步调整客户端, 网关和服务端超时 |
| 服务端收不到字段 | 字段名和服务端约定不一致        | 核对 multipart 字段名            |
