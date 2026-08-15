# IdUtil ID 生成

`IdUtil` 用于生成业务常用 ID, 包括雪花 ID 和 UUID.

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.id.IdUtil;

public class IdUtilDemo {

    public static void main(String[] args) {
        String snowflakeId = IdUtil.nextSeataSnowflakeIdAsString();
        String uuid = IdUtil.randomUUID();
        String uuidWithoutDash = IdUtil.randomUUIDWithoutDash();

        // TODO: 按业务处理 snowflakeId/uuid/uuidWithoutDash
    }
}
```

- `nextSeataSnowflakeIdAsString()`: 生成字符串雪花 ID, 适合对外返回.
- `nextSeataSnowflakeId()`: 生成 `long` 类型雪花 ID.
- `randomUUID()`: 生成带横杠的 UUID.
- `randomUUIDWithoutDash()`: 生成不带横杠的 UUID.

## 自定义雪花 ID 生成器

```java
import com.zhengshuyun.lava.core.id.IdUtil;
import com.zhengshuyun.lava.core.id.SeataSnowflake;

IdUtil.initSeataSnowflake(new SeataSnowflake(1, 1));
String id = IdUtil.nextSeataSnowflakeIdAsString();
```

- `initSeataSnowflake(...)` 只能在首次生成雪花 ID 前调用.
- 多数项目不需要自定义, 直接使用默认实现即可.

## 常见坑与排查建议

| 异常/消息                               | 原因                                | 解决方式                       |
|-----------------------------------------|-------------------------------------|--------------------------------|
| `seataSnowflake is already initialized` | 重复初始化雪花 ID 生成器            | 只在应用启动阶段初始化一次     |
| 前端展示 ID 精度异常                    | `long` 类型 ID 超过前端安全整数范围 | 对外接口优先返回字符串 ID      |
| UUID 格式不符合下游要求                 | 下游要求不带横杠                    | 使用 `randomUUIDWithoutDash()` |
