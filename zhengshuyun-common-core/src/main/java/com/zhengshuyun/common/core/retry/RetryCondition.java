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

package com.zhengshuyun.common.core.retry;

import com.zhengshuyun.common.core.lang.Validate;
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
public interface RetryCondition {

    /**
     * 任何异常都重试
     */
    RetryCondition ANY_EXCEPTION = (attempt, error, result) -> error != null;

    /**
     * 任何情况都重试
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
     * @param types 异常类型数组
     * @return 条件
     */
    @SafeVarargs
    static RetryCondition ofException(Class<? extends Throwable>... types) {
        return new RetryOnException(types);
    }

    /**
     * 基于异常类型的重试条件
     */
    class RetryOnException implements RetryCondition {

        /**
         * 异常类型数组
         */
        private final Class<? extends Throwable>[] exceptionTypes;

        @SafeVarargs
        RetryOnException(Class<? extends Throwable>... exceptionTypes) {
            Validate.notNull(exceptionTypes, "exceptionTypes must not be null");
            this.exceptionTypes = Arrays.copyOf(exceptionTypes, exceptionTypes.length);
        }

        @Override
        public boolean shouldRetry(int attempt, @Nullable Throwable error, @Nullable Object result) {
            if (error == null) {
                return false;
            }
            for (Class<? extends Throwable> type : exceptionTypes) {
                if (type.isInstance(error)) {
                    return true;
                }
            }
            return false;
        }
    }
}
