# 字符串、时间与参数校验

## 字符串处理

`StringUtils` 提供空值安全的常用字符串操作：

```java
boolean empty = StringUtils.isEmpty(input);
boolean blank = StringUtils.isBlank(input);
String safe = StringUtils.nullToEmpty(input);
String name = StringUtils.defaultIfBlank(input, "anonymous");
```

| 方法 | `null` | `""` | `"  "` |
| --- | ---: | ---: | ---: |
| `isEmpty` | `true` | `true` | `false` |
| `isBlank` | `true` | `true` | `true` |
| `defaultIfEmpty(value, "x")` | `"x"` | `"x"` | 保留原值 |
| `defaultIfBlank(value, "x")` | `"x"` | `"x"` | `"x"` |

完整方法包括 `isEmpty`、`isNotEmpty`、`isBlank`、`isNotBlank`、`nullToEmpty`、`emptyToNull`、
`defaultIfEmpty` 和 `defaultIfBlank`。这些方法不执行 `trim` 或 `strip`，非空原值会保持不变。

## 集合与映射判断

`CollectionUtils` 与 `MapUtils` 分别处理 `Collection` 和 `Map`，避免重复编写 null 与空容器判断：

```java
if (CollectionUtils.isEmpty(items)) {
    return;
}

if (MapUtils.isNotEmpty(headers)) {
    send(headers);
}
```

两者都把 `null` 视为空；`isNotEmpty` 仅在容器不为 `null` 且至少包含一个元素或条目时返回 true。

## 严格日期格式

`DateTimeFormatterUtils` 提供不可变、线程安全并采用严格解析的常用格式：

| 常量 | 格式 |
| --- | --- |
| `DATE` | `uuuu-MM-dd` |
| `TIME` | `HH:mm:ss` |
| `DATE_TIME` | `uuuu-MM-dd HH:mm:ss` |
| `DATE_TIME_MILLIS` | `uuuu-MM-dd HH:mm:ss.SSS` |
| `COMPACT_DATE` | `uuuuMMdd` |
| `COMPACT_DATE_TIME` | `uuuuMMddHHmmss` |
| `SLASH_DATE` | `uuuu/MM/dd` |
| `SLASH_DATE_TIME` | `uuuu/MM/dd HH:mm:ss` |

```java
LocalDate date = LocalDate.parse("2026-08-30", DateTimeFormatterUtils.DATE);
String text = DateTimeFormatterUtils.DATE_TIME.format(dateTime);
```

严格解析会拒绝 `2026-02-30` 之类不存在的日期。

## 兼容解析

`TimeUtils.parse(...)` 用于接收多种常见日期时间文本：

```java
LocalDateTime first = TimeUtils.parse("2026-08-30 12:30:00");
LocalDateTime second = TimeUtils.parse("2026/08/30 12:30");
LocalDateTime third = TimeUtils.parse("20260830123000");
LocalDateTime fourth = TimeUtils.parse("2026年08月30日 12时30分");
```

输入为 `null`、空白或无法识别时返回 `null`。如果协议只允许一种格式，优先直接使用对应的严格 `DateTimeFormatter`，不要使用兼容解析掩盖错误输入。

## 时长展示

```java
DurationFormatter formatter = DurationFormatter.builder()
        .chinese()
        .range(ChronoUnit.DAYS, ChronoUnit.SECONDS)
        .showZeroValues(false)
        .separator(" ")
        .build();

String text = formatter.format(Duration.ofSeconds(90));
```

支持的单位范围是天到纳秒。月和年不是固定时长，不能由 `Duration` 精确表达，因此不支持。

构建器可配置：

- `largestUnit(...)` 和 `smallestUnit(...)`；
- `range(...)`；
- `chinese()`、`english()` 或 `locale(...)`；
- `showZeroValues(...)`；
- `separator(...)`。

## 参数校验

`ValidationUtils` 适合在构造器和公开方法入口表达调用契约：

```java
ValidationUtils.requireTrue(pageSize > 0, "pageSize must be positive");
ValidationUtils.requireFalse(items.isEmpty(), "items must not be empty");

String name = ValidationUtils.requireNotBlank(input, "name must not be blank");
List<Item> items = ValidationUtils.requireNotEmpty(values, "items must not be empty");
Map<String, String> headers = ValidationUtils.requireNotEmpty(
        values,
        "headers must not be empty"
);
```

校验失败统一抛出 `IllegalArgumentException`；方法返回经过校验的原值，便于直接赋给字段。
