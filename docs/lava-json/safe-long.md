# SafeLong 长整型安全序列化

`SafeLongModule` 用于把超出 JavaScript 安全整数范围的 `long` 和 `Long` 序列化为字符串, 避免前端数字精度丢失.

## 最小可运行示例

```java
import com.zhengshuyun.lava.json.JsonUtil;
import com.zhengshuyun.lava.json.SafeLongModule;
import tools.jackson.databind.ObjectMapper;

public class SafeLongDemo {

    public static void main(String[] args) {
        ObjectMapper mapper = JsonUtil.builder()
                .setCustomizer(builder -> builder.addModule(new SafeLongModule()))
                .build();

        JsonUtil.initObjectMapper(mapper);

        String json = JsonUtil.writeValueAsString(new User(9223372036854775807L));

        // TODO: 按业务处理 json
    }

    public record User(long id) {
    }
}
```

- 默认不启用长整型安全序列化.
- 启用后只有超出安全范围的值输出为字符串, 范围内的值仍然输出为数字.
- 安全范围为 `-(2^53 - 1)` 到 `2^53 - 1`, 即 `-9007199254740991` 到 `9007199254740991`.
- 适合对外接口返回雪花 ID, 数据库主键等长整型字段.

| 示例值                | 输出                    |
|-----------------------|-------------------------|
| `42`                  | `42`                    |
| `9007199254740991`    | `9007199254740991`      |
| `9223372036854775807` | `"9223372036854775807"` |

## 常见坑与排查建议

| 异常/消息        | 原因                               | 解决方式                                    |
|------------------|------------------------------------|---------------------------------------------|
| 前端 ID 精度丢失 | 长整型超过 JavaScript 安全整数范围 | 启用 `SafeLongModule` 或 DTO 字段改为字符串 |
| 调用方类型不兼容 | 原本期望数字, 现在收到字符串       | 作为接口变更处理, 提前通知调用方            |
| 模块未生效       | `JsonUtil` 已经初始化              | 在首次 JSON 读写前完成初始化                |
