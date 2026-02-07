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

package com.zhengshuyun.lava.schedule;

import com.google.common.collect.ImmutableList;
import com.zhengshuyun.lava.core.lang.Validate;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 定时任务工具类
 * <p>
 * 提供 Builder 风格的 API 创建和管理定时任务, 内部使用 Quartz 调度器 + 虚拟线程执行器
 * <pre>{@code
 * // 固定间隔
 * ScheduledTask task = ScheduleUtil.scheduler(() -> check())
 *     .setId("health-check")
 *     .setTrigger(Trigger.interval(5000).initialDelay(1000).build())
 *     .schedule();
 *
 * // Cron
 * ScheduleUtil.scheduler(() -> backup())
 *     .setId("daily-backup")
 *     .setTrigger(Trigger.cron("0 0 2 * * ?").build())
 *     .schedule();
 *
 * // 延迟一次
 * ScheduleUtil.scheduler(() -> init())
 *     .setTrigger(Trigger.delay(10000).build())
 *     .schedule();
 *
 * // 管理任务
 * task.pause();
 * task.resume();
 * task.delete();
 * ScheduleUtil.deleteTask("health-check");
 * }</pre>
 *
 * <p><b>执行器配置</b>: 默认使用虚拟线程执行器, 可通过 {@link #initTaskExecutor(ExecutorService)} 自定义
 *
 * @author Toint
 * @since 2026/2/5
 */
public final class ScheduleUtil {

    private ScheduleUtil() {
    }

    /**
     * Quartz 调度器(负责触发任务)
     */
    private static volatile Scheduler scheduler;

    /**
     * 任务执行器(负责执行任务, 默认虚拟线程)
     */
    private static volatile ExecutorService taskExecutor;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Scheduler s = scheduler;
            if (s != null) {
                try {
                    s.shutdown(true);
                } catch (SchedulerException ignore) {
                }
            }

            ExecutorService e = taskExecutor;
            if (e != null) {
                e.shutdown();
            }
        }));
    }

    /**
     * 获取 Quartz 调度器(DCL 单例)
     */
    static Scheduler getScheduler() {
        if (scheduler == null) {
            synchronized (ScheduleUtil.class) {
                if (scheduler == null) {
                    scheduler = createScheduler();
                }
            }
        }
        return scheduler;
    }

    private static Scheduler createScheduler() {
        try {
            Properties props = new Properties();
            props.setProperty("org.quartz.scheduler.instanceName", "ZhengShuyunScheduler");
            props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            props.setProperty("org.quartz.threadPool.threadCount", "2");
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
                    taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
                }
            }
        }
        return taskExecutor;
    }

    /**
     * 自定义任务执行器
     * <p>
     * 默认使用虚拟线程执行器, 可根据任务类型选择合适的执行器:
     * <ul>
     *   <li>IO 密集型: 虚拟线程执行器(默认)</li>
     *   <li>CPU 密集型: 固定大小线程池</li>
     * </ul>
     * <p>
     * <b>注意:</b>只能初始化一次, 重复调用会抛出 IllegalArgumentException 异常.
     * 必须在首次使用定时任务之前调用.
     *
     * @param executor 自定义执行器
     * @throws IllegalArgumentException 如果 executor 为 null 或已经初始化过
     */
    public static void initTaskExecutor(ExecutorService executor) {
        Validate.notNull(executor, "executor must not be null");
        synchronized (ScheduleUtil.class) {
            Validate.isNull(taskExecutor, "TaskExecutor is already initialized");
            taskExecutor = executor;
        }
    }

    // 任务创建

    /**
     * 创建任务构建器
     *
     * @param task 任务
     * @return 任务构建器
     */
    public static TaskScheduler.Builder scheduler(Runnable task) {
        return TaskScheduler.builder(task);
    }

    // 任务管理

    /**
     * 删除任务
     * <p>
     * 幂等操作, 任务不存在时返回 false
     *
     * @param taskId 任务 ID
     * @return true 表示任务存在并已删除, false 表示任务不存在
     */
    public static boolean deleteTask(String taskId) {
        Validate.notBlank(taskId, "taskId must not be blank");
        try {
            return getScheduler().deleteJob(JobKey.jobKey(taskId));
        } catch (SchedulerException e) {
            throw new ScheduleException("删除任务失败: " + taskId, e);
        }
    }

    /**
     * 任务是否存在
     *
     * @param taskId 任务 ID
     * @return 存在返回 true
     */
    public static boolean hasTask(String taskId) {
        Validate.notBlank(taskId, "taskId must not be blank");
        try {
            return getScheduler().checkExists(JobKey.jobKey(taskId));
        } catch (SchedulerException e) {
            throw new ScheduleException("查询任务是否存在失败: " + taskId, e);
        }
    }

    /**
     * 重新调度任务
     *
     * @param taskId  任务 ID
     * @param trigger 新的触发器
     */
    public static void reschedule(String taskId, Trigger trigger) {
        Validate.notBlank(taskId, "taskId must not be blank");
        Validate.notNull(trigger, "trigger must not be null");
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(taskId);
            org.quartz.Trigger oldTrigger = getScheduler().getTrigger(triggerKey);
            if (oldTrigger == null) {
                throw new ScheduleException("任务不存在: " + taskId);
            }
            org.quartz.Trigger newQuartzTrigger = trigger.toQuartzTrigger(taskId);
            getScheduler().rescheduleJob(triggerKey, newQuartzTrigger);
        } catch (SchedulerException e) {
            throw new ScheduleException("重新调度任务失败: " + taskId, e);
        }
    }

    // 任务查询

    /**
     * 获取任务句柄
     *
     * @param taskId 任务 ID
     * @return 任务句柄
     * @throws ScheduleException 任务不存在时
     */
    public static ScheduledTask getTask(String taskId) {
        Validate.notBlank(taskId, "taskId must not be blank");
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(taskId);
            if (getScheduler().getTrigger(triggerKey) == null) {
                throw new ScheduleException("任务不存在: " + taskId);
            }
            return new ScheduledTask(taskId, getScheduler());
        } catch (SchedulerException e) {
            throw new ScheduleException("获取任务失败: " + taskId, e);
        }
    }

    /**
     * 获取所有任务
     *
     * @return 不可变任务列表(快照)
     */
    public static List<ScheduledTask> getAllTasks() {
        try {
            Scheduler s = getScheduler();
            ImmutableList.Builder<ScheduledTask> result = ImmutableList.builder();
            for (JobKey jobKey : s.getJobKeys(GroupMatcher.anyJobGroup())) {
                result.add(new ScheduledTask(jobKey.getName(), s));
            }
            return result.build();
        } catch (SchedulerException e) {
            throw new ScheduleException("获取所有任务失败", e);
        }
    }

    /**
     * 获取底层 Quartz Scheduler
     *
     * @return Quartz Scheduler
     */
    public static Scheduler getQuartzScheduler() {
        return getScheduler();
    }
}
