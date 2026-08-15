# DurationFormatter 时长格式化

`DurationFormatter` 用于把 `Duration` 格式化为可读文本, 适合日志, 进度展示和运行耗时展示.

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.time.DurationFormatter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DurationFormatterDemo {

    public static void main(String[] args) {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.HOURS, ChronoUnit.SECONDS)
                .setChinese()
                .build();

        String text = formatter.format(Duration.ofSeconds(3661));

        // TODO: 按业务处理 text
    }
}
```

- `setRange(...)`: 设置最大单位和最小单位.
- `setChinese()`: 使用中文单位.
- `format(...)`: 不接受 `null` 或负数.

## 常用配置

```java
DurationFormatter english = DurationFormatter.builder()
        .setRange(ChronoUnit.HOURS, ChronoUnit.SECONDS)
        .setEnglish()
        .build();

DurationFormatter withZero = DurationFormatter.builder()
        .setRange(ChronoUnit.MINUTES, ChronoUnit.SECONDS)
        .setShowZeroValues(true)
        .setSeparator(", ")
        .build();
```

| 配置                     | 说明                   |
|--------------------------|------------------------|
| `setLargestUnit(...)`    | 设置最大展示单位       |
| `setSmallestUnit(...)`   | 设置最小展示单位       |
| `setRange(...)`          | 同时设置最大和最小单位 |
| `setShowZeroValues(...)` | 是否展示零值单位       |
| `setSeparator(...)`      | 设置单位之间的分隔符   |

## 常见坑与排查建议

| 异常/消息                     | 原因                                | 解决方式                           |
|-------------------------------|-------------------------------------|------------------------------------|
| `duration cannot be null`     | 传入了 `null`                       | 调用前先确认耗时对象已生成         |
| `duration cannot be negative` | 传入了负数时长                      | 检查开始和结束时间的计算顺序       |
| 年月结果与自然日不完全一致    | 年按 `365` 天, 月按 `30` 天近似计算 | 精确日历周期用业务日期逻辑单独处理 |
