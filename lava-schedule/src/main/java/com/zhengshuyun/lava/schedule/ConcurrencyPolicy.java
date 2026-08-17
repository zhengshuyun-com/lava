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

/**
 * 单个调度任务的有界重叠与排队策略。
 *
 * <p>{@link #SERIAL_SKIP} 不会排队；其余策略在达到并发上限后使用有界等待队列。
 */
public final class ConcurrencyPolicy {

    /** 可用的并发模式。 */
    public enum Kind {
        /** 最多运行一个实例；触发时已有实例运行则跳过本次。 */
        SERIAL_SKIP,
        /** 最多运行一个实例；触发时已有实例运行则进入等待队列。 */
        SERIAL_QUEUE,
        /** 允许多个实例并发运行，并限制并发数和等待队列长度。 */
        PARALLEL
    }

    /** 默认策略：绝不重叠；上一次执行仍在运行时跳过本次触发。 */
    public static final ConcurrencyPolicy SERIAL_SKIP =
            new ConcurrencyPolicy(Kind.SERIAL_SKIP, 1, 0);

    private final Kind kind;
    private final int maxConcurrency;
    private final int maxPending;

    private ConcurrencyPolicy(Kind kind, int maxConcurrency, int maxPending) {
        this.kind = kind;
        this.maxConcurrency = maxConcurrency;
        this.maxPending = maxPending;
    }

    /**
     * 创建带有界待执行队列的串行策略。
     *
     * @param maxPending 等待队列的最大长度
     * @return 新的串行排队策略
     * @throws IllegalArgumentException {@code maxPending} 不为正数
     */
    public static ConcurrencyPolicy serialQueue(int maxPending) {
        requirePositive(maxPending, "maxPending");
        return new ConcurrencyPolicy(Kind.SERIAL_QUEUE, 1, maxPending);
    }

    /**
     * 创建并发数和待执行任务数均有上限的并行策略。
     *
     * @param maxConcurrency 允许同时运行的最大实例数
     * @param maxPending 等待队列的最大长度；为零表示不排队
     * @return 新的并行策略
     * @throws IllegalArgumentException 参数不符合取值范围
     */
    public static ConcurrencyPolicy parallel(int maxConcurrency, int maxPending) {
        requirePositive(maxConcurrency, "maxConcurrency");
        requireNonNegative(maxPending, "maxPending");
        return new ConcurrencyPolicy(Kind.PARALLEL, maxConcurrency, maxPending);
    }

    /** 返回并发策略的模式。 */
    public Kind kind() {
        return kind;
    }

    /** 返回允许同时运行的最大实例数。 */
    public int maxConcurrency() {
        return maxConcurrency;
    }

    /** 返回等待队列的最大长度。 */
    public int maxPending() {
        return maxPending;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
