/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.common.schedule;

import com.zhengshuyun.common.core.lang.Validate;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 定时任务工具类
 * <p>
 * 提供简单的静态方法创建定时任务, 内部使用 Quartz 调度器 + 虚拟线程执行器
 * <pre>{@code
 * // 每 5 秒执行
 * String id = ScheduleUtil.scheduleEvery(Duration.ofSeconds(5), () -> check());
 *
 * // Cron 表达式
 * String id = ScheduleUtil.scheduleCron("0 0 2 * * ?", () -> backup());
 *
 * // 延迟执行（一次性）
 * String id = ScheduleUtil.scheduleOnce(Duration.ofSeconds(10), () -> init());
 *
 * // 删除任务
 * ScheduleUtil.removeTask(id);
 * }</pre>
 *
 * <p><b>执行器配置</b>：默认使用虚拟线程执行器, 可通过 {@link #initTaskExecutor(ExecutorService)} 自定义
 * <pre>{@code
 * // CPU 密集型任务：使用固定线程池
 * ScheduleUtil.initTaskExecutor(Executors.newFixedThreadPool(8));
 *
 * // IO 密集型任务：使用虚拟线程（默认）
 * ScheduleUtil.initTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
 * }</pre>
 *
 * <p><b>高级用法</b>：需要更多控制（如暂停/恢复/修改任务）时, 使用 {@link #getManager()}
 *
 * @author Toint
 * @since 2026/2/5
 */
public final class ScheduleUtil {

    private ScheduleUtil() {
    }

    // 单例持有

    /**
     * Quartz 调度器（负责触发任务）
     */
    private static volatile Scheduler scheduler;

    /**
     * 任务执行器（负责执行任务, 默认虚拟线程）
     */
    private static volatile ExecutorService taskExecutor;

    /**
     * 标记执行器是否已初始化（无论是手动还是懒加载）
     */
    private static volatile boolean executorInitialized = false;

    static {
        // JVM 关闭时自动清理资源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 关闭调度器
            Scheduler s = scheduler;
            if (s != null) {
                try {
                    s.shutdown(true); // 等待任务完成
                } catch (SchedulerException ignore) {
                }
            }

            // 关闭执行器
            ExecutorService e = taskExecutor;
            if (e != null) {
                e.shutdown();
            }
        }));
    }

    /**
     * 获取 Quartz 调度器（DCL 单例）
     */
    private static Scheduler getScheduler() {
        if (scheduler == null) {
            synchronized (ScheduleUtil.class) {
                if (scheduler == null) {
                    scheduler = createScheduler();
                }
            }
        }
        return scheduler;
    }

    /**
     * 创建 Quartz 调度器
     */
    private static Scheduler createScheduler() {
        try {
            Properties props = new Properties();
            props.setProperty("org.quartz.scheduler.instanceName", "ZhengShuyunScheduler");
            // 线程池类型
            props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            // 调度线程数: 只负责触发, 2 个足够
            props.setProperty("org.quartz.threadPool.threadCount", "2");
            // 内存存储
            props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

            StdSchedulerFactory factory = new StdSchedulerFactory(props);
            Scheduler s = factory.getScheduler();
            s.start();
            return s;
        } catch (SchedulerException e) {
            throw new ScheduleException("创建 Quartz 调度器失败. " + e.getMessage(), e);
        }
    }

    /**
     * 获取任务执行器
     * <p>
     * 包级别可见, 供 ExecutorJob 调用
     */
    static ExecutorService getTaskExecutor() {
        if (taskExecutor == null) {
            synchronized (ScheduleUtil.class) {
                if (taskExecutor == null) {
                    // 默认使用虚拟线程执行器
                    taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
                    executorInitialized = true;
                }
            }
        }
        return taskExecutor;
    }

    // 执行器配置

    /**
     * 自定义任务执行器
     * <p>
     * 默认使用虚拟线程执行器, 可根据任务类型选择合适的执行器：
     * <ul>
     *   <li>IO 密集型：虚拟线程执行器（默认）</li>
     *   <li>CPU 密集型：固定大小线程池</li>
     * </ul>
     * <p>
     * <b>注意：</b>只能初始化一次, 重复调用会抛出 IllegalArgumentException 异常.
     * 必须在首次使用定时任务之前调用.
     *
     * @param executor 自定义执行器
     * @throws IllegalArgumentException 如果 executor 为 null 或已经初始化过
     */
    public static void initTaskExecutor(ExecutorService executor) {
        Validate.notNull(executor, "executor must not be null");
        synchronized (ScheduleUtil.class) {
            Validate.isTrue(!executorInitialized, "TaskExecutor is already initialized");
            taskExecutor = executor;
            executorInitialized = true;
        }
    }

    /**
     * @deprecated 使用 {@link #initTaskExecutor(ExecutorService)} 代替
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public static void setTaskExecutor(ExecutorService executor) {
        initTaskExecutor(executor);
    }

    // 工厂方法

    /**
     * 获取任务管理器
     * <p>
     * 管理器提供完整的任务管理能力, 包括指定任务 ID、暂停、恢复、修改等
     *
     * @return 任务管理器
     */
    public static ScheduleManager getManager() {
        return new ScheduleManager(getScheduler());
    }

    // 简单 API

    /**
     * 添加固定周期任务
     *
     * @param intervalMillis 间隔时间（毫秒）
     * @param task           任务
     * @return 任务 ID
     */
    public static String addFixedRateTask(long intervalMillis, Runnable task) {
        return getManager().addFixedRateTask(intervalMillis, task);
    }

    /**
     * 添加固定周期任务
     *
     * @param interval 间隔时间
     * @param task     任务
     * @return 任务 ID
     */
    public static String addFixedRateTask(Duration interval, Runnable task) {
        return getManager().addFixedRateTask(interval, task);
    }

    /**
     * 添加 Cron 任务
     *
     * @param cron Cron 表达式
     * @param task 任务
     * @return 任务 ID
     */
    public static String addCronTask(String cron, Runnable task) {
        return getManager().addCronTask(cron, task);
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param delayMillis 延迟时间（毫秒）
     * @param task        任务
     * @return 任务 ID
     */
    public static String addDelayedTask(long delayMillis, Runnable task) {
        return getManager().addDelayedTask(delayMillis, task);
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param delay 延迟时间
     * @param task  任务
     * @return 任务 ID
     */
    public static String addDelayedTask(Duration delay, Runnable task) {
        return getManager().addDelayedTask(delay, task);
    }

    /**
     * 删除任务
     *
     * @param taskId 任务 ID
     */
    public static void removeTask(String taskId) {
        getManager().removeTask(taskId);
    }
}
