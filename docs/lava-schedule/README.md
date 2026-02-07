# 任务调度教程

本文是 `lava-schedule` 的使用教程. `lava-schedule` 对外提供统一调度 API, 底层基于 Quartz 实现.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-schedule`.

```xml

<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-schedule</artifactId>
</dependency>
```

## 最小可运行示例

下面示例每 1 秒执行一次任务, 执行 5 秒后删除任务.

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleQuickStartDemo {

    public static void main(String[] args) throws InterruptedException {
        // 用计数器观察任务执行次数
        AtomicInteger counter = new AtomicInteger();

        // 1) 创建调度器构建器; 2) 配置任务 ID 和触发器; 3) schedule() 提交任务
        ScheduledTask task = ScheduleUtil.scheduler(() -> {
                    // 自增计数器
                    counter.incrementAndGet();
                    // TODO: 记录日志或执行业务逻辑
                })
                // 任务唯一标识, 后续暂停/恢复/删除都依赖它
                .setId("health-check")
                // 每 1000ms 执行一次
                .setTrigger(Trigger.interval(1000).build())
                .schedule();

        // 主线程等待 5 秒, 让任务有时间执行
        Thread.sleep(5000);

        // 删除任务, 返回值表示是否真的删除到了一个存在的任务
        boolean deleted = task.delete();

        // TODO: 按业务处理 deleted
    }
}
```

## 触发器类型

`Trigger` 当前提供 4 种模式: `interval`、`cron`、`delay`、`custom`.

### 固定间隔触发(`interval`)

```java
// 每 5 秒执行一次, 首次延迟 1 秒, 总共执行 3 次
ScheduledTask task = ScheduleUtil.scheduler(() -> doWork())
                .setId("interval-task")
                .setTrigger(Trigger.interval(5000)
                        .initialDelay(1000)
                        .repeatCount(2)
                        .build())
                .schedule();
```

- `interval(5000)`: 每 5 秒触发一次.
- `initialDelay(1000)`: 首次延迟 1 秒.
- `repeatCount(2)`: 重复 2 次, 即总执行 `2 + 1 = 3` 次.
- 不设置 `repeatCount` 时默认 `-1`, 表示无限重复.

### Cron 触发(`cron`)

```java
// Cron: 每天 UTC 02:00 执行一次
ScheduledTask task = ScheduleUtil.scheduler(() -> backup())
                .setId("daily-backup")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();
```

- 示例表示每天 UTC 02:00 执行一次.
- `lava-schedule` 内部按 UTC 解析 Cron, 可避免多时区机器出现触发偏差.
- 如果你想系统学习 Cron 语法和常见表达式, 参考 [Cron 表达式使用教程](./cron-表达式-使用教程.md).

### 延迟一次(`delay`)

```java
// 延迟 10 秒执行一次
ScheduledTask task = ScheduleUtil.scheduler(() -> warmUp())
                .setId("warm-up")
                .setTrigger(Trigger.delay(10_000).build())
                .schedule();
```

- 延迟 10 秒后执行 1 次.
- 任务执行完成后一次性触发器会自动清理.

## 任务生命周期管理

创建后可通过 `ScheduledTask` 或 `ScheduleUtil` 做管理.

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

public class ScheduleManageDemo {

    public static void main(String[] args) {
        // 先创建一个每 5 秒执行一次的任务
        ScheduledTask task = ScheduleUtil.scheduler(() -> check())
                .setId("manage-demo")
                .setTrigger(Trigger.cron("0/5 * * * * ?").build())
                .schedule();

        // 查询任务是否存在
        boolean exists = task.exists();

        // 暂停任务, 暂停后触发器不会再触发
        task.pause();
        boolean paused = task.isPaused();

        // 恢复任务
        task.resume();

        // 立即触发一次(不影响原有调度计划)
        task.triggerNow();

        // 把触发规则改成每 1 秒执行一次
        ScheduleUtil.reschedule("manage-demo", Trigger.interval(1000).build());

        // 获取下次/上次执行时间(毫秒时间戳)
        Long nextFireTime = task.getNextFireTime();
        Long previousFireTime = task.getPreviousFireTime();

        // 删除任务; 第二次删除通常返回 false(幂等)
        boolean deleted = task.delete();
        boolean deletedAgain = ScheduleUtil.deleteTask("manage-demo");

        // TODO: 按业务处理 exists/paused/nextFireTime/previousFireTime/deleted/deletedAgain
    }

    private static void check() {
        // TODO: 你的业务逻辑
    }
}
```

常用管理 API:

- `ScheduleUtil.hasTask(taskId)`: 任务是否存在.
- `ScheduleUtil.getTask(taskId)`: 获取任务句柄, 任务不存在会抛 `ScheduleException`.
- `ScheduleUtil.getAllTasks()`: 获取当前任务快照列表.
- `ScheduleUtil.deleteTask(taskId)`: 幂等删除, 不存在返回 `false`.

## 自定义任务执行器

默认执行器是虚拟线程池. 如果你的任务是 CPU 密集型, 可以在首次调度前初始化固定线程池.

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduleExecutorDemo {

    public static void main(String[] args) {
        // 示例: 为 CPU 密集型任务准备固定线程池
        ExecutorService fixedPool = Executors.newFixedThreadPool(8);

        // 必须在首次使用 ScheduleUtil 之前调用, 且只能初始化一次
        ScheduleUtil.initTaskExecutor(fixedPool);

        // TODO: 创建任务
    }
}
```

注意:

- `initTaskExecutor(null)` 会抛 `IllegalArgumentException`.
- 重复初始化也会抛 `IllegalArgumentException`.
- 任务执行器中的异常不会向调度线程传播.

## 自定义触发策略

当内置触发器不满足需求时, 可以使用 `Trigger.custom`. 这类高级场景可以直接构建底层触发器对象.

框架会自动处理 `withIdentity()` 和 `build()`, 用户只需提供 `TriggerBuilder` 配置.

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.Trigger;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

// custom 适合复杂场景: 需要直接控制底层触发器行为
ScheduleUtil.scheduler(() -> sendReport())
        .setId("custom-trigger")
        .setTrigger(Trigger.custom(() -> TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        // 每 30 秒触发一次, 永久重复
                        .withIntervalInSeconds(30)
                        .repeatForever())))
        .schedule();
```

## 常见坑与排查建议

- `setTrigger(...)` 是必填项, 未设置会在 `schedule()` 时抛 `IllegalArgumentException`.
- 如果不设置 `setId(...)`, 框架会自动生成 UUID 作为任务 ID.
- 同一个任务 ID 重复创建会调度失败, 请保证业务侧 ID 唯一.
- 任务内部异常会被隔离, 不会中断后续调度; 生产环境建议在任务内部自行记录日志和告警.
- Cron 表达式非法时会在调度阶段失败, 建议上线前通过单元测试覆盖关键表达式.

## 一个实用的落地建议

在实际业务里, 建议统一把任务 ID 命名为 `业务域-动作-频率`, 例如:

- `order-timeout-scan-1min`
- `coupon-expire-clean-1day`
- `risk-device-sync-5min`

这样在 `getAllTasks()` 和监控系统中更容易定位问题.
