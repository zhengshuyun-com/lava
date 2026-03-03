# JSON 编解码教程

`lava-json` 提供统一 JSON 序列化与反序列化 API, 底层基于 Jackson, 默认输出 UTC 时间格式.

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
import java.util.Map;

public class JsonQuickStartDemo {

    public static void main(String[] args) {
        // 1. 准备业务对象
        User user = new User(1001L, "alice", Instant.parse("2026-02-08T10:00:00Z"), Map.of("source", "api"));

        // 2. 序列化为 JSON 字符串
        String json = JsonUtil.writeValueAsString(user);

        // 3. 反序列化为对象
        User parsed = JsonUtil.readValue(json, User.class);

        // 4. 反序列化为泛型集合
        List<User> users = JsonUtil.readValue("[" + json + "]", new TypeReference<List<User>>() {
        });

        // TODO: 按业务处理 json/parsed/users
    }

    public record User(long id, String name, Instant createdAt, Map<String, String> ext) {
    }
}
```

- `writeValueAsString(...)`: 使用模块默认 `ObjectMapper` 完成序列化.
- `readValue(..., TypeReference<T>)`: 处理泛型集合, 避免类型擦除.
- `createdAt` 默认输出 `ISO 8601 + Z` UTC 格式.

## 核心能力

### 对象与集合编解码

```java
// 1. 对象反序列化
Order order = JsonUtil.readValue(jsonStr, Order.class);

// 2. 集合反序列化
List<Order> list = JsonUtil.readValue(jsonArr, new TypeReference<List<Order>>() {
});

// 3. 序列化为字节数组
byte[] bytes = JsonUtil.writeValueAsBytes(order);
```

- `readValue(...)` 支持 `String`/`byte[]`/`File`/`InputStream` 多种输入.
- `writeValueAsBytes(...)` 适合 RPC, 消息队列, 缓存等场景.

### Tree 模型处理

```java
// 1. 动态构建 JSON
ObjectNode root = JsonUtil.createObjectNode();
root.put("traceId", "req-20260208");
root.put("success", true);

// 2. 按需读取局部字段
JsonNode tree = JsonUtil.readTree(JsonUtil.writeValueAsString(root));
String traceId = tree.path("traceId").asText();
```

- `createObjectNode()`/`createArrayNode()` 适合动态字段.
- `readTree(...)` 适合只关心部分字段, 不需要完整 Java 类型.

### 类型转换

```java
// 1. Map 转对象
User user = JsonUtil.convertValue(payloadMap, User.class);

// 2. 对象转 JsonNode
JsonNode node = JsonUtil.valueToTree(user);
```

- `convertValue(...)` 适合 DTO 与领域对象之间转换.
- `valueToTree(...)` 适合在规则引擎或网关中做轻量结构处理.

## 时间格式规范

框架层统一输出 UTC 时间, 业务层或前端按用户时区做展示转换.

| 类型 | 默认输出示例 | 说明 |
|------|--------------|------|
| `LocalDateTime` | `2026-01-01T12:30:00Z` | 无时区类型, 框架统一补 `Z` 以保持格式一致 |
| `Date` | `2026-01-01T12:30:00Z` | 按 UTC 格式输出 |
| `Instant` | `2026-01-01T12:30:00Z` | 按 UTC 格式输出 |
| `LocalDate` | `2026-01-01` | 日期类型 |
| `LocalTime` | `12:30:00` | 时间类型 |

## 生命周期管理与自定义配置

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhengshuyun.lava.core.time.DateTimePatterns;
import com.zhengshuyun.lava.core.time.ZoneIds;
import com.zhengshuyun.lava.json.JsonBuilder;
import com.zhengshuyun.lava.json.JsonUtil;
import com.zhengshuyun.lava.json.SafeLongModule;

import java.util.Locale;

// 1. 构建自定义 ObjectMapper
ObjectMapper mapper = new JsonBuilder()
        .setDateTimeFormat(DateTimePatterns.DATE_TIME)
        .setZone(ZoneIds.ASIA_SHANGHAI)
        .setLocale(Locale.CHINA)
        .setCustomizer(builder -> builder.addModule(new SafeLongModule()))
        .build();

// 2. 在首次使用 JsonUtil 前初始化
JsonUtil.initObjectMapper(mapper);
```

- `initObjectMapper(...)` 只能调用一次, 且必须在首次读写前执行.
- 默认不启用长整型安全序列化, 需要时可通过 `SafeLongModule` 手动开启.
- 修改时间格式会影响 API 兼容性, 建议团队统一约定后再调整.

## 常见坑与排查建议

- 使用了不存在的方法名 (如 `parseObject`) 时, 统一改为 `readValue(...)`.
- 首次读写后再调用 `initObjectMapper(...)` 会抛异常, 请在应用启动阶段初始化.
- 反序列化失败多数是字段类型不匹配, 先记录原始 JSON 再核对目标类型.
- 超大响应体优先使用 `InputStream` 版本, 降低内存峰值.

## 实践建议

- API 边界统一使用 `JsonUtil`, 避免多套 `ObjectMapper` 配置漂移.
- 生产环境建议统一 UTC 入库, 展示层再做时区转换.
