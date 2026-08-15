# 任务执行器配置

`lava-schedule` 默认使用虚拟线程执行任务. 如需特殊线程池, 可以在首次创建任务前初始化任务执行器.

## 最小可运行示例

```java
import com.zhengshuyun.lava.schedule.ScheduleUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutorDemo {

    public static void main(String[] args) {
        ExecutorService fixedPool = Executors.newFixedThreadPool(8);
        ScheduleUtil.initTaskExecutor(fixedPool);

        // TODO: 后续再创建调度任务
    }
}
```

- `initTaskExecutor(...)` 只能初始化一次.
- 需要在首次创建任务前完成初始化.
- CPU 密集任务可以考虑固定线程池.

## 常见坑与排查建议

| 异常/消息      | 原因                             | 解决方式                     |
|----------------|----------------------------------|------------------------------|
| 重复初始化失败 | 多处调用 `initTaskExecutor(...)` | 只在应用启动阶段调用一次     |
| 传入 `null`    | 执行器为空                       | 确保执行器对象已创建         |
| 任务堆积       | 线程池容量不足或任务阻塞         | 根据任务类型调整线程池和监控 |
