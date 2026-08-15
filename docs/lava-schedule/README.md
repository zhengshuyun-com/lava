# 任务调度概览

`lava-schedule` 提供统一任务调度 API, 支持间隔任务, Cron 任务, 延迟任务和任务生命周期管理.

## 引入依赖

如果你已经通过 BOM 管理版本, 只需引入 `lava-schedule`.

```xml
<dependency>
    <groupId>com.zhengshuyun</groupId>
    <artifactId>lava-schedule</artifactId>
</dependency>
```

## 模块能力

| 能力        | 入口                | 文档                                          |
|-------------|---------------------|-----------------------------------------------|
| 提交任务    | `ScheduleUtil`      | [TaskScheduler 任务提交](./task-scheduler.md) |
| 触发器      | `Trigger`           | [Trigger 触发器](./trigger.md)                |
| 生命周期    | `ScheduledTask`     | [ScheduledTask 生命周期](./scheduled-task.md) |
| Cron 表达式 | `Trigger.cron(...)` | [Cron 表达式](./cron-expression.md)           |
| 任务执行器  | `ScheduleUtil`      | [任务执行器配置](./task-executor.md)          |

## 快速示例

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

public class ScheduleQuickStartDemo {

    public static void main(String[] args) {
        ScheduledTask task = ScheduleUtil.scheduler(() -> {
                    // TODO: 执行业务逻辑
                })
                .setId("health-check-1s")
                .setTrigger(Trigger.interval(1000).build())
                .schedule();

        boolean exists = task.exists();

        // TODO: 按业务处理 exists
    }
}
```

## 使用建议

- 生产环境建议显式设置任务 ID, 便于管理和排查.
- Cron 默认按 UTC 解释, 本地业务时间需要提前换算.
- 任务内部异常不会中断后续调度, 需要在任务内记录日志和告警.
