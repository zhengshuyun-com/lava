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

package com.zhengshuyun.lava.core.retry;

import com.zhengshuyun.lava.core.lang.Validate;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * 重试条件
 * <p>
 * 判断是否需要重试, 不包含重试次数限制 (由 Retrier.maxAttempts 控制)
 *
 * @author Toint
 * @since 2026/1/15
 */
@FunctionalInterface
public interface RetryCondition {

    /**
     * 任何异常都重试
     */
    RetryCondition ANY_EXCEPTION = (attempt, error, result) -> error != null;

    /**
     * 任何情况都重试
     * <p>
     * <b>注意: 成功也会重试</b>, 直到用尽 maxAttempts. 适用于轮询等待结果就绪的场景,
     * 普通的"失败才重试"请用 {@link #ANY_EXCEPTION}
     */
    RetryCondition ALWAYS = (attempt, error, result) -> true;

    /**
     * 任何情况都不重试
     */
    RetryCondition NEVER = (attempt, error, result) -> false;

    /**
     * 判断是否需要重试
     *
     * @param attempt 当前重试次数 (从 1 开始)
     * @param error   执行时抛出的异常 (可能为 null)
     * @param result  执行结果 (可能为 null)
     * @return true 表示需要重试, false 表示停止重试
     */
    boolean shouldRetry(int attempt, @Nullable Throwable error, @Nullable Object result);

    /**
     * 创建基于异常的重试条件
     * <p>
     * 只有当抛出的异常是指定类型(或其子类)时才重试
     *
     * @param types 异常类型数组 (不能为空, 元素不能为 null)
     * @return 条件
     * @throws IllegalArgumentException 如果 types 为 null、为空数组或含 null 元素
     */
    @SafeVarargs
    static RetryCondition ofException(Class<? extends Throwable>... types) {
        Validate.notNull(types, "exceptionTypes must not be null");
        // 空数组意味着任何异常都不重试, 这几乎总是调用方笔误, 与 NEVER 混淆; 提前失败而不是静默不重试
        Validate.isTrue(types.length > 0, "exceptionTypes must not be empty, use RetryCondition.NEVER instead");

        // 防御性拷贝, 避免调用方后续修改数组影响条件行为
        Class<? extends Throwable>[] exceptionTypes = Arrays.copyOf(types, types.length);
        for (Class<? extends Throwable> type : exceptionTypes) {
            Validate.notNull(type, "exceptionType must not be null");
        }

        return (attempt, error, result) -> {
            if (error == null) {
                return false;
            }
            for (Class<? extends Throwable> type : exceptionTypes) {
                if (type.isInstance(error)) {
                    return true;
                }
            }
            return false;
        };
    }
}
