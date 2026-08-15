# 时间格式规范

`lava-json` 不改 Jackson 的核心行为, 只提供简化配置的入口.
不配置任何参数时, 输出与裸 Jackson 3 一致, 也与 Spring Boot 的默认值一致,
因此 `@JsonFormat` 等注解全部照常生效.

## 最小可运行示例

```java
import com.zhengshuyun.lava.json.JsonUtil;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class TimeFormatDemo {

    public static void main(String[] args) {
        Order order = new Order(
                Instant.parse("2026-01-01T12:30:00Z"),  // 下单时刻
                LocalDate.of(2026, 1, 1),               // 配送日期
                LocalTime.of(9, 0)                      // 营业开始时间
        );

        String json = JsonUtil.writeValueAsString(order);
        // {"createdAt":"2026-01-01T12:30:00Z","deliveryDate":"2026-01-01","openAt":"09:00:00"}

        // TODO: 按业务处理 json
    }

    public record Order(Instant createdAt, LocalDate deliveryDate, LocalTime openAt) {
    }
}
```

## 默认输出

默认时区为 UTC, 亚秒精度按实际值输出.

| 类型                 | 默认输出示例                  | 说明                          |
|----------------------|-------------------------------|-------------------------------|
| `Instant`            | `2026-01-01T12:30:00Z`        | 推荐用于表示"某个时刻"          |
| `java.util.Date`     | `2026-01-01T12:30:00.000Z`    | Jackson 默认固定输出毫秒      |
| `java.sql.Timestamp` | `2026-01-01T12:30:00.000Z`    | 同 `Date`                     |
| `java.util.Calendar` | `2026-01-01T12:30:00.000Z`    | 同 `Date`                     |
| `OffsetDateTime`     | `2026-01-01T20:30:00+08:00`   | 保留原有偏移量                |
| `ZonedDateTime`      | `2026-01-01T20:30:00+08:00`   | 保留原有偏移量                |
| `LocalDateTime`      | `2026-01-01T12:30:00`         | 不带时区后缀                  |
| `LocalDate`          | `2026-01-01`                  |                               |
| `LocalTime`          | `12:30:00`                    |                               |
| `java.sql.Date`      | `2025-12-31T16:00:00.000Z`    | 见下方"已知陷阱"               |
| `java.sql.Time`      | `12:30:00`                    |                               |

`Instant` 只在有亚秒时输出小数部分, `Date` 固定输出三位毫秒, 这是 Jackson 自身的差异, 未做干预.
需要两者格式统一时用字段注解或全局 `setCustomizer(...)` 自行调整.

反序列化输入端保持宽松, 以下写法都能解析为同一时刻:

```
"2026-01-01T12:30:00Z"
"2026-01-01T20:30:00+08:00"
```

## 已知陷阱: 不要在接口上用 java.sql.Date

Jackson 按其父类 `java.util.Date` 把它当作绝对时刻处理, 会做时区换算,
`java.sql.Date` 却只有日期部分. 结果是 JSON 文本上的日期整体偏移:

```
JVM 时区 Asia/Shanghai
  java.sql.Date.valueOf("2026-01-01")  →  "2025-12-31T16:00:00.000Z"
```

同一时区内往返能还原, 跨时区会丢一天:

| 场景                        | 结果         |
|-----------------------------|--------------|
| 上海写, 上海读              | `2026-01-01` |
| 上海写, 纽约读              | `2025-12-31` |

`@JsonFormat(pattern = "yyyy/MM/dd")` 也救不回来, 因为换算发生在格式化之前, 输出 `2025/12/31`.

`lava-json` 不干预这个行为: 抢过来修就要接管序列化器, `@JsonFormat` 会随之失效,
等于用一个静默错误换另一个. 正确做法是在持久层就转成 `LocalDate`:

```java
LocalDate date = resultSet.getObject("birth_date", LocalDate.class);
```

## 自定义格式

格式配置只作用于三个 `java.time` 本地时间类型, 且只在显式调用后生效.

| 配置                    | 作用范围        |
|-------------------------|-----------------|
| `setDateTimeFormat(..)` | `LocalDateTime` |
| `setDateFormat(..)`     | `LocalDate`     |
| `setTimeFormat(..)`     | `LocalTime`     |

绝对时刻不受上述配置影响, 需要定制时用字段注解:

```java
public record Order(
        @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss", timezone = "Asia/Shanghai")
        Instant createdAt) {
}
```

字段注解优先级高于全局配置.

## 类型选择建议

| 业务含义                 | 推荐类型      | 避免                        |
|--------------------------|---------------|-----------------------------|
| 某个时刻, 如创建时间     | `Instant`     | `java.sql.Timestamp`        |
| 日历上的某天, 如生日     | `LocalDate`   | `java.sql.Date`             |
| 一天中的钟点, 如营业时间 | `LocalTime`   | `java.sql.Time`             |

`LocalDateTime` 既不是绝对时刻也不是纯日历概念, 语义模糊, 不建议出现在对外接口上.
需要表达时刻请用 `Instant`, 从业务时区转换时显式写出 `ZoneId`:

```java
Instant instant = localDateTime.atZone(ZoneIds.ASIA_SHANGHAI).toInstant();
```

## 常见坑与排查建议

| 异常/消息                          | 原因                                       | 解决方式                              |
|------------------------------------|--------------------------------------------|---------------------------------------|
| 前端显示时区不对                   | 后端默认输出 UTC, 前端未转换展示时区       | 前端或展示层按用户时区转换            |
| 改了格式但 `Instant`/`Date` 没变   | 格式配置只作用于三个本地时间类型           | 用字段上的 `@JsonFormat` 定制         |
| `java.sql.Date` 日期少一天         | Jackson 按绝对时刻做了时区换算             | 改用 `LocalDate`, 见"已知陷阱"          |
| `Unsupported field: YearOfEra`     | `@JsonFormat` 给 `Instant` 配了日期格式但没给时区 | 注解上补 `timezone = "..."`      |
| `UnsupportedTemporalTypeException` | `setDateTimeFormat` 里带了偏移量 `XXX`/`Z` | 本地时间不能带偏移量, 去掉该部分      |
| 设了时区后偏移量丢失               | 显式设时区会让 Jackson 归一 `OffsetDateTime` | 不设时区即保留原偏移量              |
| `LocalDateTime` 时区含义不清       | 该类型本身不含时区                         | 改用 `Instant`, 转换时显式写 `ZoneId` |
| 入库时间混乱                       | 多处使用不同时间约定                       | 建议统一 UTC 入库                     |
