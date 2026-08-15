# JsonBuilder 自定义配置

`JsonBuilder` 用于构建自定义 `ObjectMapper`, 并可通过 `JsonUtil.initObjectMapper(...)` 替换模块默认配置.

## 最小可运行示例

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhengshuyun.lava.core.time.DateTimePatterns;
import com.zhengshuyun.lava.core.time.ZoneIds;
import com.zhengshuyun.lava.json.JsonBuilder;
import com.zhengshuyun.lava.json.JsonUtil;
import com.zhengshuyun.lava.json.SafeLongModule;

import java.util.Locale;

public class JsonBuilderDemo {

    public static void main(String[] args) {
        ObjectMapper mapper = new JsonBuilder()
                .setDateTimeFormat(DateTimePatterns.DATE_TIME)
                .setZone(ZoneIds.ASIA_SHANGHAI)
                .setLocale(Locale.CHINA)
                .setCustomizer(builder -> builder.addModule(new SafeLongModule()))
                .build();

        JsonUtil.initObjectMapper(mapper);
    }
}
```

- `initObjectMapper(...)` 只能调用一次.
- 必须在首次读写 JSON 前初始化.
- 修改时间格式或时区会影响接口兼容性, 建议团队统一约定后再调整.

## 默认配置

| 配置         | 默认值                     |
|--------------|----------------------------|
| 日期时间格式 | `yyyy-MM-dd'T'HH:mm:ss'Z'` |
| 日期格式     | `yyyy-MM-dd`               |
| 时间格式     | `HH:mm:ss`                 |
| 时区         | UTC                        |
| Locale       | `Locale.ROOT`              |
| 未知字段     | 反序列化时忽略             |

## 常见坑与排查建议

| 异常/消息                         | 原因                 | 解决方式                            |
|-----------------------------------|----------------------|-------------------------------------|
| `JsonUtil is already initialized` | 首次读写后又初始化   | 在应用启动阶段提前初始化            |
| 时间格式变化导致调用方失败        | 修改了对外 JSON 格式 | 评估兼容性并灰度发布                |
| 自定义模块未生效                  | 初始化时机太晚       | 确认没有其他代码提前触发 `JsonUtil` |
