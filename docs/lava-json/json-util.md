# JsonUtil 编解码

`JsonUtil` 是 `lava-json` 的统一编解码入口, 用于对象, 集合, 文件, 字节数组和输入流之间的 JSON 转换.

## 最小可运行示例

```java
import com.zhengshuyun.lava.json.JsonUtil;
import tools.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.List;

public class JsonUtilDemo {

    public static void main(String[] args) {
        Order order = new Order(1001L, "paid", Instant.parse("2026-02-08T10:00:00Z"));

        String json = JsonUtil.writeValueAsString(order);
        Order parsed = JsonUtil.readValue(json, Order.class);
        List<Order> list = JsonUtil.readValue("[" + json + "]", new TypeReference<List<Order>>() {
        });

        // TODO: 按业务处理 json/parsed/list
    }

    public record Order(long id, String status, Instant createdAt) {
    }
}
```

- `writeValueAsString(...)`: 序列化为 JSON 字符串.
- `writeValueAsPrettyString(...)`: 序列化为格式化 JSON 字符串.
- `writeValueAsBytes(...)`: 序列化为字节数组.
- `readValue(..., TypeReference<T>)`: 处理泛型集合, 避免类型擦除.

## 输入输出类型

```java
Order fromString = JsonUtil.readValue(json, Order.class);
Order fromBytes = JsonUtil.readValue(bytes, Order.class);
Order fromFile = JsonUtil.readValue(file, Order.class);
Order fromStream = JsonUtil.readValue(inputStream, Order.class);
```

| 输入          | 适用场景                 |
|---------------|--------------------------|
| `String`      | 普通接口报文和配置文本   |
| `byte[]`      | 缓存, MQ, RPC 等字节数据 |
| `File`        | 本地 JSON 文件           |
| `InputStream` | 大响应体或流式读取       |

## 类型转换

```java
Order order = JsonUtil.convertValue(payloadMap, Order.class);
byte[] bytes = JsonUtil.writeValueAsBytes(order);
```

- `convertValue(...)`: 适合 DTO, Map 和领域对象之间转换.
- 结构稳定时优先用具名类或 `record`, 不建议在业务代码里散落字段读取逻辑.

## 常见坑与排查建议

| 异常/消息                | 原因                             | 解决方式                               |
|--------------------------|----------------------------------|----------------------------------------|
| `JsonException`          | 底层读写或类型转换失败           | 查看 `cause`, 核对 JSON 字段和目标类型 |
| 泛型集合反序列化类型不对 | 使用了 `List.class` 之类的裸类型 | 使用 `TypeReference<List<T>>`          |
| 反序列化失败             | 字段类型不匹配或 JSON 格式非法   | 先记录原始 JSON, 再核对目标类型        |
