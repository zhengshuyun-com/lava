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

import com.google.common.base.Strings;
import com.zhengshuyun.common.core.id.IdUtil;
import com.zhengshuyun.common.core.lang.Validate;
import org.quartz.*;

/**
 * 任务调度器
 * <p>
 * 通过 {@link Builder} 构建并调度任务
 * <pre>{@code
 * ScheduledTask task = TaskScheduler.builder(() -> doWork())
 *     .setId("my-task")
 *     .setTrigger(Trigger.interval(5000).build())
 *     .schedule();
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/6
 */
public final class TaskScheduler {

    private TaskScheduler() {
    }

    /**
     * 创建任务构建器
     *
     * @param task 任务
     * @return 构建器
     */
    public static Builder builder(Runnable task) {
        Validate.notNull(task, "task must not be null");
        return new Builder(task);
    }

    /**
     * 任务构建器
     * <p>
     * 通过 {@link TaskScheduler#builder(Runnable)} 或 {@link ScheduleUtil#scheduler(Runnable)} 创建
     *
     * @author Toint
     * @since 2026/2/6
     */
    public static final class Builder {

        /**
         * 任务逻辑
         */
        private final Runnable task;

        /**
         * 任务 ID(可选, schedule 时自动生成)
         */
        private String id;

        /**
         * 触发器(必填)
         */
        private Trigger trigger;

        Builder(Runnable task) {
            Validate.notNull(task, "task must not be null");
            this.task = task;
        }

        /**
         * 设置任务 ID
         * <p>
         * 可选, 不调用则自动生成 UUID, 不允许为空
         *
         * @param id 任务 ID
         * @return this
         */
        public Builder setId(String id) {
            Validate.notBlank(id, "id must not be blank");
            this.id = id;
            return this;
        }

        /**
         * 设置触发器
         *
         * @param trigger 触发器
         * @return this
         */
        public Builder setTrigger(Trigger trigger) {
            Validate.notNull(trigger, "trigger must not be null");
            this.trigger = trigger;
            return this;
        }

        /**
         * 提交任务到调度器
         *
         * @return 已调度任务的句柄
         */
        public ScheduledTask schedule() {
            Validate.notNull(trigger, "trigger must not be null, call setTrigger() first");

            String taskId = getOrCreateTaskId();
            Scheduler scheduler = ScheduleUtil.getScheduler();

            try {
                TaskWrapper wrapper = new TaskWrapper(task);

                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put(ExecutorJob.TASK_WRAPPER_KEY, wrapper);

                JobDetail jobDetail = JobBuilder.newJob(ExecutorJob.class)
                        .withIdentity(taskId)
                        .usingJobData(jobDataMap)
                        .build();

                org.quartz.Trigger quartzTrigger = trigger.toQuartzTrigger(taskId);
                scheduler.scheduleJob(jobDetail, quartzTrigger);

                return new ScheduledTask(taskId, scheduler);
            } catch (SchedulerException e) {
                throw new ScheduleException("调度任务失败: " + taskId, e);
            }
        }

        private String getOrCreateTaskId() {
            if (Strings.nullToEmpty(id).isBlank()) {
                id = IdUtil.randomUUIDWithoutDash();
            }
            return id;
        }
    }
}
