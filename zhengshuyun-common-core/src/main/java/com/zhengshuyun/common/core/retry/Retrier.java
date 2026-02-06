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

import com.google.common.base.Throwables;
import com.zhengshuyun.common.core.lang.Validate;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * 重试执行器
 * <p>
 * 支持自定义重试次数、延迟策略、重试条件和监听器的任务重试执行器. 
 * 当所有尝试都失败时, 会根据异常类型抛出：
 * <ul>
 *   <li>非受检异常 (RuntimeException 及其子类、Error) ：直接抛出原始异常</li>
 *   <li>受检异常 (Exception 及其子类) ：包装为 {@link RetryException} 抛出</li>
 * </ul>
 *
 * @author Toint
 * @since 2026/1/15
 */
public final class Retrier {

    /**
     * 最大尝试次数 (包含首次执行) 
     */
    private final int maxAttempts;

    /**
     * 重试延迟策略
     */
    private final RetryStrategy retryStrategy;

    /**
     * 重试条件
     */
    private final RetryCondition retryCondition;

    /**
     * 重试监听器
     * <p>
     * 注意：监听器方法抛出的异常会中断重试流程
     */
    private final @Nullable RetryListener retryListener;

    private Retrier(Builder builder) {
        Validate.isTrue(builder.maxAttempts >= 1, "maxAttempts must be >= 1");
        this.maxAttempts = builder.maxAttempts;
        this.retryStrategy = Validate.notNull(builder.retryStrategy, "retryStrategy must not be null");
        this.retryCondition = Validate.notNull(builder.retryCondition, "retryCondition must not be null");
        this.retryListener = builder.retryListener;
    }

    /**
     * 创建 RetrierBuilder 实例
     *
     * @return RetrierBuilder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 执行任务
     * <p>
     * 按照配置的重试策略执行任务, 失败时根据异常类型抛出：
     * <ul>
     *   <li>非受检异常：直接抛出原始异常</li>
     *   <li>受检异常：包装为 RetryException 抛出</li>
     * </ul>
     *
     * @param callable 任务
     * @param <T>      返回值类型
     * @return 执行结果
     * @throws RuntimeException 执行失败且为非受检异常时直接抛出
     * @throws RetryException   执行失败且为受检异常时包装抛出
     */
    public <T> T execute(Callable<T> callable) {
        return executeInternal(callable);
    }

    /**
     * 执行无返回值任务
     * <p>
     * 用于不需要返回值的场景, 简化调用代码
     * <p>
     * 示例:
     * <pre>{@code
     * // 保存数据, 不需要返回值
     * Retrier.builder()
     *     .setRetryOnException(IOException.class)
     *     .setMaxAttempts(3)
     *     .build()
     *     .execute(() -> saveToDatabase(data));
     *
     * // 发送通知, 不需要返回值
     * Retrier.builder()
     *     .setFixedDelayMillis(1000)
     *     .build()
     *     .execute(() -> sendNotification(message));
     * }</pre>
     *
     * @param runnable 无返回值任务
     * @throws RuntimeException 执行失败且为非受检异常时直接抛出
     * @throws RetryException   执行失败且为受检异常时包装抛出
     */
    public void execute(Runnable runnable) {
        execute(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 内部执行逻辑
     *
     * @param callable 任务
     * @param <T>      返回值类型
     * @return 执行结果
     * @throws RuntimeException 执行失败且为非受检异常时直接抛出
     * @throws RetryException   执行失败且为受检异常时包装抛出
     */
    private <T> T executeInternal(Callable<T> callable) {
        Validate.notNull(callable, "callable must not be null");
        T result = null;
        Throwable lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // 通知监听器开始
            if (retryListener != null) {
                retryListener.onRetryStart(attempt, maxAttempts);
            }

            // 执行任务
            boolean success;
            try {
                result = callable.call();
                lastError = null;
                success = true;
            } catch (Throwable e) {
                lastError = e;
                result = null;
                success = false;
            }

            // 通知监听器完成
            if (retryListener != null) {
                retryListener.onRetryComplete(attempt, success);
            }

            // 判断是否需要重试
            boolean shouldRetry = retryCondition.shouldRetry(attempt, lastError, result);
            if (!shouldRetry || attempt >= maxAttempts) {
                break;
            }

            // 等待后重试
            Duration delay = retryStrategy.getDelay(attempt);
            if (delay.isPositive()) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RetryException(e.getMessage(), e);
                }
            }
        }

        // 返回结果或抛出异常
        // 非受检异常直接抛出, 受检异常包装为 RetryException
        if (lastError != null) {
            Throwables.throwIfUnchecked(lastError);
            throw new RetryException(lastError.getMessage(), lastError);
        }

