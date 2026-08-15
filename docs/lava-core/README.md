# 基础工具集概览

`lava-core` 提供 Java 项目常用的基础工具能力, 包括重试执行, 时间处理, ID 生成, IO 复制, 参数校验和邮箱校验.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-core`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-core</artifactId>
</dependency>
```

## 模块能力

| 能力          | 入口                | 文档                                                    |
|---------------|---------------------|---------------------------------------------------------|
| 重试执行      | `RetryUtil`         | [RetryUtil 重试执行](./retry-util.md)                   |
| 时间解析      | `TimeUtil`          | [TimeUtil 时间解析](./time-util.md)                     |
| 时长格式化    | `DurationFormatter` | [DurationFormatter 时长格式化](./duration-formatter.md) |
| ID 生成       | `IdUtil`            | [IdUtil ID 生成](./id-util.md)                          |
| IO 与数据传输 | `IoUtil`            | [IoUtil IO 与数据传输](./io-util.md)                    |
| 参数校验      | `Validate`          | [Validate 参数校验](./validate.md)                      |
| 邮箱校验      | `EmailValidator`    | [EmailValidator 邮箱校验](./email-validator.md)         |

## 快速示例

```java
import com.zhengshuyun.lava.core.id.IdUtil;
import com.zhengshuyun.lava.core.time.TimeUtil;

import java.time.LocalDateTime;

public class LavaCoreQuickStartDemo {

    public static void main(String[] args) {
        // 1. 解析常见时间字符串
        LocalDateTime createdAt = TimeUtil.parse("2026-02-08 10:30:00");

        // 2. 生成对外展示更稳妥的字符串 ID
        String requestId = IdUtil.nextSeataSnowflakeIdAsString();

        // TODO: 按业务处理 createdAt/requestId
    }
}
```

## 使用建议

- 新项目建议先通过 `lava-bom` 统一版本, 再按需引入 `lava-core`.
- README 只保留模块入口和最小示例, 具体功能说明放在独立文档中.
- 业务代码优先使用明确的工具入口, 避免同一项目内散落多套基础工具实现.
