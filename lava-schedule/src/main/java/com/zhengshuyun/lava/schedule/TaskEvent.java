/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.schedule;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 单次任务执行的不可变结构化终态事件。
 *
 * <p>对于 {@link TaskEventStatus#SUCCESS} 和 {@link TaskEventStatus#FAILURE}，
 * {@code startedAt} 必须存在；被跳过或拒绝的 occurrence 不会有开始时间。
 *
 * @param taskId      任务标识
 * @param status      本次 occurrence 的终态
 * @param scheduledAt 计划执行时间
 * @param startedAt   实际开始执行时间；未执行时为 {@code null}
 * @param completedAt 终态事件产生时间
 * @param failure     任务失败原因；仅 FAILURE 状态允许存在
 * @param reason      被跳过或拒绝的原因；成功和失败时通常为空
 */
public record TaskEvent(
        String taskId,
        TaskEventStatus status,
        Instant scheduledAt,
        @Nullable Instant startedAt,
        Instant completedAt,
        @Nullable Throwable failure,
        @Nullable String reason) {

    /**
     * 校验事件字段及状态相关的不变量。
     *
     * @param taskId      任务标识
     * @param status      本次 occurrence 的终态
     * @param scheduledAt 计划执行时间
     * @param startedAt   实际开始执行时间
     * @param completedAt 终态事件产生时间
     * @param failure     任务失败原因
     * @param reason      被跳过或拒绝的原因
     */
    public TaskEvent {
        taskId = ValidationUtils.requireNotBlank(taskId, "taskId must not be blank");
        ValidationUtils.requireNonNull(status, "status must not be null");
        ValidationUtils.requireNonNull(scheduledAt, "scheduledAt must not be null");
        ValidationUtils.requireNonNull(completedAt, "completedAt must not be null");
        boolean executed = status == TaskEventStatus.SUCCESS || status == TaskEventStatus.FAILURE;
        if (executed && startedAt == null) {
            throw new IllegalArgumentException("An executed event must include its start time");
        }
        if (!executed && startedAt != null) {
            throw new IllegalArgumentException("A skipped or rejected event cannot include a start time");
        }
        if (status == TaskEventStatus.FAILURE && failure == null) {
            throw new IllegalArgumentException("A FAILURE event must include its cause");
        }
        if (status != TaskEventStatus.FAILURE && failure != null) {
            throw new IllegalArgumentException("Only a FAILURE event may include a cause");
        }
    }
}
