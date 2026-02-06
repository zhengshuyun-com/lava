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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RetryUtil 单元测试
 * 测试 RetryUtil 提供的重试工具方法
 */
class RetryUtilTest {

    /**
     * 测试默认重试机制
     * 使用默认配置的重试机制, 首次失败后在第二次尝试时成功
     */
    @Test
    void testExecute() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = RetryUtil.retrier()
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() == 1) {
                        throw new RuntimeException("first attempt failed");
                    }
                    return "success";
                });

        assertEquals(2, counter.get());
        assertEquals("success", result);
    }

    /**
     * 测试固定延迟重试策略
     * 每次重试间隔固定时间 (10ms) , 最大尝试5次, 在第3次成功
     */
    @Test
    void testExecuteFixedDelay() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = RetryUtil.retrier()
                .setFixedDelayMillis(10)
                .setMaxAttempts(5)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() < 3) {
                        throw new RuntimeException("fails");
                    }
                    return "success";
                });

        assertEquals(3, counter.get());
        assertEquals("success", result);
    }

    /**
     * 测试指数退避重试策略
     * 延迟时间按指数增长 (初始10ms, 倍数2, 最大100ms) , 最大尝试5次
     */
    @Test
    void testExecuteExponentialBackoff() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = RetryUtil.retrier()
                .setExponentialBackoffMillis(10, 2, 100)
                .setMaxAttempts(5)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() < 3) {
                        throw new RuntimeException("fails");
                    }
                    return "success";
                });

        assertEquals(3, counter.get());
        assertEquals("success", result);
    }

    /**
     * 测试仅对特定异常类型进行重试
     * 只对IOException进行重试, 最大尝试5次, 在第3次成功
     */
    @Test
    void testExecuteOnException() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = RetryUtil.retrier()
                .setRetryOnException(IOException.class)
                .setFixedDelayMillis(10)
                .setMaxAttempts(5)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() < 3) {
                        throw new IOException("IO error");
                    }
                    return "success";
                });

        assertEquals(3, counter.get());
        assertEquals("success", result);
    }

    /**
     * 测试异常类型不匹配时不重试
     * 配置只重试IOException, 抛出IllegalArgumentException时直接抛出不重试, 仅执行1次
     */
    @Test
    void testExecuteOnExceptionNotMatch() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(IllegalArgumentException.class, () -> {
            RetryUtil.retrier()
                    .setRetryOnException(IOException.class)
                    .setFixedDelayMillis(10)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new IllegalArgumentException("not IO error");
                    });
        });

        assertEquals(1, counter.get());
    }

    /**
     * 测试根据结果条件进行重试
     * 使用 setRetryCondition 实现基于结果的重试
     */
    @Test
    void testExecuteOnResult() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = RetryUtil.retrier()
                .setRetryCondition((attempt, error, res) -> {
                    if (error != null) return false;
                    String s = (String) res;
                    return s == null || s.isEmpty();
                })
                .setFixedDelayMillis(10)
                .setMaxAttempts(5)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() < 3) {
                        return "";
                    }
                    return "success";
                });

        assertEquals(3, counter.get());
        assertEquals("success", result);
    }

    /**
     * 测试结果既非空也不符合条件时的行为
     * 当结果不满足重试条件时, 应该直接返回该结果不进行重试
     */
    @Test
    void testExecuteOnResultNonEmptyNotMatched() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = RetryUtil.retrier()
                .setRetryCondition((attempt, error, res) -> {
                    if (error != null) return false;
                    String s = (String) res;
                    return s == null || s.isEmpty();
                })
                .setFixedDelayMillis(10)
                .setMaxAttempts(5)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() == 1) {
                        return "valid";
                    }
                    return "success";
                });

        assertEquals(1, counter.get());
        assertEquals("valid", result);
    }

    /**
     * 测试重试监听器
     * 验证每次重试时监听器的回调被正确调用, 验证监听器回调参数的值
     */
    @Test
    void testExecuteWithListener() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger startCalls = new AtomicInteger(0);
        AtomicInteger completeCalls = new AtomicInteger(0);

        RetryListener listener = new RetryListener() {
            @Override
            public void onRetryStart(int attempt, int maxAttempts) {
                startCalls.incrementAndGet();
                assertEquals(attempt, counter.get() + 1);
                assertEquals(3, maxAttempts);
            }

            @Override
            public void onRetryComplete(int attempt, boolean success) {
                completeCalls.incrementAndGet();
                assertEquals(attempt, counter.get());
                assertEquals(success, attempt == 3);
            }
        };

        String result = RetryUtil.retrier()
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
                .setRetryListener(listener)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() < 3) {
                        throw new RuntimeException("fails");
                    }
                    return "success";
                });

        assertEquals(3, counter.get());
        assertEquals(3, startCalls.get());
        assertEquals(3, completeCalls.get());
        assertEquals("success", result);
    }

    /**
     * 测试达到最大重试次数后抛出异常
     * 重试3次全部失败后抛出原始异常, 总共执行3次
     */
    @Test
    void testExecuteMaxAttemptsReached() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            RetryUtil.retrier()
                    .setFixedDelayMillis(10)
                    .setMaxAttempts(3)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("always fails");
                    });
        });

        assertEquals(3, counter.get());
    }

    /**
     * 测试通过 RetryUtil 使用 Runnable
     */
    @Test
    void testExecuteRunnable() {
        AtomicInteger counter = new AtomicInteger(0);

        RetryUtil.retrier()
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
                .build()
                .execute((Runnable) () -> {
                    counter.incrementAndGet();
                    if (counter.get() < 2) {
                        throw new RuntimeException("fails");
                    }
                });

        assertEquals(2, counter.get());
    }
}
