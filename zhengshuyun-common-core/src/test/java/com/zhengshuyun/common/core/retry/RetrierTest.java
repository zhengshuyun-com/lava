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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Retrier 单元测试
 * 测试重试策略、配置和行为, 包括固定延迟、指数退避、异常处理和监听器
 */
class RetrierTest {

    /**
     * 测试首次尝试即成功
     * 任务在第一次执行时就成功, 无需重试
     */
    @Test
    void testExecuteSuccessOnFirstAttempt() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = Retrier.builder()
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    return "success";
                });

        assertEquals(1, counter.get());
        assertEquals("success", result);
    }

    /**
     * 测试第二次尝试成功
     * 首次失败后在重试时成功
     */
    @Test
    void testExecuteSuccessOnSecondAttempt() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = Retrier.builder()
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
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
     * 测试达到最大重试次数后抛出异常
     * 全部尝试失败后抛出异常
     */
    @Test
    void testExecuteMaxAttemptsReached() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
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
     * 测试固定延迟策略的时间准确性
     * 验证重试之间的延迟时间符合预期 (延迟50ms, 执行3次共2次延迟, 至少100ms) 
     */
    @Test
    void testFixedDelayStrategy() {
        AtomicInteger counter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setFixedDelayMillis(50)
                    .setMaxAttempts(3)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertEquals(3, counter.get());
        assertTrue(elapsedTime >= 100, "Expected at least 100ms (2 delays of 50ms)");
    }

    /**
     * 测试指数退避策略
     * 验证重试延迟按指数增长
     */
    @Test
    void testExponentialBackoffStrategy() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setExponentialBackoffMillis(10, 2, 1000)
                    .setMaxAttempts(4)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        assertEquals(4, counter.get());
    }

    /**
     * 测试特定异常重试
     * 只对IOException进行重试, 达到最大次数后包装为RuntimeException抛出, 总共执行3次
     */
    @Test
    void testRetryOnSpecificException() {
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setRetryOnException(IOException.class)
                    .setFixedDelayMillis(10)
                    .setMaxAttempts(3)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new IOException("IO error");
                    });
        });

        assertTrue(e.getCause() instanceof IOException);
        assertEquals("IO error", e.getCause().getMessage());
        assertEquals(3, counter.get());
    }

    /**
     * 测试异常类型不匹配时不重试
     * 抛出非目标异常类型 (IllegalArgumentException) 时直接失败, 不进行重试, 仅执行1次
     */
    @Test
    void testRetryDoesNotMatchOtherException() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(IllegalArgumentException.class, () -> {
            Retrier.builder()
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
     * 测试根据结果条件重试
     * 使用 setRetryCondition 实现基于结果的重试
     */
    @Test
    void testRetryOnResultCondition() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = Retrier.builder()
                .setRetryCondition((attempt, error, res) -> {
                    if (error != null) return false;  // 有异常不检查结果
                    String str = (String) res;
                    return str == null || str.isEmpty();  // null 或空字符串需要重试
                })
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
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
    void testRetryOnResultConditionNonEmptyNotMatched() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = Retrier.builder()
                .setRetryCondition((attempt, error, res) -> {
                    if (error != null) return false;
                    String str = (String) res;
                    return str == null || str.isEmpty();
                })
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() == 1) {
                        return "valid";  // 非空字符串,不重试
                    }
                    return "success";
                });

        assertEquals(1, counter.get());
        assertEquals("valid", result);
    }

    /**
     * 测试重试监听器功能
     * 验证每次重试时监听器的onRetryStart和onRetryComplete回调被正确调用
     * 验证监听器回调参数attempt和success的值是否正确
     */
    @Test
    void testRetryListener() {
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

        String result = Retrier.builder()
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
     * 测试自定义重试条件
     * 使用自定义条件判断是否继续重试 (尝试次数小于3次) 
     */
    @Test
    void testCustomRetryCondition() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = Retrier.builder()
                .setRetryCondition((attempt, error, retryResult) -> attempt < 3)
                .setFixedDelayMillis(10)
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
     * 测试默认配置
     * 使用默认最大重试次数 (3次) 和默认延迟策略, 全部失败后抛出异常, 总共执行3次
     */
    @Test
    void testDefaultConfiguration() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        assertEquals(3, counter.get());
    }

    /**
     * 测试无延迟策略
     */
    @Test
    void testNoDelayStrategy() {
        AtomicInteger counter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setRetryStrategy(RetryStrategy.ofNoDelay())
                    .setMaxAttempts(5)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertEquals(5, counter.get());
        assertTrue(elapsedTime < 100, "No delay should complete quickly");
    }

    /**
     * 测试 maxAttempts 为 1 (不重试)
     */
    @Test
    void testMaxAttemptsOne() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setMaxAttempts(1)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        assertEquals(1, counter.get());
    }

    /**
     * 测试 maxAttempts 小于1抛异常
     */
    @Test
    void testInvalidMaxAttempts() {
        assertThrows(IllegalArgumentException.class, () -> {
            Retrier.builder()
                    .setMaxAttempts(0)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Retrier.builder()
                    .setMaxAttempts(-1)
                    .build();
        });
    }

    /**
     * 测试受检异常被包装为 RetryException
     */
    @Test
    void testCheckedExceptionWrapped() {
        AtomicInteger counter = new AtomicInteger(0);

        RetryException exception = assertThrows(RetryException.class, () -> {
            Retrier.builder()
                    .setFixedDelayMillis(10)
                    .setMaxAttempts(2)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new Exception("checked exception");
                    });
        });

        assertEquals(2, counter.get());
        assertInstanceOf(Exception.class, exception.getCause());
        assertEquals("checked exception", exception.getCause().getMessage());
    }

    /**
     * 测试 InterruptedException 处理
     */
    @Test
    void testInterruptedException() {
        Thread.currentThread().interrupt(); // 设置中断标志

        RetryException exception = assertThrows(RetryException.class, () -> {
            Retrier.builder()
                    .setFixedDelayMillis(100)
                    .setMaxAttempts(3)
                    .build()
                    .execute(() -> {
                        throw new RuntimeException("first fails");
                    });
        });

        assertTrue(exception.getCause() instanceof InterruptedException);
        assertTrue(Thread.interrupted()); // 清除中断标志
    }

    /**
     * 测试 ALWAYS 重试条件
     */
    @Test
    void testAlwaysRetryCondition() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = Retrier.builder()
                .setRetryCondition(RetryCondition.ALWAYS)
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
                .build()
                .execute(() -> {
                    counter.incrementAndGet();
                    if (counter.get() < 3) {
                        return "not ready";
                    }
                    return "success";
                });

        assertEquals(3, counter.get());
        assertEquals("success", result);
    }

    /**
     * 测试 NEVER 重试条件
     */
    @Test
    void testNeverRetryCondition() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setRetryCondition(RetryCondition.NEVER)
                    .setMaxAttempts(5)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        assertEquals(1, counter.get(), "Should not retry with NEVER condition");
    }

    /**
     * 测试 callable 为 null
     */
    @Test
    void testNullCallable() {
        Retrier retrier = Retrier.builder().build();
        assertThrows(IllegalArgumentException.class, () -> retrier.execute((java.util.concurrent.Callable<Object>) null));
    }

    /**
     * 测试多种异常类型的重试
     */
    @Test
    void testMultipleExceptionTypes() {
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setRetryOnException(IOException.class, IllegalStateException.class)
                    .setFixedDelayMillis(10)
                    .setMaxAttempts(3)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        if (counter.get() == 1) {
                            throw new IOException("IO error");
                        } else if (counter.get() == 2) {
                            throw new IllegalStateException("State error");
                        } else {
                            throw new RuntimeException("Runtime error");
                        }
                    });
        });

        assertEquals(3, counter.get());
        assertEquals("Runtime error", exception.getMessage());
    }

    /**
     * 测试监听器中抛异常会中断重试
     */
    @Test
    void testListenerExceptionStopsRetry() {
        AtomicInteger counter = new AtomicInteger(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setFixedDelayMillis(10)
                    .setMaxAttempts(5)
                    .setRetryListener(new RetryListener() {
                        @Override
                        public void onRetryStart(int attempt, int maxAttempts) {
                            if (attempt == 2) {
                                throw new RuntimeException("Listener error");
                            }
                        }

                        @Override
                        public void onRetryComplete(int attempt, boolean success) {
                        }
                    })
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("task error");
                    });
        });

        assertTrue(counter.get() < 5, "Retry should be interrupted by listener exception");
        assertEquals("Listener error", exception.getMessage());
    }

    /**
     * 测试指数退避达到最大延迟
     */
    @Test
    void testExponentialBackoffMaxDelay() {
        AtomicInteger counter = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setExponentialBackoffMillis(10, 3, 50) // 初始10ms, 倍数3, 最大50ms
                    .setMaxAttempts(5)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertEquals(5, counter.get());
        // 延迟: 10 + 30 + 50 + 50 = 140ms (后两次达到最大值)
        assertTrue(elapsedTime >= 100, "Should respect max delay");
    }

    /**
     * 测试 Duration 形式的固定延迟
     */
    @Test
    void testFixedDelayWithDuration() {
        AtomicInteger counter = new AtomicInteger(0);

        String result = Retrier.builder()
                .setFixedDelay(java.time.Duration.ofMillis(10))
                .setMaxAttempts(3)
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
     * 测试 Duration 形式的指数退避
     */
    @Test
    void testExponentialBackoffWithDuration() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setExponentialBackoff(
                            java.time.Duration.ofMillis(10),
                            2.0,
                            java.time.Duration.ofMillis(100)
                    )
                    .setMaxAttempts(4)
                    .build()
                    .execute(() -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("fails");
                    });
        });

        assertEquals(4, counter.get());
    }



    /**
     * 测试 FixedDelayStrategy 负延迟校验
     */
    @Test
    void testFixedDelayStrategyNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategy.ofFixedDelayMillis(-1)
        );

        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategy.ofFixedDelay(java.time.Duration.ofMillis(-1))
        );
    }

    /**
     * 测试 ExponentialBackoffStrategy 参数校验
     */
    @Test
    void testExponentialBackoffStrategyValidation() {
        // 负初始延迟
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategy.ofExponentialBackoffMillis(-1, 2, 100)
        );

        // 零或负倍数
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategy.ofExponentialBackoffMillis(10, 0, 100)
        );

        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategy.ofExponentialBackoffMillis(10, -1, 100)
        );

        // 负最大延迟
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategy.ofExponentialBackoffMillis(10, 2, -1)
        );
    }

    /**
     * 测试返回 null 的场景
     */
    @Test
    void testExecuteReturnsNull() {
        String result = Retrier.builder()
                .build()
                .execute(() -> null);

        assertNull(result);
    }

    /**
     * 测试监听器的 onRetryComplete 在成功和失败时都被调用
     */
    @Test
    void testListenerOnRetryCompleteCalledOnBothSuccessAndFailure() {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Retrier.builder()
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
                .setRetryListener(new RetryListener() {
                    @Override
                    public void onRetryStart(int attempt, int maxAttempts) {
                    }

                    @Override
                    public void onRetryComplete(int attempt, boolean success) {
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                })
                .build()
                .execute(() -> {
                    if (successCount.get() + failureCount.get() < 2) {
                        throw new RuntimeException("fails");
                    }
                    return "success";
                });

        assertEquals(1, successCount.get());
        assertEquals(2, failureCount.get());
    }

    /**
     * 测试 Runnable 支持 - 正常执行
     */
    @Test
    void testExecuteRunnableSuccess() {
        AtomicInteger counter = new AtomicInteger(0);

        Retrier.builder()
                .build()
                .execute((Runnable) () -> counter.incrementAndGet());

        assertEquals(1, counter.get());
    }

    /**
     * 测试 Runnable 支持 - 重试机制
     */
    @Test
    void testExecuteRunnableWithRetry() {
        AtomicInteger counter = new AtomicInteger(0);

        Retrier.builder()
                .setFixedDelayMillis(10)
                .setMaxAttempts(3)
                .build()
                .execute((Runnable) () -> {
                    counter.incrementAndGet();
                    if (counter.get() < 3) {
                        throw new RuntimeException("fails");
                    }
                });

        assertEquals(3, counter.get());
    }

    /**
     * 测试 Runnable 支持 - 异常抛出
     */
    @Test
    void testExecuteRunnableThrowsException() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () -> {
            Retrier.builder()
                    .setMaxAttempts(2)
                    .build()
                    .execute((Runnable) () -> {
                        counter.incrementAndGet();
                        throw new RuntimeException("always fails");
                    });
        });

        assertEquals(2, counter.get());
    }

    /**
     * 测试 RetryCondition.ofException 工厂方法
     */
    @Test
    void testRetryConditionOfException() {
        RetryCondition condition = RetryCondition.ofException(IOException.class);

        // 匹配的异常类型
        assertTrue(condition.shouldRetry(1, new IOException("test"), null));

        // 匹配的子类异常
        assertTrue(condition.shouldRetry(1, new java.io.FileNotFoundException("test"), null));

        // 不匹配的异常类型
        assertFalse(condition.shouldRetry(1, new IllegalArgumentException("test"), null));

        // 无异常
        assertFalse(condition.shouldRetry(1, null, "result"));
    }

    /**
     * 测试 RetryCondition.ANY_EXCEPTION 常量
     */
    @Test
    void testRetryConditionAnyException() {
        // 有异常时重试
        assertTrue(RetryCondition.ANY_EXCEPTION.shouldRetry(1, new RuntimeException(), null));
        assertTrue(RetryCondition.ANY_EXCEPTION.shouldRetry(1, new IOException(), null));

        // 无异常时不重试
        assertFalse(RetryCondition.ANY_EXCEPTION.shouldRetry(1, null, "result"));
    }

    /**
     * 测试 RetryException 单参数构造函数
     */
    @Test
    void testRetryExceptionSingleArgConstructor() {
        RetryException exception = new RetryException("test message");

        assertEquals("test message", exception.getMessage());
        assertNull(exception.getCause());
    }

    /**
     * 测试 RetryException 双参数构造函数
     */
    @Test
    void testRetryExceptionTwoArgConstructor() {
        IOException cause = new IOException("cause message");
        RetryException exception = new RetryException("test message", cause);

        assertEquals("test message", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
