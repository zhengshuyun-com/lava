# ScheduledTask 生命周期

`ScheduledTask` 表示已提交的任务句柄, 用于查询, 暂停, 恢复, 立即触发和删除任务.

## 最小可运行示例

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

public class ScheduledTaskDemo {

    public static void main(String[] args) {
        ScheduledTask task = ScheduleUtil.scheduler(() -> check())
                .setId("manage-demo")
                .setTrigger(Trigger.interval(3000).build())
                .schedule();

        boolean exists = task.exists();
        task.pause();
        boolean paused = task.isPaused();
        task.resume();
        task.triggerNow();
        boolean deleted = task.delete();

        // TODO: 按业务处理 exists/paused/deleted
    }
}
```

## 常用操作

| 方法                    | 说明               |
|-------------------------|--------------------|
| `exists()`              | 判断任务是否存在   |
| `pause()`               | 暂停任务           |
| `resume()`              | 恢复任务           |
| `triggerNow()`          | 立即触发一次       |
| `delete()`              | 删除任务           |
| `getNextFireTime()`     | 获取下一次触发时间 |
| `getPreviousFireTime()` | 获取上一次触发时间 |

## 常见坑与排查建议

| 异常/消息                 | 原因                   | 解决方式                                        |
|---------------------------|------------------------|-------------------------------------------------|
| `triggerNow()` 后规则没变 | 立即触发不会改变原规则 | 如需改规则, 使用 `ScheduleUtil.reschedule(...)` |
| 删除后仍访问任务          | 句柄对应任务已不存在   | 操作前先用 `exists()` 判断                      |
| 状态不符合预期            | 多处同时管理同一任务   | 统一任务管理入口                                |
