# 任务调度教程

`lava-schedule` 提供统一任务调度 API, 底层基于 Quartz, 默认使用虚拟线程执行任务.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-schedule`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-schedule</artifactId>
</dependency>
```

## 最小可运行示例

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleQuickStartDemo {

    public static void main(String[] args) throws InterruptedException {
        // 1) 准备任务逻辑
        AtomicInteger counter = new AtomicInteger(0);

        // 2) 创建并提交间隔任务(每 1 秒触发一次)
        ScheduledTask task = ScheduleUtil.scheduler(() -> {
                    counter.incrementAndGet();
                    // TODO: 执行业务逻辑
                })
                .setId("health-check-1s")
                .setTrigger(Trigger.interval(1000).build())
                .schedule();

        // 3) 运行 5 秒后删除任务
        Thread.sleep(5000);
        boolean deleted = task.delete();

        // TODO: 按业务处理 counter/deleted
    }
}
```

- `setTrigger(...)` 是必填项, 否则 `schedule()` 会抛异常.
- 任务 ID 不填时会自动生成 UUID, 但生产环境建议显式命名.

## 触发器类型

### 固定间隔(`interval`)

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
- `repeatCount(2)`: 总执行次数是 `2 + 1 = 3` 次.
- 默认 `repeatCount = -1`, 表示无限重复.

### Cron(`cron`)

```java
ScheduledTask task = ScheduleUtil.scheduler(() -> backup())
        .setId("daily-backup")
        .setTrigger(Trigger.cron("0 0 2 * * ?").build())
        .schedule();
```

- 该表达式表示每天 UTC `02:00` 执行一次.
- Cron 详细规则见 [cron-表达式教程](./cron-表达式教程.md).

### 延迟一次(`delay`)

```java
ScheduledTask task = ScheduleUtil.scheduler(() -> warmUp())
        .setId("service-warm-up")
        .setTrigger(Trigger.delay(10_000).build())
        .schedule();
```

- `delay(...)` 只执行一次, 完成后自动结束.

## 任务生命周期管理

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

ScheduledTask task = ScheduleUtil.scheduler(() -> check())
        .setId("manage-demo")
        .setTrigger(Trigger.interval(3000).build())
        .schedule();

boolean exists = task.exists();
task.pause();
boolean paused = task.isPaused();
task.resume();
task.triggerNow();
ScheduleUtil.reschedule("manage-demo", Trigger.interval(1000).build());
Long nextFireTime = task.getNextFireTime();
Long previousFireTime = task.getPreviousFireTime();
boolean deleted = task.delete();
```

- `triggerNow()` 不会改变原有调度规则.
- `delete()` 和 `ScheduleUtil.deleteTask(...)` 都是幂等操作.

## 高级用法

### 自定义任务执行器

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 1) 为 CPU 密集任务准备固定线程池
ExecutorService fixedPool = Executors.newFixedThreadPool(8);

// 2) 在首次创建任务前初始化执行器
ScheduleUtil.initTaskExecutor(fixedPool);
```

- `initTaskExecutor(...)` 只能初始化一次.
- 传 `null` 或重复初始化都会抛 `IllegalArgumentException`.

### 自定义触发策略

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.Trigger;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

ScheduleUtil.scheduler(() -> sendReport())
        .setId("custom-trigger")
        .setTrigger(Trigger.custom(() -> TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(30)
                        .repeatForever())))
        .schedule();
```

- 框架会自动补充 `withIdentity(...)` 和 `build()`.

## 常见坑与排查建议

- 同一任务 ID 重复创建会调度失败, 需要保证业务层唯一性.
- Cron 表达式非法会在调度阶段抛异常, 建议关键表达式放入单测.
- 任务内部异常不会中断后续调度, 需要在任务内部自行记录日志和告警.
- 默认 Cron 按 UTC 解析, 如果业务按本地时区执行, 需要先做时区换算.

## 任务 ID 命名建议

建议使用 `业务域-动作-频率` 规范, 例如:

- `order-timeout-scan-1min`
- `coupon-expire-clean-1day`
- `risk-device-sync-5min`

这样在 `getAllTasks()` 和监控系统中更容易定位任务.
