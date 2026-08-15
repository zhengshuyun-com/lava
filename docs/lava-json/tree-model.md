# Tree 模型

Tree 模型适合动态 JSON, 局部字段读取和临时结构组装.

## 最小可运行示例

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhengshuyun.lava.json.JsonUtil;

public class TreeModelDemo {

    public static void main(String[] args) {
        ObjectNode root = JsonUtil.createObjectNode();
        root.put("traceId", "req-20260208");
        root.put("success", true);

        JsonNode node = JsonUtil.readTree(JsonUtil.writeValueAsString(root));
        String traceId = node.path("traceId").asText();

        // TODO: 按业务处理 traceId
    }
}
```

- `createObjectNode()`: 创建 JSON 对象节点.
- `createArrayNode()`: 创建 JSON 数组节点.
- `readTree(...)`: 从字符串, 字节数组, 文件或输入流读取 Tree.
- `valueToTree(...)`: 把 Java 对象转换成 `JsonNode`.

## 适用场景

| 场景                 | 建议                                 |
|----------------------|--------------------------------------|
| 字段结构稳定         | 优先定义具名类或 `record`            |
| 只读取少量字段       | 可以使用 `readTree(...)`             |
| 动态字段较多         | 可以使用 `ObjectNode` 或 `ArrayNode` |
| 需要继续转成业务对象 | 使用 `convertValue(...)`             |

## 常见坑与排查建议

| 异常/消息          | 原因                         | 解决方式                                |
|--------------------|------------------------------|-----------------------------------------|
| 读取字段为空       | 字段不存在或路径不对         | 使用 `path(...)` 前先确认原始 JSON 结构 |
| 业务代码到处读字段 | JSON 结构已经稳定但仍用 Tree | 改成具名类或 `record` 承接              |
| `JsonException`    | JSON 内容非法                | 打印脱敏后的原始内容排查                |
