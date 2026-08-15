# TaskScheduler 任务提交

`TaskScheduler` 用于构建并提交调度任务, 常用入口是 `ScheduleUtil.scheduler(...)`.

## 最小可运行示例

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

public class TaskSchedulerDemo {

    public static void main(String[] args) {
        ScheduledTask task = ScheduleUtil.scheduler(() -> {
                    // TODO: 执行业务逻辑
                })
                .setId("order-timeout-scan")
                .setTrigger(Trigger.interval(5000).build())
                .schedule();

        // TODO: 按业务持有 task
    }
}
```

- `setId(...)`: 设置任务 ID, 生产环境建议显式命名.
- `setTrigger(...)`: 设置触发器, 必填.
- `schedule()`: 提交任务并返回 `ScheduledTask`.

## 任务 ID 建议

建议使用 `业务域-动作-频率` 规范, 例如:

- `order-timeout-scan-1min`
- `coupon-expire-clean-1day`
- `risk-device-sync-5min`

## 常见坑与排查建议

| 异常/消息        | 原因                         | 解决方式                                 |
|------------------|------------------------------|------------------------------------------|
| 调度失败         | 未设置触发器                 | 调用 `setTrigger(...)` 后再 `schedule()` |
| 任务 ID 冲突     | 同一 ID 重复创建             | 保证业务层任务 ID 唯一                   |
| 任务异常未被发现 | 任务内部异常不会停止后续调度 | 任务内部自行记录日志和告警               |
