# 快速开始

`Lava` 是一组 Java 基础设施工具库. 新项目建议先用 `lava-bom` 统一版本, 再按场景引入需要的模块.

## 1. 导入 BOM

在父 `pom.xml` 的 `dependencyManagement` 中导入 `lava-bom`.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zhengshuyun</groupId>
            <artifactId>lava-bom</artifactId>
            <version>${lava.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

导入 BOM 后, 业务模块中的 `lava-*` 依赖通常不再单独写版本.

## 2. 按需引入模块

```xml
<dependencies>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-json</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zhengshuyun</groupId>
        <artifactId>lava-http</artifactId>
    </dependency>
</dependencies>
```

常见选择:

| 场景                         | 模块            |
|------------------------------|-----------------|
| 基础工具, 重试, 时间, ID, IO | `lava-core`     |
| JSON 编解码                  | `lava-json`     |
| HTTP 调用                    | `lava-http`     |
| 密码哈希和密钥处理           | `lava-crypto`   |
| JWT 签发与验签               | `lava-jwt`      |
| 后台任务调度                 | `lava-schedule` |
| 邮件收发                     | `lava-mail`     |

## 3. 跑通最小示例

```java
import com.zhengshuyun.lava.core.retry.RetryUtil;
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;
import com.zhengshuyun.lava.json.JsonUtil;

import java.util.Map;

public class LavaQuickStartDemo {

    public static void main(String[] args) {
        String json = JsonUtil.writeValueAsString(Map.of("source", "lava"));

        String body = RetryUtil.retrier()
                .setMaxAttempts(3)
                .setFixedDelayMillis(200)
                .build()
                .execute(() -> {
                    try (HttpResponse response = HttpRequest.post("https://httpbin.org/post")
                            .setJsonBody(json)
                            .build()
                            .execute()) {
                        return response.getBodyAsString();
                    }
                });

        // TODO: 按业务处理 body
    }
}
```

- `JsonUtil`: 统一 JSON 编解码入口.
- `RetryUtil`: 给短暂失败的任务增加重试能力.
- `HttpRequest`: 构建并执行 HTTP 请求.
- `HttpResponse`: 必须关闭, 推荐使用 `try-with-resources`.

## 4. 下一步

| 目标                 | 文档                                            |
|----------------------|-------------------------------------------------|
| 统一依赖版本         | [lava-bom 概览](./lava-bom/README.md)           |
| 使用基础工具         | [lava-core 概览](./lava-core/README.md)         |
| 处理 JSON            | [lava-json 概览](./lava-json/README.md)         |
| 调用外部接口         | [lava-http 概览](./lava-http/README.md)         |
| 做密码哈希和密钥处理 | [lava-crypto 概览](./lava-crypto/README.md)     |
| 签发和验证 JWT       | [lava-jwt 概览](./lava-jwt/README.md)           |
| 创建后台任务         | [lava-schedule 概览](./lava-schedule/README.md) |
| 收发邮件             | [lava-mail 概览](./lava-mail/README.md)         |
