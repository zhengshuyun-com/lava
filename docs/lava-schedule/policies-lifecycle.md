# 并发、Misfire 与生命周期

## 并发策略

| 策略 | 行为 |
| --- | --- |
| `ConcurrencyPolicy.SERIAL_SKIP` | 前一次仍运行时跳过，不重叠、不排队 |
| `serialQueue(maxPending)` | 单任务串行，使用有界待执行队列 |
| `parallel(maxConcurrency, maxPending)` | 有界并发和有界待执行队列 |

默认 `ScheduleOptions.DEFAULT` 使用 `SERIAL_SKIP + SKIP`。

队列满或底层 executor 拒绝时产生 `REJECTED` 事件，不创建无界虚拟线程。

## Misfire

| 策略 | 行为 |
| --- | --- |
| `SKIP` | 丢弃已经错过的 occurrence |
| `FIRE_ONCE` | 无论错过多少次，只补一次 |
| `CATCH_UP` | 把错过的 occurrence 交给当前有界并发策略 |

`CATCH_UP` 不代表无界补偿。最终仍受任务队列和调度器执行边界限制，容量不足时产生跳过或拒绝事件。

## 任务事件

监听器接收四种终态：

- `SUCCESS`
- `FAILURE`
- `SKIPPED`
- `REJECTED`

```java
LavaScheduler scheduler = LavaScheduler.builder()
        .listener(event -> metrics.record(
                event.taskId(),
                event.status(),
                event.scheduledAt(),
                event.startedAt(),
                event.completedAt(),
                event.reason()
        ))
        .build();
```

监听器异常会被隔离，不中断调度器。

## 资源所有权

- 每个调度器始终拥有一个协调线程；
- 默认 worker 是调度器拥有的有界虚拟线程 executor；
- `.executor(executor)` 传入的执行器属于借用资源，不会被关闭；
- `close()` 停止新的 occurrence，并按配置超时等待活跃任务；
- 超时后会取消本调度器提交的任务，但不会关闭外部 executor；
- `close(Duration)` 返回是否在超时前完成全部活跃执行；
- 等待被中断时恢复中断标志并返回 `false`。

进程崩溃后，内存中的任务和执行事件都会丢失。
