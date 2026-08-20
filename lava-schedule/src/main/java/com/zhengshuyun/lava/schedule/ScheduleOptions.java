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

/**
 * 每个任务的调度选项。
 *
 * @param concurrencyPolicy 任务实例的并发与排队策略
 * @param misfirePolicy     任务错过计划时刻时的补偿策略
 */
public record ScheduleOptions(
        ConcurrencyPolicy concurrencyPolicy,
        MisfirePolicy misfirePolicy) {

    /**
     * 默认不重叠、不排队，并丢弃错过的计划时刻。
     */
    public static final ScheduleOptions DEFAULT =
            new ScheduleOptions(ConcurrencyPolicy.SERIAL_SKIP, MisfirePolicy.SKIP);

    /**
     * 校验两个策略均已提供。
     *
     * @param concurrencyPolicy 并发与排队策略
     * @param misfirePolicy     misfire 策略
     */
    public ScheduleOptions {
        ValidationUtils.requireNonNull(concurrencyPolicy, "concurrencyPolicy must not be null");
        ValidationUtils.requireNonNull(misfirePolicy, "misfirePolicy must not be null");
    }

    /**
     * 创建任务调度选项。
     *
     * @param concurrencyPolicy 并发与排队策略
     * @param misfirePolicy     misfire 策略
     * @return 新的调度选项
     */
    public static ScheduleOptions of(
            ConcurrencyPolicy concurrencyPolicy, MisfirePolicy misfirePolicy) {
        return new ScheduleOptions(concurrencyPolicy, misfirePolicy);
    }
}
