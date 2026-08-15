# RetryUtil 重试执行

`RetryUtil` 用于执行可能短暂失败的任务, 适合远程调用, 配置读取, 短时资源竞争等可重试场景.

## 最小可运行示例

```java
import com.zhengshuyun.lava.core.retry.RetryUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class RetryUtilDemo {

    public static void main(String[] args) {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryUtil.retrier()
                .setMaxAttempts(3)
                .setFixedDelayMillis(200)
                .build()
                .execute(() -> loadRemoteConfig(attempts));

        // TODO: 按业务处理 result
    }

    private static String loadRemoteConfig(AtomicInteger attempts) {
        if (attempts.incrementAndGet() < 2) {
            throw new IllegalStateException("simulate transient error");
        }
        return "ok";
    }
}
```

- `setMaxAttempts(3)`: 总尝试次数是 `3`, 包含首次执行.
- `setFixedDelayMillis(200)`: 两次尝试之间等待 `200ms`.
- `execute(...)`: 任务最终失败时, 非受检异常直接抛出, 受检异常包装为 `RetryException`.

## 固定延迟

```java
String body = RetryUtil.retrier()
        .setMaxAttempts(5)
        .setFixedDelayMillis(300)
        .setRetryOnException(java.io.IOException.class)
        .build()
        .execute(() -> callRemoteApi());
```

- `setRetryOnException(...)`: 只对指定异常类型重试.
- 适合网络抖动, 临时限流, 下游短暂不可用等场景.

## 指数退避

```java
RetryUtil.retrier()
        .setMaxAttempts(6)
        .setExponentialBackoffMillis(200, 2.0, 3000)
        .build()
        .execute(() -> invokeThirdParty());
```

- `initialDelay`: 首次重试等待时间.
- `multiplier`: 每次重试后的等待时间倍数.
- `maxDelay`: 单次等待时间上限.

## 常见坑与排查建议

| 异常/消息                    | 原因                       | 解决方式                               |
|------------------------------|----------------------------|----------------------------------------|
| `maxAttempts must be >= 1`   | 最大尝试次数小于 `1`       | 至少设置为 `1`, 表示只执行一次         |
| 最终抛出 `RetryException`    | 受检异常在所有尝试后仍失败 | 检查原始 `cause` 并确认是否应继续重试  |
| 重复写入或重复调用造成副作用 | 被重试任务不是幂等操作     | 只对幂等操作重试, 或在业务层加幂等保护 |
