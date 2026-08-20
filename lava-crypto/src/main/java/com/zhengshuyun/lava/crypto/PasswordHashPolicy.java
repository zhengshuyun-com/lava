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

package com.zhengshuyun.lava.crypto;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

/**
 * 不可变的 Argon2id 策略。生成成本与验证资源上限有意分离，避免攻击者指定无界的 PHC 参数。
 *
 * @param generation         写入新哈希时使用的参数
 * @param verificationLimits 验证外部 PHC 字符串时允许的资源上限
 */
public record PasswordHashPolicy(
        Generation generation,
        VerificationLimits verificationLimits) {

    private static final int DEFAULT_MAX_ENCODED_LENGTH = 1_024;

    /**
     * 默认生成参数：64 MiB、3 次迭代、1 个并行通道、16 字节盐和 32 字节哈希。
     */
    public static final Generation DEFAULT_GENERATION = new Generation(65_536, 3, 1, 16, 32);

    /**
     * 默认验证上限：256 MiB、10 次迭代、16 个并行通道以及 64 字节盐和哈希。
     */
    public static final VerificationLimits DEFAULT_VERIFICATION_LIMITS =
            new VerificationLimits(262_144, 10, 16, 64, 64, DEFAULT_MAX_ENCODED_LENGTH);

    /**
     * 默认密码哈希策略。
     */
    public static final PasswordHashPolicy DEFAULT =
            new PasswordHashPolicy(DEFAULT_GENERATION, DEFAULT_VERIFICATION_LIMITS);

    public PasswordHashPolicy {
        ValidationUtils.requireNonNull(generation, "generation must not be null");
        ValidationUtils.requireNonNull(verificationLimits, "verificationLimits must not be null");
        if (generation.memoryKiB() > verificationLimits.maxMemoryKiB()
                || generation.iterations() > verificationLimits.maxIterations()
                || generation.parallelism() > verificationLimits.maxParallelism()
                || generation.saltLengthBytes() > verificationLimits.maxSaltLengthBytes()
                || generation.hashLengthBytes() > verificationLimits.maxHashLengthBytes()) {
            throw new IllegalArgumentException(
                    "Generation parameters must fit within verification resource limits");
        }
        if (verificationLimits.maxEncodedHashLength()
                < minimumEncodedHashLength(generation)) {
            throw new IllegalArgumentException(
                    "maxEncodedHashLength must fit generated Argon2id hashes");
        }
    }

    /**
     * 返回默认密码哈希策略。
     *
     * @return 默认策略
     */
    public static PasswordHashPolicy defaults() {
        return DEFAULT;
    }

    /**
     * 写入新生成 PHC 字符串的参数。
     *
     * @param memoryKiB       Argon2 使用的内存大小，单位 KiB
     * @param iterations      Argon2 迭代次数
     * @param parallelism     Argon2 并行通道数
     * @param saltLengthBytes 随机盐字节数
     * @param hashLengthBytes 输出哈希字节数
     */
    public record Generation(
            int memoryKiB,
            int iterations,
            int parallelism,
            int saltLengthBytes,
            int hashLengthBytes) {

        public Generation {
            requirePositive(memoryKiB, "memoryKiB");
            requirePositive(iterations, "iterations");
            requirePositive(parallelism, "parallelism");
            if (memoryKiB < 8L * parallelism) {
                throw new IllegalArgumentException(
                        "memoryKiB must be at least eight times parallelism");
            }
            if (saltLengthBytes < 8) {
                throw new IllegalArgumentException("saltLengthBytes must be at least 8");
            }
            if (hashLengthBytes < 4) {
                throw new IllegalArgumentException("hashLengthBytes must be at least 4");
            }
        }
    }

    /**
     * 在执行任何 Argon2 运算或按攻击者指定的大小分配 Base64 内存前检查的硬性上限。
     *
     * @param maxMemoryKiB         允许验证的最大内存大小，单位 KiB
     * @param maxIterations        允许验证的最大迭代次数
     * @param maxParallelism       允许验证的最大并行通道数
     * @param maxSaltLengthBytes   允许验证的最大盐字节数
     * @param maxHashLengthBytes   允许验证的最大哈希字节数
     * @param maxEncodedHashLength 允许的 PHC 字符串最大字符数
     */
    public record VerificationLimits(
            int maxMemoryKiB,
            int maxIterations,
            int maxParallelism,
            int maxSaltLengthBytes,
            int maxHashLengthBytes,
            int maxEncodedHashLength) {

        /**
         * 使用默认 PHC 字符串长度上限创建验证资源上限。
         *
         * @param maxMemoryKiB       允许验证的最大内存大小，单位 KiB
         * @param maxIterations      允许验证的最大迭代次数
         * @param maxParallelism     允许验证的最大并行通道数
         * @param maxSaltLengthBytes 允许验证的最大盐字节数
         * @param maxHashLengthBytes 允许验证的最大哈希字节数
         */
        public VerificationLimits(
                int maxMemoryKiB,
                int maxIterations,
                int maxParallelism,
                int maxSaltLengthBytes,
                int maxHashLengthBytes) {
            this(
                    maxMemoryKiB,
                    maxIterations,
                    maxParallelism,
                    maxSaltLengthBytes,
                    maxHashLengthBytes,
                    DEFAULT_MAX_ENCODED_LENGTH);
        }

        public VerificationLimits {
            requirePositive(maxMemoryKiB, "maxMemoryKiB");
            requirePositive(maxIterations, "maxIterations");
            requirePositive(maxParallelism, "maxParallelism");
            if (maxSaltLengthBytes < 8) {
                throw new IllegalArgumentException("maxSaltLengthBytes must be at least 8");
            }
            if (maxHashLengthBytes < 4) {
                throw new IllegalArgumentException("maxHashLengthBytes must be at least 4");
            }
            if (maxEncodedHashLength < 64) {
                throw new IllegalArgumentException("maxEncodedHashLength must be at least 64");
            }
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static long minimumEncodedHashLength(Generation generation) {
        String prefix = "$argon2id$v=19$m=" + generation.memoryKiB()
                + ",t=" + generation.iterations()
                + ",p=" + generation.parallelism() + "$";
        return prefix.length()
                + base64Length(generation.saltLengthBytes())
                + 1
                + base64Length(generation.hashLengthBytes());
    }

    private static long base64Length(int byteLength) {
        return (byteLength / 3L) * 4
                + switch (byteLength % 3) {
            case 1 -> 2;
            case 2 -> 3;
            default -> 0;
        };
    }
}
