# Trigger 触发器

`Trigger` 用于描述任务触发规则, 支持固定间隔, Cron, 延迟一次和自定义触发策略.

## 固定间隔

```java
ScheduledTask task = ScheduleUtil.scheduler(() -> doWork())
        .setId("order-timeout-scan")
        .setTrigger(Trigger.interval(5000)
                .initialDelay(1000)
                .repeatCount(2)
                .build())
        .schedule();
```

- `interval(5000)`: 每 `5s` 触发一次.
- `initialDelay(1000)`: 首次触发前等待 `1s`.
- `repeatCount(2)`: 总执行次数是 `2 + 1 = 3` 次.

## Cron

```java
ScheduledTask task = ScheduleUtil.scheduler(() -> backup())
        .setId("daily-backup")
        .setTrigger(Trigger.cron("0 0 2 * * ?").build())
        .schedule();
```

- 该表达式表示每天 UTC `02:00` 执行一次.
- 详细规则见 [Cron 表达式](./cron-expression.md).

## 延迟一次

```java
ScheduledTask task = ScheduleUtil.scheduler(() -> warmUp())
        .setId("service-warm-up")
        .setTrigger(Trigger.delay(10_000).build())
        .schedule();
```

- `delay(...)` 只执行一次, 完成后自动结束.

## 常见坑与排查建议

| 异常/消息              | 原因                                   | 解决方式                                   |
|------------------------|----------------------------------------|--------------------------------------------|
| Cron 不按预期时间执行  | 默认按 UTC 解释                        | 按业务时区提前换算                         |
| 间隔任务次数不符合预期 | `repeatCount` 是重复次数, 不等于总次数 | 总次数 = `repeatCount + 1`                 |
| 自定义触发器缺少身份   | 手动构建 Quartz Trigger 容易漏字段     | 使用 `Trigger.custom(...)`, 框架会补充身份 |
