# JSON 编解码教程

`lava-json` 提供统一 JSON 序列化与反序列化 API, 底层基于 Jackson, 默认内置长整型安全序列化和 ISO 时间格式.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-json`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-json</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.zhengshuyun.lava.json.JsonUtil;

import java.time.Instant;
import java.util.List;

public class JsonQuickStartDemo {

    public static void main(String[] args) {
        // 1) 对象序列化为 JSON 字符串
        User user = new User(9007199254740993L, "alice", Instant.parse("2026-02-08T10:00:00Z"));
        String json = JsonUtil.writeValueAsString(user);

        // 2) JSON 字符串反序列化为对象
        User parsed = JsonUtil.readValue(json, User.class);

        // 3) JSON 数组反序列化为泛型集合
        List<User> users = JsonUtil.readValue("[" + json + "]", new TypeReference<List<User>>() {
        });

        // TODO: 按业务处理 json/parsed/users
    }

    public record User(long id, String name, Instant createdAt) {
    }
}
```

- `writeValueAsString(...)`: 默认使用模块内置 `ObjectMapper`.
- `readValue(..., TypeReference<T>)`: 处理泛型场景, 避免类型擦除问题.
- `id` 超过 JS 安全范围时会序列化为字符串, 减少前端精度丢失风险.

## 常用编解码操作

### 对象与集合

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.zhengshuyun.lava.json.JsonUtil;

Order order = JsonUtil.readValue(jsonStr, Order.class);
List<Order> list = JsonUtil.readValue(jsonArr, new TypeReference<List<Order>>() {
});
byte[] bytes = JsonUtil.writeValueAsBytes(order);
```

- `readValue(...)`: 统一入口, 覆盖 `String`/`byte[]`/`File`/`InputStream`.
- `writeValueAsBytes(...)`: 适合 RPC 或缓存场景.

### Tree 模型

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhengshuyun.lava.json.JsonUtil;

// 1) 创建可变 JSON 对象
ObjectNode root = JsonUtil.createObjectNode();
root.put("traceId", "req-20260208");
root.put("success", true);

// 2) 序列化与反序列化
String json = JsonUtil.writeValueAsString(root);
JsonNode tree = JsonUtil.readTree(json);
```

- `createObjectNode()` 和 `createArrayNode()` 适合动态字段场景.
- `readTree(...)` 适合只读部分字段, 不必定义完整 Java 类型.

## 自定义序列化配置

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhengshuyun.lava.core.time.DateTimePatterns;
import com.zhengshuyun.lava.core.time.ZoneIds;
import com.zhengshuyun.lava.json.JsonBuilder;
import com.zhengshuyun.lava.json.JsonUtil;

import java.util.Locale;

// 1) 构建自定义 ObjectMapper
ObjectMapper mapper = new JsonBuilder()
        .setDateTimeFormat(DateTimePatterns.DATE_TIME)
        .setZone(ZoneIds.ASIA_SHANGHAI)
        .setLocale(Locale.CHINA)
        .build();

// 2) 在首次使用 JsonUtil 前注入
JsonUtil.initObjectMapper(mapper);
```

- `initObjectMapper(...)` 只能初始化一次.
- 必须在首次调用 `JsonUtil` 任何读写方法前执行.

## 常见坑与排查建议

- 使用了不存在的方法名(如 `parseObject`), 请统一改为 `readValue`.
- `initObjectMapper(...)` 在首次读写后再调用会抛异常, 建议在应用启动阶段统一初始化.
- 反序列化失败通常是字段类型不匹配, 先输出原始 JSON 再核对目标类型.
- 超大响应体不要直接 `readValue(String, ...)`, 可优先用 `InputStream` 版本降低内存峰值.

## 实践建议

- API 边界统一使用 `JsonUtil`, 避免多套 `ObjectMapper` 配置不一致.
- 面向前端的 `long` 字段默认保留安全序列化, 除非前后端已统一 `BigInt` 方案.
- 时间字段建议统一 UTC 入库, 展示层再做时区转换.
