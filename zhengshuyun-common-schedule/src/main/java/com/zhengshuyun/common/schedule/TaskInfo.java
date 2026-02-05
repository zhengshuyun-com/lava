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

import java.time.Instant;

/**
 * 任务信息
 *
 * @author Toint
 * @since 2026/2/5
 */
public class TaskInfo {

    /**
     * 任务状态
     */
    public enum Status {
        /** 等待执行 */
        WAITING,
        /** 已暂停 */
        PAUSED,
        /** 正在执行 */
        RUNNING
    }

    /** 任务 ID */
    private final String taskId;

    /** 任务状态 */
    private final Status status;

    /** 下次执行时间, 可能为 null（已暂停或一次性任务已完成） */
    private final @Nullable Instant nextFireTime;

    /** 上次执行时间, 可能为 null（从未执行过） */
    private final @Nullable Instant previousFireTime;

    public TaskInfo(String taskId, Status status,
                    @Nullable Instant nextFireTime, @Nullable Instant previousFireTime) {
        this.taskId = taskId;
        this.status = status;
        this.nextFireTime = nextFireTime;
        this.previousFireTime = previousFireTime;
    }

    public String getTaskId() {
        return taskId;
    }

    public Status getStatus() {
        return status;
    }

    public @Nullable Instant getNextFireTime() {
        return nextFireTime;
    }

    public @Nullable Instant getPreviousFireTime() {
        return previousFireTime;
    }

    @Override
    public String toString() {
        return "TaskInfo{" +
                "taskId='" + taskId + '\'' +
                ", status=" + status +
                ", nextFireTime=" + nextFireTime +
                ", previousFireTime=" + previousFireTime +
                '}';
    }
}
