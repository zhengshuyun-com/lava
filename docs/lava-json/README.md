# JSON 编解码概览

`lava-json` 提供统一 JSON 序列化, 反序列化, Tree 模型, 类型转换和 JSON 配置能力.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-json`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-json</artifactId>
</dependency>
```

## 模块能力

| 能力             | 入口             | 文档                                        |
|------------------|------------------|---------------------------------------------|
| JSON 编解码      | `JsonUtil`       | [JsonUtil 编解码](./json-util.md)           |
| Tree 模型        | `JsonUtil`       | [Tree 模型](./tree-model.md)                |
| 自定义配置       | `JsonBuilder`    | [JsonBuilder 自定义配置](./json-builder.md) |
| 时间格式         | `JsonBuilder`    | [时间格式规范](./time-format.md)            |
| 长整型安全序列化 | `SafeLongModule` | [SafeLong 长整型安全序列化](./safe-long.md) |

## 快速示例

```java
import com.zhengshuyun.lava.json.JsonUtil;
import tools.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class JsonQuickStartDemo {

    public static void main(String[] args) {
        // 1. 准备业务对象
        User user = new User(1001L, "alice", Instant.parse("2026-02-08T10:00:00Z"), Map.of("source", "api"));

        // 2. 序列化为 JSON 字符串
        String json = JsonUtil.writeValueAsString(user);

        // 3. 反序列化为泛型集合
        List<User> users = JsonUtil.readValue("[" + json + "]", new TypeReference<List<User>>() {
        });

        // TODO: 按业务处理 json/users
    }

    public record User(long id, String name, Instant createdAt, Map<String, String> ext) {
    }
}
```

## 使用建议

- API 边界统一使用 `JsonUtil`, 避免多套 `ObjectMapper` 配置漂移.
- 如果需要调整默认时间格式或扩展 Jackson 模块, 在应用启动阶段初始化一次.
- 表示时刻用 `Instant`, 默认输出 ISO-8601 UTC, 展示层再按用户时区转换.
