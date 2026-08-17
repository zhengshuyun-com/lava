/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.core.retry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void retriesCheckedExceptionsAndRethrowsTheOriginalInstance() {
        IOException expected = new IOException("offline");
        AtomicInteger calls = new AtomicInteger();
        RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(3)
                .retryOnException(IOException.class)
                .build();

        IOException actual = assertThrows(IOException.class, () ->
                new RetryExecutor().execute(policy, () -> {
                    calls.incrementAndGet();
                    throw expected;
                }));

        assertSame(expected, actual);
        assertEquals(3, calls.get());
    }

    @Test
    void doesNotEvaluateExceptionConditionAfterFinalSupplierAttempt() {
        IOException expected = new IOException("offline");
        RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(1)
                .retryOnException(failure -> {
                    throw new AssertionError("terminal condition must not be evaluated");
                })
                .build();

        IOException actual = assertThrows(IOException.class,
                () -> new RetryExecutor().execute(policy, () -> {
                    throw expected;
                }));

        assertSame(expected, actual);
    }

    @Test
    void doesNotEvaluateResultConditionAfterFinalAttempt() throws Exception {
        RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(1)
                .retryOnResult(result -> {
                    throw new AssertionError("terminal condition must not be evaluated");
                })
                .build();

        String result = new RetryExecutor().execute(policy, () -> "ready");

        assertEquals("ready", result);
    }

    @Test
    void doesNotEvaluateExceptionConditionAfterFinalRunnableAttempt() {
        IOException expected = new IOException("offline");
        RetryPolicy<Void> policy = RetryPolicy.<Void>builder()
                .maxAttempts(1)
                .retryOnException(failure -> {
                    throw new AssertionError("terminal condition must not be evaluated");
                })
                .build();

        IOException actual = assertThrows(IOException.class,
                () -> new RetryExecutor().run(policy, () -> {
                    throw expected;
                }));

        assertSame(expected, actual);
    }

    @Test
    void supportsResultConditionsListenersAndInjectedSleeper() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<Duration> slept = new ArrayList<>();
        List<RetryAttempt<String>> attempts = new ArrayList<>();
        RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(4)
                .fixedDelay(Duration.ofMillis(7))
                .retryOnResult("pending"::equals)
                .listener(attempts::add)
                .build();

        String result = new RetryExecutor(new Random(1), slept::add).execute(
                policy,
                () -> calls.incrementAndGet() < 3 ? "pending" : "ready");

        assertEquals("ready", result);
        assertEquals(List.of(Duration.ofMillis(7), Duration.ofMillis(7)), slept);
        assertEquals(3, attempts.size());
        assertTrue(attempts.getFirst().willRetry());
        assertFalse(attempts.getLast().willRetry());
        assertEquals(Duration.ZERO, attempts.getLast().nextDelay());
    }

    @Test
    void computesCappedExponentialAndFullJitterDelays() {
        RetryDelayStrategy exponential = RetryDelayStrategy.exponential(
                Duration.ofMillis(10), 2, Duration.ofMillis(25));
        assertEquals(Duration.ofMillis(10), exponential.delayAfter(1, new Random(1)));
        assertEquals(Duration.ofMillis(20), exponential.delayAfter(2, new Random(1)));
        assertEquals(Duration.ofMillis(25), exponential.delayAfter(3, new Random(1)));

        RetryDelayStrategy jitter = RetryDelayStrategy.fullJitter(
                Duration.ofMillis(100), 2, Duration.ofSeconds(1));
        Random random = new Random(1234);
        for (int attempt = 1; attempt <= 20; attempt++) {
            Duration cap = exponentialCap(attempt);
            Duration delay = jitter.delayAfter(attempt, random);
            assertFalse(delay.isNegative());
            assertTrue(delay.compareTo(cap) < 0);
        }
    }

    @Test
    void interruptionIsNeverRetriedAndInterruptFlagIsRestored() {
        AtomicInteger calls = new AtomicInteger();
        List<RetryAttempt<String>> attempts = new ArrayList<>();
        RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(5)
                .listener(attempts::add)
                .build();

        assertThrows(InterruptedException.class, () ->
                new RetryExecutor().execute(policy, () -> {
                    calls.incrementAndGet();
                    throw new InterruptedException("cancelled");
                }));

        assertEquals(1, calls.get());
        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(1, attempts.size());
        assertInstanceOf(InterruptedException.class, attempts.getFirst().failure());
        assertFalse(attempts.getFirst().willRetry());
        assertEquals(Duration.ZERO, attempts.getFirst().nextDelay());
    }

    @Test
    void runnableInterruptionEmitsTerminalAttemptAndRestoresInterruptFlag() {
        List<RetryAttempt<Void>> attempts = new ArrayList<>();
        RetryPolicy<Void> policy = RetryPolicy.<Void>builder()
                .maxAttempts(5)
                .listener(attempts::add)
                .build();

        assertThrows(InterruptedException.class, () -> new RetryExecutor().run(policy, () -> {
            throw new InterruptedException("cancelled");
        }));

        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(1, attempts.size());
        assertInstanceOf(InterruptedException.class, attempts.getFirst().failure());
        assertFalse(attempts.getFirst().willRetry());
        assertEquals(Duration.ZERO, attempts.getFirst().nextDelay());
    }

    @Test
    void delayInterruptionRestoresInterruptFlag() {
        RetryPolicy<String> policy = RetryPolicy.<String>builder()
                .maxAttempts(2)
                .fixedDelay(Duration.ofMillis(1))
                .build();
        RetryExecutor executor = new RetryExecutor(new Random(1), delay -> {
            throw new InterruptedException("stop waiting");
        });

        assertThrows(InterruptedException.class,
                () -> executor.execute(policy, () -> {
                    throw new IOException("retryable");
                }));
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void worksFromVirtualThreadsAndSupportsCheckedRunnable() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        RetryPolicy<Void> policy = RetryPolicy.<Void>builder().maxAttempts(2).build();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                new RetryExecutor().run(policy, () -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IOException("retry");
                    }
                });
                return null;
            }).get();
        }

        assertEquals(2, attempts.get());
    }

    private static Duration exponentialCap(int attempt) {
        long millis = Math.min(100L << Math.min(attempt - 1, 4), 1000L);
        return Duration.ofMillis(millis);
    }
}
