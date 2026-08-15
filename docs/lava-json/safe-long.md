# SafeLong 长整型安全序列化

`SafeLongModule` 用于把 Java `long` 和 `Long` 序列化为字符串, 避免前端 JavaScript 数字精度丢失.

## 最小可运行示例

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhengshuyun.lava.json.JsonUtil;
import com.zhengshuyun.lava.json.SafeLongModule;

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
- 启用后 `long` 和 `Long` 会输出为 JSON 字符串.
- 适合对外接口返回雪花 ID, 数据库主键等长整型字段.

## 常见坑与排查建议

| 异常/消息        | 原因                               | 解决方式                                    |
|------------------|------------------------------------|---------------------------------------------|
| 前端 ID 精度丢失 | 长整型超过 JavaScript 安全整数范围 | 启用 `SafeLongModule` 或 DTO 字段改为字符串 |
| 调用方类型不兼容 | 原本期望数字, 现在收到字符串       | 作为接口变更处理, 提前通知调用方            |
| 模块未生效       | `JsonUtil` 已经初始化              | 在首次 JSON 读写前完成初始化                |