        return result;
    }

    /**
     * 重试执行器构建器
     * <p>
     * 使用 Builder 模式构建 {@link Retrier} 实例, 支持流式 API 配置
     *
     * @author Toint
     * @since 2026/1/15
     */
    public static final class Builder {

        /**
         * 最大尝试次数 (包含首次执行)
         */
        private int maxAttempts = 3;

        /**
         * 重试延迟策略, 默认固定延迟 1000ms
         */
        private RetryStrategy retryStrategy = RetryStrategy.ofFixedDelayMillis(1000);

        /**
         * 重试条件, 默认任何异常都重试
         */
        private RetryCondition retryCondition = RetryCondition.ANY_EXCEPTION;

        /**
         * 重试监听器, 可选
         */
        private @Nullable RetryListener retryListener;

        private Builder() {
        }

        /**
         * 创建 RetrierBuilder 实例
         *
         * @return RetrierBuilder 实例
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * 设置最大尝试次数 (包含首次执行) 
         *
         * @param maxAttempts 最大尝试次数 (必须 >= 1) 
         * @return this
         */
        public Builder setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * 设置重试延迟策略
         *
         * @param retryStrategy 重试延迟策略
         * @return this
         */
        public Builder setRetryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }

        /**
         * 设置固定延迟重试 (毫秒) 
         *
         * @param delayMillis 延迟时间 (毫秒) 
         * @return this
         */
        public Builder setFixedDelayMillis(long delayMillis) {
            this.retryStrategy = RetryStrategy.ofFixedDelayMillis(delayMillis);
            return this;
        }

        /**
         * 设置固定延迟重试
         *
         * @param delay 延迟时间
         * @return this
         */
        public Builder setFixedDelay(Duration delay) {
            this.retryStrategy = RetryStrategy.ofFixedDelay(delay);
            return this;
        }

        /**
         * 设置指数退避重试 (毫秒) 
         *
         * @param initialDelay 初始延迟 (毫秒) 
         * @param multiplier   倍数
         * @param maxDelay     最大延迟 (毫秒) 
         * @return this
         */
        public Builder setExponentialBackoffMillis(long initialDelay, double multiplier, long maxDelay) {
            this.retryStrategy = RetryStrategy.ofExponentialBackoffMillis(initialDelay, multiplier, maxDelay);
            return this;
        }

        /**
         * 设置指数退避重试
         *
         * @param initialDelay 初始延迟
         * @param multiplier   倍数
         * @param maxDelay     最大延迟
         * @return this
         */
        public Builder setExponentialBackoff(Duration initialDelay, double multiplier, Duration maxDelay) {
            this.retryStrategy = RetryStrategy.ofExponentialBackoff(initialDelay, multiplier, maxDelay);
            return this;
        }

        /**
         * 设置重试条件
         * <p>
         * 通过自定义条件判断是否需要重试,可用于复杂的重试逻辑,包括基于结果的重试
         * <p>
         * 示例:
         * <pre>{@code
         * // 基于结果的重试 (结果为 null 或空字符串时重试)
         * retrier.setRetryCondition((attempt, error, result) -> {
         *     if (error != null) return false;  // 有异常时不检查结果
         *     String str = (String) result;
         *     return str == null || str.isEmpty();
         * })
         * .build()
         * .execute(() -> fetchData());
         *
         * // 基于尝试次数的重试
         * retrier.setRetryCondition((attempt, error, result) -> attempt < 3)
         *        .build()
         *        .execute(() -> unstableTask());
         *
         * // 组合条件: 异常重试 + 结果重试
         * retrier.setRetryCondition((attempt, error, result) -> {
         *     if (error instanceof IOException) return true;  // IOException 总是重试
         *     if (result == null) return true;  // 结果为 null 重试
         *     return false;
         * })
         * .build()
         * .execute(() -> complexTask());
         * }</pre>
         *
         * @param condition 重试条件
         * @return this
         */
        public Builder setRetryCondition(RetryCondition condition) {
            this.retryCondition = condition;
            return this;
        }

        /**
         * 设置异常重试条件
         * <p>
         * 只对指定类型的异常进行重试
         * <p>
         * 示例:
         * <pre>{@code
         * // 只重试 IOException
         * retrier.setRetryOnException(IOException.class)
         *        .build()
         *        .execute(() -> readFile());
         *
         * // 重试多种异常类型
         * retrier.setRetryOnException(IOException.class, TimeoutException.class)
         *        .build()
         *        .execute(() -> httpCall());
         * }</pre>
         *
         * @param exceptionTypes 异常类型数组
         * @return this
         */
        @SafeVarargs
        public final Builder setRetryOnException(Class<? extends Throwable>... exceptionTypes) {
            this.retryCondition = RetryCondition.ofException(exceptionTypes);
            return this;
        }

        /**
         * 设置重试监听器
         * <p>
         * 注意：监听器方法抛出的异常会中断重试流程
         *
         * @param listener 重试监听器, 可以为 null
         * @return this
         */
        public Builder setRetryListener(@Nullable RetryListener listener) {
            this.retryListener = listener;
            return this;
        }

        /**
         * 构建 Retrier 实例
         *
         * @return Retrier 实例
         */
        public Retrier build() {
            return new Retrier(this);
        }
    }
}
