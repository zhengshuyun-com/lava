# 时间格式规范

`lava-json` 默认按 UTC 输出日期时间, 业务展示层再按用户时区转换.

## 最小可运行示例

```java
import com.zhengshuyun.lava.json.JsonUtil;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeFormatDemo {

    public static void main(String[] args) {
        TimePayload payload = new TimePayload(
                LocalDateTime.of(2026, 1, 1, 12, 30),
                Instant.parse("2026-01-01T12:30:00Z"),
                LocalDate.of(2026, 1, 1),
                LocalTime.of(12, 30)
        );

        String json = JsonUtil.writeValueAsString(payload);

        // TODO: 按业务处理 json
    }

    public record TimePayload(LocalDateTime createdAt, Instant eventAt, LocalDate date, LocalTime time) {
    }
}
```

## 默认输出

| 类型            | 默认输出示例           | 说明            |
|-----------------|------------------------|-----------------|
| `LocalDateTime` | `2026-01-01T12:30:00Z` | 按统一格式输出  |
| `Date`          | `2026-01-01T12:30:00Z` | 按 UTC 格式输出 |
| `Instant`       | `2026-01-01T12:30:00Z` | 按 UTC 格式输出 |
| `LocalDate`     | `2026-01-01`           | 日期类型        |
| `LocalTime`     | `12:30:00`             | 时间类型        |

## 常见坑与排查建议

| 异常/消息        | 原因                                 | 解决方式                   |
|------------------|--------------------------------------|----------------------------|
| 前端显示时区不对 | 后端统一输出 UTC, 前端未转换展示时区 | 前端或展示层按用户时区转换 |
| 接口格式突然变化 | 自定义 `JsonBuilder` 修改了时间格式  | 对外接口变更前做兼容评估   |
| 入库时间混乱     | 多处使用不同时间约定                 | 建议统一 UTC 入库          |
