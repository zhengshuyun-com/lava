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

import com.zhengshuyun.common.core.id.IdUtil;
import com.zhengshuyun.common.core.lang.Validate;
import org.quartz.*;

/**
 * 链式任务构建器
 * <p>
 * 通过 {@link ScheduleUtil#taskBuilder(Runnable)} 创建
 * <pre>{@code
 * ScheduledTask task = ScheduleUtil.taskBuilder(() -> doWork())
 *     .setId("my-task")
 *     .setTrigger(Trigger.builder().setInterval(5000).build())
 *     .schedule();
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/6
 */
public class TaskBuilder {

    private final Runnable task;
    private String id;
    private Trigger trigger;

    TaskBuilder(Runnable task) {
        this.task = task;
    }

    /**
     * 设置任务 ID
     * <p>
     * 可选, 不调用则自动生成 UUID
     *
     * @param id 任务 ID
     * @return this
     */
    public TaskBuilder setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * 设置触发器
     *
     * @param trigger 触发器
     * @return this
     */
    public TaskBuilder setTrigger(Trigger trigger) {
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

        String taskId = (id != null && !id.isBlank()) ? id : IdUtil.randomUUIDWithoutDash();
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
}
