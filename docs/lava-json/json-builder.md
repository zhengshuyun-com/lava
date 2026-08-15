# JsonBuilder 自定义配置

`JsonBuilder` 用于构建自定义 `ObjectMapper`, 并可通过 `JsonUtil.initObjectMapper(...)` 替换模块默认配置.

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.time.DateTimePatterns;
import com.zhengshuyun.lava.json.JsonBuilder;
import com.zhengshuyun.lava.json.JsonUtil;
import com.zhengshuyun.lava.json.SafeLongModule;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

public class JsonBuilderDemo {

    public static void main(String[] args) {
        ObjectMapper mapper = new JsonBuilder()
                // 只影响 LocalDateTime, 不影响 Instant/Date
                .setDateTimeFormat(DateTimePatterns.DATE_TIME)
                .setLocale(Locale.CHINA)
                .setCustomizer(builder -> builder.addModule(new SafeLongModule()))
                .build();

        JsonUtil.initObjectMapper(mapper);
    }
}
```

- `initObjectMapper(...)` 只能调用一次.
- 必须在首次读写 JSON 前初始化.
- 修改时间格式会影响接口兼容性, 建议团队统一约定后再调整.

## 设计原则

不改 Jackson 的核心行为, 只提供简化配置的入口. 不配置任何参数时, 行为与裸 Jackson 3 一致,
也与 Spring Boot 的默认值一致, 因此 `@JsonFormat`, `@JsonSerialize` 等注解全部照常生效.

只有一处偏离 Jackson 默认值: `Locale` 默认收敛为 `Locale.ROOT`.
Jackson 取 JVM 默认地区, 会导致同一份代码在不同机器上输出不同结果.

## 可配置项

所有格式和时区配置默认不生效, 只在显式调用后覆盖 Jackson 行为.

| 配置                    | 作用范围                                | 不配置时            |
|-------------------------|-----------------------------------------|---------------------|
| `setDateTimeFormat(..)` | `LocalDateTime`                         | Jackson 的 ISO-8601 |
| `setDateFormat(..)`     | `LocalDate`                             | Jackson 的 ISO-8601 |
| `setTimeFormat(..)`     | `LocalTime`                             | Jackson 的 ISO-8601 |
| `setZone(..)`           | `Date`, `Calendar` 等绝对时刻的渲染时区 | Jackson 默认值 UTC  |
| `setLocale(..)`         | 含文本的格式, 例如月份名                | `Locale.ROOT`       |
| `setCustomizer(..)`     | 直接操作 `JsonMapper.Builder`           | 不调用              |

格式配置只作用于三个 `java.time` 本地时间类型. 其他时间类型需要定制时用字段上的 `@JsonFormat`.

沿用的 Jackson 3 默认行为: 反序列化忽略未知字段, 序列化空对象不抛异常,
时间输出 ISO 文本而非数字时间戳.

## 常见坑与排查建议

| 异常/消息                            | 原因                                         | 解决方式                            |
|--------------------------------------|----------------------------------------------|-------------------------------------|
| `JsonUtil is already initialized`    | 首次读写后又初始化                           | 在应用启动阶段提前初始化            |
| `UnsupportedTemporalTypeException`   | `setDateTimeFormat` 里带了偏移量 `XXX`/`Z`   | 本地时间不能带偏移量, 去掉该部分    |
| 改了格式但 `Instant`/`Date` 没变     | 格式配置只作用于本地时间类型                 | 用字段上的 `@JsonFormat` 定制       |
| 设了时区后偏移量丢失                 | 显式设时区会让 Jackson 归一 `OffsetDateTime` | 不设时区即保留原偏移量              |
| 时间格式变化导致调用方失败           | 修改了对外 JSON 格式                         | 评估兼容性并灰度发布                |
| 自定义模块未生效                     | 初始化时机太晚                               | 确认没有其他代码提前触发 `JsonUtil` |
