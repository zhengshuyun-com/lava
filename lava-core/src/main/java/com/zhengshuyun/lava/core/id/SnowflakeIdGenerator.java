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

package com.zhengshuyun.lava.core.id;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import java.time.Clock;
import java.time.Instant;

/**
 * 使用显式工作节点标识的标准 64 位雪花算法生成器。
 *
 * <p>布局包含 1 个未使用的符号位、41 个时间戳位、10 个工作节点位和 12 个序列位。Lava epoch
 * 为 2026-01-01T00:00:00Z。时钟回拨和单毫秒序列耗尽都会立即失败；调用方可观测并告警，
 * 不会遭遇隐式等待。
 *
 * <p>{@code workerId} 必须由部署配置分配，且在所有实例间唯一、重启后稳定：两个实例复用同一个
 * {@code workerId} 会产出逐位相同的标识符序列。本类不提供默认值，因为该标识无法由主机名、IP 或
 * 进程号可靠推导——10 位仅有 1024 个取值，20 个实例哈希取值的碰撞概率已达 17%，而容器 IP 的
 * 低位熵远小于 10 位。推导默认值只会把启动期的显式失败换成运行期的静默重复标识。
 */
public final class SnowflakeIdGenerator {

    public static final Instant LAVA_EPOCH = Instant.parse("2026-01-01T00:00:00Z");
    public static final int MIN_WORKER_ID = 0;
    public static final int MAX_WORKER_ID = 1023;

    private static final int WORKER_BITS = 10;
    private static final int SEQUENCE_BITS = 12;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long MAX_TIMESTAMP = (1L << 41) - 1;
    private static final long EPOCH_MILLIS = LAVA_EPOCH.toEpochMilli();

    private final int workerId;
    private final Clock clock;

    private long lastUnixMillis = Long.MIN_VALUE;
    private long sequence;

    /**
     * 创建生成器。工作节点标识必须由部署配置明确分配。
     *
     * @param workerId 工作节点标识，范围为 0 至 1023
     */
    public SnowflakeIdGenerator(int workerId) {
        this(workerId, Clock.systemUTC());
    }

    /**
     * 使用显式时钟创建生成器，主要用于确定性测试。
     *
     * @param workerId 工作节点标识，范围为 0 至 1023
     * @param clock    读取当前时间的时钟
     */
    public SnowflakeIdGenerator(int workerId, Clock clock) {
        if (workerId < MIN_WORKER_ID || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and 1023: " + workerId);
        }
        this.workerId = workerId;
        this.clock = ValidationUtils.requireNonNull(clock, "clock");
    }

    /**
     * 返回下一个雪花算法标识符。
     *
     * @return 新的非负雪花算法标识符
     */
    public synchronized long nextId() {
        long unixMillis = clock.millis();
        long relativeMillis = unixMillis - EPOCH_MILLIS;
        if (relativeMillis < 0 || relativeMillis > MAX_TIMESTAMP) {
            throw new IdGenerationException(
                    "Snowflake timestamp is outside the Lava epoch range: " + unixMillis);
        }

        if (lastUnixMillis != Long.MIN_VALUE && unixMillis < lastUnixMillis) {
            throw new IdGenerationException(
                    "Clock moved backwards by " + (lastUnixMillis - unixMillis) + " ms");
        }

        if (unixMillis == lastUnixMillis) {
            if (sequence == MAX_SEQUENCE) {
                throw new IdGenerationException(
                        "Snowflake sequence exhausted at Unix millisecond " + unixMillis);
            }
            sequence++;
        } else {
            lastUnixMillis = unixMillis;
            sequence = 0;
        }

        return (relativeMillis << (WORKER_BITS + SEQUENCE_BITS))
                | ((long) workerId << SEQUENCE_BITS)
                | sequence;
    }

    /**
     * 以十进制且无精度损失地返回 {@link #nextId()}。
     *
     * @return 新的雪花算法标识符的十进制字符串
     */
    public String nextIdString() {
        return Long.toString(nextId());
    }

    /**
     * 返回此生成器的工作节点标识。
     *
     * @return 工作节点标识
     */
    public int workerId() {
        return workerId;
    }

    /**
     * 从使用 Lava epoch 生成的标识符中提取原始时间戳。
     *
     * @param id 非负雪花算法标识符
     * @return 标识符包含的 UTC 时间戳
     */
    public static Instant timestamp(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must not be negative");
        }
        long relativeMillis = id >>> (WORKER_BITS + SEQUENCE_BITS);
        return Instant.ofEpochMilli(EPOCH_MILLIS + relativeMillis);
    }
}
