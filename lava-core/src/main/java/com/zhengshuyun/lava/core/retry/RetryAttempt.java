/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.core.retry;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 每次尝试结束后发出的不可变观测结果。
 *
 * @param attempt     当前尝试序号，从 1 开始
 * @param maxAttempts 允许的总尝试次数
 * @param result      本次操作结果；异常失败时为 null
 * @param failure     本次失败原因；操作正常返回时为 null
 * @param willRetry   根据策略和剩余次数是否计划继续下一次尝试；等待期间仍可能被中断
 * @param nextDelay   计划在下一次尝试前等待的时间；未计划重试时为零
 * @param <T>         操作结果类型
 */
public record RetryAttempt<T>(
        int attempt,
        int maxAttempts,
        @Nullable T result,
        @Nullable Throwable failure,
        boolean willRetry,
        Duration nextDelay) {
}
