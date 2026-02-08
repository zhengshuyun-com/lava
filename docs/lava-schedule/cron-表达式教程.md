# Cron 表达式教程

本教程面向需要在 `lava-schedule` 中编写和评审 Cron 的开发者, 重点覆盖可直接落地的表达式写法.

## 先记住 3 个关键点

- `lava-schedule` 使用 Quartz Cron 语法, 常见 6 段, 可选第 7 段(年).
- 模块默认按 UTC 解释 Cron, 不是机器本地时区.
- 日字段和周字段通常二选一, 另一位写 `?`.

## 核心流程

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;
import com.zhengshuyun.lava.schedule.ScheduledTask;
import com.zhengshuyun.lava.schedule.Trigger;

public class CronQuickStartDemo {

    public static void main(String[] args) {
        // 1) 定义任务逻辑
        Runnable backupTask = () -> {
            // TODO: 执行备份逻辑
        };

        // 2) 创建 Cron 任务(每天 UTC 02:00)
        ScheduledTask task = ScheduleUtil.scheduler(backupTask)
                .setId("daily-backup")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();

        // 3) 按需管理任务生命周期
        boolean exists = task.exists();

        // TODO: 按业务处理 exists, 或在合适时机执行 task.pause()/task.resume()/task.delete()
    }
}
```

- `Trigger.cron(...).build()`: 创建不可变触发器.
- `0 0 2 * * ?`: 表示每天 UTC `02:00`.

## Quartz 字段速记

| 位置 | 字段 | 允许值 | 示例 |
|------|------|--------|------|
| 1 | 秒 | `0-59` | `0` |
| 2 | 分 | `0-59` | `30` |
| 3 | 时 | `0-23` | `2` |
| 4 | 日 | `1-31` | `15` |
| 5 | 月 | `1-12` 或 `JAN-DEC` | `*` |
| 6 | 周 | `1-7` 或 `SUN-SAT` | `?` |
| 7(可选) | 年 | `1970-2099` | `2026` |

## 可以直接复用的表达式

| 场景 | 表达式 | 说明 |
|------|--------|------|
| 每 10 秒 | `0/10 * * * * ?` | 秒字段步长 |
| 每 5 分钟 | `0 */5 * * * ?` | 分字段步长 |
| 每天 UTC 02:00 | `0 0 2 * * ?` | 日常离线任务常用 |
| 每周一 09:00 | `0 0 9 ? * MON` | 周维度调度 |
| 每月 1 号 00:00 | `0 0 0 1 * ?` | 月初任务 |
| 工作日 18:30 | `0 30 18 ? * MON-FRI` | 工作日规则 |

## 时区换算示例

如果业务要求 "北京时间每天 02:00" 触发, 因默认按 UTC 解释, 需要写成前一天 UTC `18:00`:

- 业务时间: `UTC+8 02:00`.
- 对应 UTC: 前一天 `18:00`.
- 表达式: `0 0 18 * * ?`.

建议团队统一约定 "Cron 一律按 UTC 评审".

## 常见错误与排查

- 表达式写错导致调度阶段抛异常, 建议把关键 Cron 放入单元测试.
- 把本地时区表达式直接用于线上, 会造成固定偏移.
- 同时设置了日字段和周字段具体值, 导致触发行为不符合预期.
- 忘记设置任务 ID 唯一性, 任务重复创建时会调度失败.
