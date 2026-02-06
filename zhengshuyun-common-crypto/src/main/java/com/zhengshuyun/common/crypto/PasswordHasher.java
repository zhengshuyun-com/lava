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

package com.zhengshuyun.common.crypto;

import com.zhengshuyun.common.core.lang.Validate;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Argon2id 密码哈希执行器
 * <p>
 * 使用 Argon2id 算法对密码进行哈希和验证, 输出 PHC 格式字符串.
 * 执行器不可变, 线程安全, 可作为单例复用.
 * <p>
 * 示例:
 * <pre>{@code
 * PasswordHasher hasher = CryptoUtil.passwordHasher()
 *     .setMemoryKiB(65536)
 *     .setIterations(3)
 *     .build();
 * String hash = hasher.hash("myPassword");
 * boolean ok = hasher.verify("myPassword", hash);
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/7
 */
public final class PasswordHasher {

    /**
     * PHC 格式前缀
     */
    private static final String PHC_PREFIX = "$argon2id$v=";

    /**
     * Argon2 版本号 (0x13 = 19)
     */
    private static final int ARGON2_VERSION = Argon2Parameters.ARGON2_VERSION_13;

    /**
     * verify 时允许的最大内存 (KiB), 防止恶意哈希串导致资源耗尽
     */
    private static final int MAX_MEMORY_KIB = 4 * 1024 * 1024;

    /**
     * verify 时允许的最大迭代次数
     */
    private static final int MAX_ITERATIONS = 100;

    /**
     * verify 时允许的最大并行度
     */
    private static final int MAX_PARALLELISM = 128;

    /**
     * Base64 编码器 (无 padding)
     */
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();

    /**
     * Base64 解码器
     */
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    /**
     * 内存大小 (KiB)
     */
    private final int memoryKiB;

    /**
     * 迭代次数
     */
    private final int iterations;

    /**
     * 并行度
     */
    private final int parallelism;

    /**
     * 盐长度 (字节)
     */
    private final int saltLengthBytes;

    /**
     * 哈希长度 (字节)
     */
    private final int hashLengthBytes;

    /**
     * 安全随机数生成器
     */
    private final SecureRandom secureRandom;

    private PasswordHasher(Builder builder) {
        Validate.isTrue(builder.memoryKiB >= 1, "memoryKiB must be >= 1");
        Validate.isTrue(builder.iterations >= 1, "iterations must be >= 1");
        Validate.isTrue(builder.parallelism >= 1, "parallelism must be >= 1");
        Validate.isTrue(builder.saltLengthBytes >= 8, "saltLengthBytes must be >= 8");
        Validate.isTrue(builder.hashLengthBytes >= 4, "hashLengthBytes must be >= 4");
        this.memoryKiB = builder.memoryKiB;
        this.iterations = builder.iterations;
        this.parallelism = builder.parallelism;
        this.saltLengthBytes = builder.saltLengthBytes;
        this.hashLengthBytes = builder.hashLengthBytes;
        this.secureRandom = new SecureRandom();
    }

    /**
     * 创建 Builder 实例
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 对密码进行哈希
     *
     * @param password 明文密码
     * @return PHC 格式哈希字符串, 如 {@code $argon2id$v=19$m=65536,t=3,p=1$<salt>$<hash>}
     */
    public String hash(String password) {
        Validate.notBlank(password, "password must not be blank");

        byte[] salt = new byte[saltLengthBytes];
        secureRandom.nextBytes(salt);

        byte[] hash = computeHash(password, salt, memoryKiB, iterations, parallelism, hashLengthBytes);
        return encode(salt, hash, memoryKiB, iterations, parallelism);
    }

    /**
     * 验证密码是否与哈希匹配
     * <p>
     * 从 encodedHash 中解析参数进行验证, 忽略 Builder 配置的参数.
     * 参数升级后旧哈希仍可验证.
     *
     * @param password    明文密码
     * @param encodedHash PHC 格式哈希字符串
     * @return 密码匹配返回 true, 否则返回 false
     */
    public boolean verify(String password, String encodedHash) {
        Validate.notBlank(password, "password must not be blank");
        Validate.notBlank(encodedHash, "encodedHash must not be blank");

        ParsedHash parsed = decode(encodedHash);
        byte[] computedHash = computeHash(
                password, parsed.salt, parsed.memoryKiB, parsed.iterations, parsed.parallelism, parsed.hash.length);

        return MessageDigest.isEqual(parsed.hash, computedHash);
    }

    /**
     * 计算 Argon2id 哈希
     */
    private static byte[] computeHash(
            String password, byte[] salt, int memoryKiB, int iterations, int parallelism, int hashLength) {
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        try {
            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(ARGON2_VERSION)
                    .withMemoryAsKB(memoryKiB)
                    .withIterations(iterations)
                    .withParallelism(parallelism)
                    .withSalt(salt)
                    .build();

            byte[] hash = new byte[hashLength];
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(params);
            generator.generateBytes(passwordBytes, hash);
            return hash;
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    /**
     * 编码为 PHC 格式字符串
     */
    private static String encode(byte[] salt, byte[] hash, int memoryKiB, int iterations, int parallelism) {
        return PHC_PREFIX + ARGON2_VERSION
                + "$m=" + memoryKiB + ",t=" + iterations + ",p=" + parallelism
                + "$" + BASE64_ENCODER.encodeToString(salt)
                + "$" + BASE64_ENCODER.encodeToString(hash);
    }

    /**
     * 解析 PHC 格式字符串
     */
    private static ParsedHash decode(String encodedHash) {
        // $argon2id$v=19$m=65536,t=3,p=1$<salt>$<hash>
        String[] parts = encodedHash.split("\\$");
        if (parts.length != 6 || !"argon2id".equals(parts[1])) {
            throw new CryptoException("Invalid Argon2id hash format");
        }

        // 解析并校验版本
        int version = parseParam(parts[2], "v");
        if (version != ARGON2_VERSION) {
            throw new CryptoException("Unsupported Argon2id version: " + version + ", expected: " + ARGON2_VERSION);
        }

        // 解析参数 m=xxx,t=xxx,p=xxx
        String[] paramParts = parts[3].split(",");
        if (paramParts.length != 3) {
            throw new CryptoException("Invalid Argon2id hash format: invalid parameters");
        }

        int memoryKiB = parseParam(paramParts[0], "m");
        int iterations = parseParam(paramParts[1], "t");
        int parallelism = parseParam(paramParts[2], "p");

        // 参数上限保护, 防止恶意哈希串导致资源耗尽
        if (memoryKiB < 1 || memoryKiB > MAX_MEMORY_KIB) {
            throw new CryptoException("Argon2id memory out of range: " + memoryKiB);
        }
        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new CryptoException("Argon2id iterations out of range: " + iterations);
        }
        if (parallelism < 1 || parallelism > MAX_PARALLELISM) {
            throw new CryptoException("Argon2id parallelism out of range: " + parallelism);
        }

        try {
            byte[] salt = BASE64_DECODER.decode(parts[4]);
            byte[] hash = BASE64_DECODER.decode(parts[5]);
            return new ParsedHash(salt, hash, memoryKiB, iterations, parallelism);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("Invalid Argon2id hash format: invalid Base64", e);
        }
    }

    /**
     * 解析单个参数
     */
    private static int parseParam(String param, String prefix) {
        if (!param.startsWith(prefix + "=")) {
            throw new CryptoException("Invalid Argon2id hash format: expected " + prefix);
        }
        try {
            return Integer.parseInt(param.substring(prefix.length() + 1));
        } catch (NumberFormatException e) {
            throw new CryptoException("Invalid Argon2id hash format: invalid " + prefix + " value", e);
        }
    }

    /**
     * 解析后的哈希数据
     */
    private record ParsedHash(byte[] salt, byte[] hash, int memoryKiB, int iterations, int parallelism) {
    }

    /**
     * 密码哈希执行器构建器
     *
     * @author Toint
     * @since 2026/2/7
     */
    public static final class Builder {

        /**
         * 内存大小 (KiB), OWASP 推荐最低值
         */
        private int memoryKiB = 65536;

        /**
         * 迭代次数, OWASP 推荐
         */
        private int iterations = 3;

        /**
         * 并行度
         */
        private int parallelism = 1;

        /**
         * 盐长度 (字节), 128-bit
         */
        private int saltLengthBytes = 16;

        /**
         * 哈希长度 (字节), 256-bit
         */
        private int hashLengthBytes = 32;

        private Builder() {
        }

        /**
         * 设置内存大小
         *
         * @param memoryKiB 内存大小 (KiB), 默认 65536 (64 MiB)
         * @return this
         */
        public Builder setMemoryKiB(int memoryKiB) {
            this.memoryKiB = memoryKiB;
            return this;
        }

        /**
         * 设置迭代次数
         *
         * @param iterations 迭代次数, 默认 3
         * @return this
         */
        public Builder setIterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        /**
         * 设置并行度
         *
         * @param parallelism 并行度, 默认 1
         * @return this
         */
        public Builder setParallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        /**
         * 设置盐长度
         *
         * @param saltLengthBytes 盐长度 (字节), 默认 16 (128-bit)
         * @return this
         */
        public Builder setSaltLengthBytes(int saltLengthBytes) {
            this.saltLengthBytes = saltLengthBytes;
            return this;
        }

        /**
         * 设置哈希长度
         *
         * @param hashLengthBytes 哈希长度 (字节), 默认 32 (256-bit)
         * @return this
         */
        public Builder setHashLengthBytes(int hashLengthBytes) {
            this.hashLengthBytes = hashLengthBytes;
            return this;
        }

        /**
         * 构建 PasswordHasher 实例
         *
         * @return PasswordHasher 实例
         */
        public PasswordHasher build() {
            return new PasswordHasher(this);
        }
    }
}
