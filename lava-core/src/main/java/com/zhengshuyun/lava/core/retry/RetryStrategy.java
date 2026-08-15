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

import java.time.Duration;

/**
 * 重试延迟策略
 * <p>
 * 定义重试时的延迟时间计算逻辑, 不包含重试次数限制.
 * 通过静态工厂方法创建内置策略, 也可直接用 lambda 实现自定义策略
 *
 * @author Toint
 * @since 2026/1/15
 */
@FunctionalInterface
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
     * @param delayMillis 延迟时间 (毫秒, 必须 &gt;= 0)
     * @return 固定延迟策略
     * @throws IllegalArgumentException 如果 delayMillis &lt; 0
     */
    static RetryStrategy ofFixedDelayMillis(long delayMillis) {
        return ofFixedDelay(Duration.ofMillis(delayMillis));
    }

    /**
     * 创建固定延迟策略
     *
     * @param delay 延迟时间 (必须 &gt;= 0)
     * @return 固定延迟策略
     * @throws IllegalArgumentException 如果 delay 为 null 或负数
     */
    static RetryStrategy ofFixedDelay(Duration delay) {
        Validate.notNull(delay, "delay must not be null");
        Validate.isFalse(delay.isNegative(), "delay must be >= 0");
        return attempt -> delay;
    }

    /**
     * 创建指数退避策略 (毫秒)
     *
     * @param initialDelay 初始延迟 (毫秒, 必须 &gt;= 0)
     * @param multiplier   倍数 (必须 &gt; 0)
     * @param maxDelay     最大延迟 (毫秒, 必须 &gt;= 0)
     * @return 指数退避策略
     * @throws IllegalArgumentException 参数非法
     */
    static RetryStrategy ofExponentialBackoffMillis(long initialDelay, double multiplier, long maxDelay) {
        return ofExponentialBackoff(Duration.ofMillis(initialDelay), multiplier, Duration.ofMillis(maxDelay));
    }

    /**
     * 创建指数退避策略
     * <p>
     * 延迟时间按指数增长: {@code initialDelay * multiplier^(attempt-1)}, 并以 maxDelay 封顶
     * <p>
     * 示例 (initialDelay=1s, multiplier=2, maxDelay=10s) :
     * <ul>
     *   <li>第1次重试 (attempt=1) : 1 * 2^0 = 1s</li>
     *   <li>第2次重试 (attempt=2) : 1 * 2^1 = 2s</li>
     *   <li>第3次重试 (attempt=3) : 1 * 2^2 = 4s</li>
     *   <li>第4次重试 (attempt=4) : 1 * 2^3 = 8s</li>
     *   <li>第5次重试 (attempt=5) : 1 * 2^4 = 16s → 封顶为 10s</li>
     * </ul>
     *
     * @param initialDelay 初始延迟 (必须 &gt;= 0)
     * @param multiplier   倍数 (必须 &gt; 0)
     * @param maxDelay     最大延迟 (必须 &gt;= 0)
     * @return 指数退避策略
     * @throws IllegalArgumentException 参数非法
     */
    static RetryStrategy ofExponentialBackoff(Duration initialDelay, double multiplier, Duration maxDelay) {
        Validate.notNull(initialDelay, "initialDelay must not be null");
        Validate.isFalse(initialDelay.isNegative(), "initialDelay must be >= 0");
        Validate.isTrue(multiplier > 0, "multiplier must be > 0");
        Validate.notNull(maxDelay, "maxDelay must not be null");
        Validate.isFalse(maxDelay.isNegative(), "maxDelay must be >= 0");

        long initialMillis = initialDelay.toMillis();
        long maxMillis = maxDelay.toMillis();

        return attempt -> {
            // 指数增长可能溢出为 Infinity, 转 long 时会饱和为 Long.MAX_VALUE, 随后被 maxMillis 封顶
            double delayMillis = initialMillis * Math.pow(multiplier, attempt - 1);
            return Duration.ofMillis(Math.min((long) delayMillis, maxMillis));
        };
    }

    /**
     * 创建无延迟策略 (立即重试)
     *
     * @return 无延迟策略
     */
    static RetryStrategy ofNoDelay() {
        return attempt -> Duration.ZERO;
    }

}
