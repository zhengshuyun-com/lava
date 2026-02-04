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

import java.time.Duration;

/**
 * 重试延迟策略
 * <p>
 * 定义重试时的延迟时间计算逻辑, 不包含重试次数限制
 *
 * @author Toint
 * @since 2026/1/15
 */
public interface RetryStrategy {

    /**
     * 获取下一次重试前的延迟时间
     *
     * @param attempt 当前重试次数 (从 1 开始) 
     * @return 延迟时间
     */
    Duration getDelay(int attempt);

    /**
     * 创建固定延迟策略 (毫秒) 
     *
     * @param delayMillis 延迟时间 (毫秒) 
     * @return 固定延迟策略
     */
    static RetryStrategy ofFixedDelayMillis(long delayMillis) {
        return new FixedDelayStrategy(Duration.ofMillis(delayMillis));
    }

    /**
     * 创建固定延迟策略
     *
     * @param delay 延迟时间
     * @return 固定延迟策略
     */
    static RetryStrategy ofFixedDelay(Duration delay) {
        return new FixedDelayStrategy(delay);
    }

    /**
     * 创建指数退避策略 (毫秒) 
     *
     * @param initialDelay 初始延迟 (毫秒) 
     * @param multiplier   倍数
     * @param maxDelay     最大延迟 (毫秒) 
     * @return 指数退避策略
     */
    static RetryStrategy ofExponentialBackoffMillis(long initialDelay, double multiplier, long maxDelay) {
        return new ExponentialBackoffStrategy(Duration.ofMillis(initialDelay), multiplier, Duration.ofMillis(maxDelay));
    }

    /**
     * 创建指数退避策略
     *
     * @param initialDelay 初始延迟
     * @param multiplier   倍数
     * @param maxDelay     最大延迟
     * @return 指数退避策略
     */
    static RetryStrategy ofExponentialBackoff(Duration initialDelay, double multiplier, Duration maxDelay) {
        return new ExponentialBackoffStrategy(initialDelay, multiplier, maxDelay);
    }

    /**
     * 创建无延迟策略 (立即重试) 
     *
     * @return 无延迟策略
     */
    static RetryStrategy ofNoDelay() {
        return new NoDelayStrategy();
    }

    /**
     * 固定延迟策略
     */
    class FixedDelayStrategy implements RetryStrategy {

        private final Duration delay;

        public FixedDelayStrategy(Duration delay) {
            if (delay == null || delay.isNegative()) {
                throw new IllegalArgumentException("delay must be >= 0");
            }
            this.delay = delay;
        }

        @Override
        public Duration getDelay(int attempt) {
            return delay;
        }
    }

    /**
     * 指数退避策略
     * <p>
     * 延迟时间按指数增长：initialDelay * multiplier^(attempt-1)
     * <p>
     * 示例 (initialDelay=1s, multiplier=2, maxDelay=10s) ：
     * <ul>
     *   <li>第1次重试 (attempt=1) ：1 * 2^0 = 1s</li>
     *   <li>第2次重试 (attempt=2) ：1 * 2^1 = 2s</li>
     *   <li>第3次重试 (attempt=3) ：1 * 2^2 = 4s</li>
     *   <li>第4次重试 (attempt=4) ：1 * 2^3 = 8s</li>
     *   <li>第5次重试 (attempt=5) ：1 * 2^4 = 16s → 限制为 10s</li>
     * </ul>
     */
    class ExponentialBackoffStrategy implements RetryStrategy {

        /**
         * 初始延迟
         */
        private final Duration initialDelay;

        /**
         * 倍数, 每次重试延迟乘以该值
         */
        private final double multiplier;

        /**
         * 最大延迟, 延迟时间不超过此值
         */
        private final Duration maxDelay;

        public ExponentialBackoffStrategy(Duration initialDelay, double multiplier, Duration maxDelay) {
            if (initialDelay == null || initialDelay.isNegative()) {
                throw new IllegalArgumentException("initialDelay must be >= 0");
            }
            if (multiplier <= 0) {
                throw new IllegalArgumentException("multiplier must be > 0");
            }
            if (maxDelay == null || maxDelay.isNegative()) {
                throw new IllegalArgumentException("maxDelay must be >= 0");
            }
            this.initialDelay = initialDelay;
            this.multiplier = multiplier;
            this.maxDelay = maxDelay;
        }

        @Override
        public Duration getDelay(int attempt) {
            // 计算指数退避延迟：initialDelay * multiplier^(attempt-1)
            long delayMillis = (long) (initialDelay.toMillis() * Math.pow(multiplier, attempt - 1));
            // 限制最大延迟
            long cappedDelayMillis = Math.min(delayMillis, maxDelay.toMillis());
            return Duration.ofMillis(cappedDelayMillis);
        }
    }

    /**
     * 无延迟策略 (立即重试) 
     */
    class NoDelayStrategy implements RetryStrategy {

        @Override
        public Duration getDelay(int attempt) {
            return Duration.ZERO;
        }
    }
}
