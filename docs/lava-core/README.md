# 基础工具集教程

`lava-core` 提供重试执行, ID 生成, 时间解析, IO 复制等基础能力, 底层复用 Guava.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-core`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-core</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.id.IdUtil;
import com.zhengshuyun.lava.core.retry.RetryUtil;
import com.zhengshuyun.lava.core.time.TimeUtil;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class LavaCoreQuickStartDemo {

    public static void main(String[] args) {
        // 1) 构造重试执行器并调用不稳定任务
        AtomicInteger attempts = new AtomicInteger(0);
        String config = RetryUtil.retrier()
                .setMaxAttempts(3)
                .setFixedDelayMillis(200)
                .build()
                .execute(() -> loadRemoteConfig(attempts));

        // 2) 解析时间字符串(支持多种格式)
        LocalDateTime parsed = TimeUtil.parse("2026-02-08 10:30:00");

        // 3) 生成分布式 ID
        String requestId = IdUtil.nextSeataSnowflakeIdAsString();

        // TODO: 按业务处理 config/parsed/requestId
    }

    private static String loadRemoteConfig(AtomicInteger attempts) {
        // 这里模拟第一次失败, 第二次成功
        if (attempts.incrementAndGet() < 2) {
            throw new IllegalStateException("simulate transient error");
        }
        return "ok";
    }
}
```

- `setMaxAttempts(3)`: 总尝试次数是 `3`, 包含首次执行.
- `setFixedDelayMillis(200)`: 两次重试间隔 `200ms`.
- `TimeUtil.parse(...)`: 解析失败返回 `null`, 调用方需要判空.

## 重试执行

### 固定延迟 + 指定异常重试

```java
// 只对 IOException 重试, 其余异常直接抛出
String body = RetryUtil.retrier()
        .setMaxAttempts(5)
        .setFixedDelayMillis(300)
        .setRetryOnException(java.io.IOException.class)
        .build()
        .execute(() -> callRemoteApi());
```

- `setRetryOnException(...)`: 仅匹配到指定异常类型才重试.
- `execute(...)`: 最终失败时, 受检异常包装为 `RetryException`, 非受检异常直接抛出.

### 指数退避

```java
// 首次重试等待 200ms, 之后按 2 倍增长, 上限 3s
RetryUtil.retrier()
        .setMaxAttempts(6)
        .setExponentialBackoffMillis(200, 2.0, 3000)
        .build()
        .execute(() -> invokeThirdParty());
```

- `setExponentialBackoffMillis(...)`: 适合短时抖动明显的外部依赖.
- 建议只用于幂等操作, 避免重复执行带来副作用.

## 时间与时长处理

```java
import com.zhengshuyun.lava.core.time.DurationFormatter;
import com.zhengshuyun.lava.core.time.TimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// 支持多种日期格式
LocalDateTime t1 = TimeUtil.parse("2026-02-08 10:00:00");
LocalDateTime t2 = TimeUtil.parse("2026/02/08 10:00:00.123");

// 自定义时长输出格式
DurationFormatter formatter = DurationFormatter.builder()
        .setRange(ChronoUnit.HOURS, ChronoUnit.SECONDS)
        .setChinese()
        .build();
String text = formatter.format(Duration.ofSeconds(3661));
```

- `TimeUtil.parse(...)`: 同时支持 `yyyy-MM-dd HH:mm:ss` 和 `yyyy/MM/dd HH:mm:ss.SSS` 等格式.
- `DurationFormatter`: 默认英文单位, `setChinese()` 可切换中文单位.

## IO 与数据传输

```java
import com.zhengshuyun.lava.core.io.DataTransferUtil;
import com.zhengshuyun.lava.core.io.IoUtil;

import java.nio.file.Path;

// 复制文件
long copied = IoUtil.copier()
        .setSource(Path.of("/tmp/source.bin"))
        .build()
        .write(Path.of("/tmp/target.bin"));

// 格式化进度文本
DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(10 * 1024 * 1024);
String progress = tracker.format(copied);
```

- `IoUtil.copier()`: 统一入口创建 `ByteStreamCopier.Builder`.
- `Tracker.format(...)`: 输出包含大小, 百分比, 速率, 剩余时间的进度文本.

## 常见坑与排查建议

- `setMaxAttempts(1)` 表示只执行一次, 不会重试.
- `TimeUtil.parse(...)` 返回 `null` 通常是输入格式不匹配, 先打印原始字符串排查空格和分隔符.
- `ByteStreamCopier.Builder#setSource(InputStream)` 只支持单次写入, 需要多次写入时用 `setSource(Supplier<InputStream>)`.
- `IdUtil.initSeataSnowflake(...)` 只能初始化一次, 重复调用会抛 `IllegalArgumentException`.

## 实践建议

- 重试策略优先在业务边界统一配置, 避免每个调用点散落不同参数.
- 时间格式常量优先复用 `DateTimePatterns`, 时区常量优先复用 `ZoneIds`.
- 对外暴露 ID 建议使用 `nextSeataSnowflakeIdAsString()`, 避免前端精度丢失.
