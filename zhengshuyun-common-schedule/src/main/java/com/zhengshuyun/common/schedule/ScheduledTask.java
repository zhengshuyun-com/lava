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

import org.jspecify.annotations.Nullable;
import org.quartz.*;
import org.quartz.Trigger.TriggerState;

import java.util.Date;

/**
 * 已调度任务的句柄
 * <p>
 * 提供任务的生命周期管理和状态查询
 * <pre>{@code
 * ScheduledTask task = ScheduleUtil.taskBuilder(() -> check())
 *     .setId("health-check")
 *     .setTrigger(Trigger.builder().setInterval(5000).build())
 *     .schedule();
 *
 * task.pause();
 * task.resume();
 * task.delete();
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/6
 */
public class ScheduledTask {

    /**
     * 任务 ID
     */
    private final String id;

    /**
     * Quartz 调度器
     */
    private final Scheduler scheduler;

    ScheduledTask(String id, Scheduler scheduler) {
        this.id = id;
        this.scheduler = scheduler;
    }

    /**
     * 获取任务 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 暂停任务
     */
    public void pause() {
        try {
            scheduler.pauseTrigger(TriggerKey.triggerKey(id));
        } catch (SchedulerException e) {
            throw new ScheduleException("暂停任务失败: " + id, e);
        }
    }

    /**
     * 恢复任务
     */
    public void resume() {
        try {
            scheduler.resumeTrigger(TriggerKey.triggerKey(id));
        } catch (SchedulerException e) {
            throw new ScheduleException("恢复任务失败: " + id, e);
        }
    }

    /**
     * 删除任务
     * <p>
     * 幂等操作, 任务不存在时返回 false
     *
     * @return true 表示任务存在并已删除, false 表示任务不存在
     */
    public boolean delete() {
        try {
            return scheduler.deleteJob(JobKey.jobKey(id));
        } catch (SchedulerException e) {
            throw new ScheduleException("删除任务失败: " + id, e);
        }
    }

    /**
     * 任务是否存在于调度器中
     */
    public boolean exists() {
        try {
            return scheduler.checkExists(JobKey.jobKey(id));
        } catch (SchedulerException e) {
            throw new ScheduleException("查询任务是否存在失败: " + id, e);
        }
    }

    /**
     * 立即触发一次
     */
    public void triggerNow() {
        try {
            scheduler.triggerJob(JobKey.jobKey(id));
        } catch (SchedulerException e) {
            throw new ScheduleException("立即触发任务失败: " + id, e);
        }
    }

    /**
     * 是否已暂停
     */
    public boolean isPaused() {
        try {
            TriggerState state = scheduler.getTriggerState(TriggerKey.triggerKey(id));
            return state == TriggerState.PAUSED;
        } catch (SchedulerException e) {
            throw new ScheduleException("查询任务状态失败: " + id, e);
        }
    }

    /**
     * 获取下次执行时间(毫秒时间戳)
     *
     * @return 毫秒时间戳, 无下次执行时返回 null
     */
    public @Nullable Long getNextFireTime() {
        try {
            org.quartz.Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(id));
            if (trigger == null) {
                return null;
            }
            Date next = trigger.getNextFireTime();
            return next != null ? next.getTime() : null;
        } catch (SchedulerException e) {
            throw new ScheduleException("查询下次执行时间失败: " + id, e);
        }
    }

    /**
     * 获取上次执行时间(毫秒时间戳)
     *
     * @return 毫秒时间戳, 从未执行过时返回 null
     */
    public @Nullable Long getPreviousFireTime() {
        try {
            org.quartz.Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(id));
            if (trigger == null) {
                return null;
            }
            Date prev = trigger.getPreviousFireTime();
            return prev != null ? prev.getTime() : null;
        } catch (SchedulerException e) {
            throw new ScheduleException("查询上次执行时间失败: " + id, e);
        }
    }
}
