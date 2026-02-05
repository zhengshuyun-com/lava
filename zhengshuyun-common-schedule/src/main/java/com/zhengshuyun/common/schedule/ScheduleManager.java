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
import com.zhengshuyun.common.core.time.ZoneIds;
import org.quartz.*;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.matchers.GroupMatcher;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 定时任务管理器
 * <p>
 * 提供完整的任务管理能力, 包括添加、删除、修改、暂停、恢复等操作
 * <pre>{@code
 * ScheduleManager manager = ScheduleUtil.manager();
 *
 * // 添加 Cron 任务
 * String taskId = manager.addCronTask("0 0 2 * * ?", () -> backup());
 *
 * // 指定任务 ID
 * manager.addCronTask("daily-backup", "0 0 2 * * ?", () -> backup());
 *
 * // 动态管理
 * manager.pauseTask(taskId);
 * manager.resumeTask(taskId);
 * manager.removeTask(taskId);
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/5
 */
public class ScheduleManager {

    private final Scheduler scheduler;

    /**
     * 包级别构造器, 由 ScheduleUtil.manager() 创建
     */
    ScheduleManager(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    // Cron 任务

    /**
     * 添加 Cron 任务
     *
     * @param cron Cron 表达式
     * @param task 任务
     * @return 自动生成的任务 ID
     */
    public String addCronTask(String cron, Runnable task) {
        return addCronTask(generateTaskId(), cron, task, TaskConfig.defaults());
    }

    /**
     * 添加 Cron 任务
     *
     * @param cron   Cron 表达式
     * @param task   任务
     * @param config 任务配置
     * @return 自动生成的任务 ID
     */
    public String addCronTask(String cron, Runnable task, TaskConfig config) {
        return addCronTask(generateTaskId(), cron, task, config);
    }

    /**
     * 添加 Cron 任务
     *
     * @param taskId 任务 ID
     * @param cron   Cron 表达式
     * @param task   任务
     * @return 任务 ID
     */
    public String addCronTask(String taskId, String cron, Runnable task) {
        return addCronTask(taskId, cron, task, TaskConfig.defaults());
    }

    /**
     * 添加 Cron 任务
     *
     * @param taskId 任务 ID
     * @param cron   Cron 表达式
     * @param task   任务
     * @param config 任务配置
     * @return 任务 ID
     */
    public String addCronTask(String taskId, String cron, Runnable task, TaskConfig config) {
        validateTaskId(taskId);
        Validate.notBlank(cron, "cron must not be blank");
        Validate.notNull(task, "task must not be null");
        Validate.notNull(config, "config must not be null");

        try {
            // 创建任务包装器
            TaskWrapper wrapper = new TaskWrapper(task, config);

            // 创建 JobDataMap
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(ExecutorJob.TASK_WRAPPER_KEY, wrapper);

            // 创建 JobDetail
            JobDetail jobDetail = JobBuilder.newJob(ExecutorJob.class)
                    .withIdentity(taskId)
                    .usingJobData(jobDataMap)
                    .build();

            // 创建 Cron 触发器（默认使用 UTC 时区）
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(taskId)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .inTimeZone(TimeZone.getTimeZone(ZoneIds.UTC)))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            return taskId;
        } catch (SchedulerException e) {
            throw new ScheduleException("添加 Cron 任务失败: " + taskId, e);
        }
    }

    // 固定周期任务

    /**
     * 添加固定周期任务
     *
     * @param intervalMillis 间隔时间（毫秒）
     * @param task           任务
     * @return 自动生成的任务 ID
     */
    public String addFixedRateTask(long intervalMillis, Runnable task) {
        return addFixedRateTask(generateTaskId(), intervalMillis, task, TaskConfig.defaults());
    }

    /**
     * 添加固定周期任务
     *
     * @param interval 间隔时间
     * @param task     任务
     * @return 自动生成的任务 ID
     */
    public String addFixedRateTask(Duration interval, Runnable task) {
        return addFixedRateTask(generateTaskId(), interval, task, TaskConfig.defaults());
    }

    /**
     * 添加固定周期任务
     *
     * @param intervalMillis 间隔时间（毫秒）
     * @param task           任务
     * @param config         任务配置
     * @return 自动生成的任务 ID
     */
    public String addFixedRateTask(long intervalMillis, Runnable task, TaskConfig config) {
        return addFixedRateTask(generateTaskId(), intervalMillis, task, config);
    }

    /**
     * 添加固定周期任务
     *
     * @param interval 间隔时间
     * @param task     任务
     * @param config   任务配置
     * @return 自动生成的任务 ID
     */
    public String addFixedRateTask(Duration interval, Runnable task, TaskConfig config) {
        return addFixedRateTask(generateTaskId(), interval, task, config);
    }

    /**
     * 添加固定周期任务
     *
     * @param taskId         任务 ID
     * @param intervalMillis 间隔时间（毫秒）
     * @param task           任务
     * @return 任务 ID
     */
    public String addFixedRateTask(String taskId, long intervalMillis, Runnable task) {
        return addFixedRateTask(taskId, intervalMillis, task, TaskConfig.defaults());
    }

    /**
     * 添加固定周期任务
     *
     * @param taskId   任务 ID
     * @param interval 间隔时间
     * @param task     任务
     * @return 任务 ID
     */
    public String addFixedRateTask(String taskId, Duration interval, Runnable task) {
        return addFixedRateTask(taskId, interval, task, TaskConfig.defaults());
    }

    /**
     * 添加固定周期任务
     *
     * @param taskId         任务 ID
     * @param intervalMillis 间隔时间（毫秒）
     * @param task           任务
     * @param config         任务配置
     * @return 任务 ID
     */
    public String addFixedRateTask(String taskId, long intervalMillis, Runnable task, TaskConfig config) {
        validateTaskId(taskId);
        validateInterval(intervalMillis);
        Validate.notNull(task, "task must not be null");
        Validate.notNull(config, "config must not be null");

        try {
            TaskWrapper wrapper = new TaskWrapper(task, config);

            // 创建 JobDataMap
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(ExecutorJob.TASK_WRAPPER_KEY, wrapper);

            JobDetail jobDetail = JobBuilder.newJob(ExecutorJob.class)
                    .withIdentity(taskId)
                    .usingJobData(jobDataMap)
                    .build();

            // 使用 SimpleScheduleBuilder 创建固定周期触发器
            SimpleTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(taskId)
                    .startNow() // 立即开始
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(intervalMillis)
                            .repeatForever())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            return taskId;
        } catch (SchedulerException e) {
            throw new ScheduleException("添加固定周期任务失败: " + taskId, e);
        }
    }

    /**
     * 添加固定周期任务
     *
     * @param taskId   任务 ID
     * @param interval 间隔时间
     * @param task     任务
     * @param config   任务配置
     * @return 任务 ID
     */
    public String addFixedRateTask(String taskId, Duration interval, Runnable task, TaskConfig config) {
        validateInterval(interval);
        return addFixedRateTask(taskId, interval.toMillis(), task, config);
    }

    // 延迟任务（一次性）

    /**
     * 添加延迟任务（一次性）
     *
     * @param delayMillis 延迟时间（毫秒）
     * @param task        任务
     * @return 自动生成的任务 ID
     */
    public String addDelayedTask(long delayMillis, Runnable task) {
        return addDelayedTask(generateTaskId(), delayMillis, task, TaskConfig.defaults());
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param delay 延迟时间
     * @param task  任务
     * @return 自动生成的任务 ID
     */
    public String addDelayedTask(Duration delay, Runnable task) {
        return addDelayedTask(generateTaskId(), delay, task, TaskConfig.defaults());
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param delayMillis 延迟时间（毫秒）
     * @param task        任务
     * @param config      任务配置
     * @return 自动生成的任务 ID
     */
    public String addDelayedTask(long delayMillis, Runnable task, TaskConfig config) {
        return addDelayedTask(generateTaskId(), delayMillis, task, config);
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param delay  延迟时间
     * @param task   任务
     * @param config 任务配置
     * @return 自动生成的任务 ID
     */
    public String addDelayedTask(Duration delay, Runnable task, TaskConfig config) {
        return addDelayedTask(generateTaskId(), delay, task, config);
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param taskId      任务 ID
     * @param delayMillis 延迟时间（毫秒）
     * @param task        任务
     * @return 任务 ID
     */
    public String addDelayedTask(String taskId, long delayMillis, Runnable task) {
        return addDelayedTask(taskId, delayMillis, task, TaskConfig.defaults());
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param taskId 任务 ID
     * @param delay  延迟时间
     * @param task   任务
     * @return 任务 ID
     */
    public String addDelayedTask(String taskId, Duration delay, Runnable task) {
        return addDelayedTask(taskId, delay, task, TaskConfig.defaults());
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param taskId      任务 ID
     * @param delayMillis 延迟时间（毫秒）
     * @param task        任务
     * @param config      任务配置
     * @return 任务 ID
     */
    public String addDelayedTask(String taskId, long delayMillis, Runnable task, TaskConfig config) {
        validateTaskId(taskId);
        validateDelay(delayMillis);
        Validate.notNull(task, "task must not be null");
        Validate.notNull(config, "config must not be null");

        try {
            TaskWrapper wrapper = new TaskWrapper(task, config);

            // 创建 JobDataMap
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(ExecutorJob.TASK_WRAPPER_KEY, wrapper);

            JobDetail jobDetail = JobBuilder.newJob(ExecutorJob.class)
                    .withIdentity(taskId)
                    .usingJobData(jobDataMap)
                    .build();

            // 计算触发时间
            Date startTime = new Date(System.currentTimeMillis() + delayMillis);

            // 一次性触发器
            SimpleTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(taskId)
                    .startAt(startTime)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withRepeatCount(0)) // 不重复
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            return taskId;
        } catch (SchedulerException e) {
            throw new ScheduleException("添加延迟任务失败: " + taskId, e);
        }
    }

    /**
     * 添加延迟任务（一次性）
     *
     * @param taskId 任务 ID
     * @param delay  延迟时间
     * @param task   任务
     * @param config 任务配置
     * @return 任务 ID
     */
    public String addDelayedTask(String taskId, Duration delay, Runnable task, TaskConfig config) {
        validateDelay(delay);
        return addDelayedTask(taskId, delay.toMillis(), task, config);
    }

    // 任务管理

    /**
     * 删除任务
     *
     * @param taskId 任务 ID
     */
    public void removeTask(String taskId) {
        validateTaskId(taskId);
        try {
            JobKey jobKey = JobKey.jobKey(taskId);
            if (!scheduler.deleteJob(jobKey)) {
                throw new ScheduleException("任务不存在: " + taskId);
            }
        } catch (SchedulerException e) {
            throw new ScheduleException("删除任务失败: " + taskId, e);
        }
    }

    /**
     * 暂停任务
     *
     * @param taskId 任务 ID
     */
    public void pauseTask(String taskId) {
        validateTaskId(taskId);
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(taskId);
            scheduler.pauseTrigger(triggerKey);
        } catch (SchedulerException e) {
            throw new ScheduleException("暂停任务失败: " + taskId, e);
        }
    }

    /**
     * 恢复任务
     *
     * @param taskId 任务 ID
     */
    public void resumeTask(String taskId) {
        validateTaskId(taskId);
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(taskId);
            scheduler.resumeTrigger(triggerKey);
        } catch (SchedulerException e) {
            throw new ScheduleException("恢复任务失败: " + taskId, e);
        }
    }

    /**
     * 立即触发任务
     *
     * @param taskId 任务 ID
     */
    public void triggerNow(String taskId) {
        validateTaskId(taskId);
        try {
            JobKey jobKey = JobKey.jobKey(taskId);
            scheduler.triggerJob(jobKey);
        } catch (SchedulerException e) {
            throw new ScheduleException("立即触发任务失败: " + taskId, e);
        }
    }

    /**
     * 修改 Cron 表达式
     *
     * @param taskId  任务 ID
     * @param newCron 新的 Cron 表达式
     */
    public void updateCronTask(String taskId, String newCron) {
        validateTaskId(taskId);
        Validate.notBlank(newCron, "newCron must not be blank");
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(taskId);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                throw new ScheduleException("任务不存在: " + taskId);
            }

            // 类型安全检查
            if (!(trigger instanceof CronTrigger)) {
                throw new ScheduleException("任务不是 Cron 任务: " + taskId +
                        " (实际类型: " + trigger.getClass().getSimpleName() + ")");
            }

            CronTrigger oldTrigger = (CronTrigger) trigger;

            // 创建新的触发器（保持 UTC 时区）
            CronTrigger newTrigger = oldTrigger.getTriggerBuilder()
                    .withSchedule(CronScheduleBuilder.cronSchedule(newCron)
                            .inTimeZone(TimeZone.getTimeZone(ZoneIds.UTC)))
                    .build();

            scheduler.rescheduleJob(triggerKey, newTrigger);
        } catch (SchedulerException e) {
            throw new ScheduleException("修改 Cron 任务失败: " + taskId, e);
        }
    }

    /**
     * 获取任务信息
     *
     * @param taskId 任务 ID
     * @return 任务信息
     */
    public TaskInfo getTask(String taskId) {
        validateTaskId(taskId);
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(taskId);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                throw new ScheduleException("任务不存在: " + taskId);
            }
            return buildTaskInfo(taskId, trigger);
        } catch (SchedulerException e) {
            throw new ScheduleException("获取任务信息失败: " + taskId, e);
        }
    }

    /**
     * 列出所有任务
     *
     * @return 任务列表
     */
    public List<TaskInfo> listAllTasks() {
        try {
            List<TaskInfo> result = new ArrayList<>();
            // 显式使用 GroupMatcher.anyJobGroup() 而非 null
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyJobGroup())) {
                // 获取该 Job 的所有 Trigger（支持未来扩展多 Trigger 场景）
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (!triggers.isEmpty()) {
                    // 当前实现：一个 Job 只有一个 Trigger，取第一个
                    Trigger trigger = triggers.get(0);
                    result.add(buildTaskInfo(jobKey.getName(), trigger));
                }
            }
            return result;
        } catch (SchedulerException e) {
            throw new ScheduleException("列出所有任务失败", e);
        }
    }

    // 私有方法

    /**
     * 生成任务 ID
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 校验任务 ID
     */
    private void validateTaskId(String taskId) {
        Validate.notBlank(taskId, "taskId must not be blank");
    }

    /**
     * 校验间隔时间（毫秒）
     */
    private void validateInterval(long intervalMillis) {
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("intervalMillis must be > 0, got: " + intervalMillis);
        }
    }

    /**
     * 校验间隔时间（Duration）
     */
    private void validateInterval(Duration interval) {
        Validate.notNull(interval, "interval must not be null");
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval must be positive, got: " + interval);
        }
        // 统一校验：必须至少 1 毫秒（避免 toMillis() 返回 0 的情况）
        if (interval.toMillis() < 1) {
            throw new IllegalArgumentException("interval must be at least 1 millisecond, got: " + interval);
        }
    }

    /**
     * 校验延迟时间（毫秒）
     */
    private void validateDelay(long delayMillis) {
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("delayMillis must be > 0, got: " + delayMillis);
        }
    }

    /**
     * 校验延迟时间（Duration）
     */
    private void validateDelay(Duration delay) {
        Validate.notNull(delay, "delay must not be null");
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("delay must be positive, got: " + delay);
        }
        // 统一校验：必须至少 1 毫秒（避免 toMillis() 返回 0 的情况）
        if (delay.toMillis() < 1) {
            throw new IllegalArgumentException("delay must be at least 1 millisecond, got: " + delay);
        }
    }

    /**
     * 构建任务信息
     */
    private TaskInfo buildTaskInfo(String taskId, Trigger trigger) throws SchedulerException {
        TriggerState state = scheduler.getTriggerState(trigger.getKey());
        TaskInfo.Status status = switch (state) {
            case PAUSED -> TaskInfo.Status.PAUSED;
            // 移除 RUNNING 状态：任务实际在外部执行器执行，Quartz 状态不能准确反映
            default -> TaskInfo.Status.WAITING;
        };

        Date nextFire = trigger.getNextFireTime();
        Date prevFire = trigger.getPreviousFireTime();

        return new TaskInfo(
                taskId,
                status,
                nextFire != null ? nextFire.toInstant() : null,
                prevFire != null ? prevFire.toInstant() : null
        );
    }
}
