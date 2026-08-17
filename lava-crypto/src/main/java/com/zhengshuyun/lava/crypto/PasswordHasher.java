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

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/** 不可变且线程安全的 Argon2id 密码哈希器。 */
public final class PasswordHasher {

    private static final String ALGORITHM = "argon2id";
    private static final int VERSION = Argon2Parameters.ARGON2_VERSION_13;
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    private final PasswordHashPolicy policy;
    private final SecureRandom secureRandom;

    /** 使用默认策略和新的安全随机源创建密码哈希器。 */
    public PasswordHasher() {
        this(PasswordHashPolicy.DEFAULT);
    }

    /**
     * 使用指定策略和新的安全随机源创建密码哈希器。
     *
     * @param policy 密码哈希与验证资源策略
     */
    public PasswordHasher(PasswordHashPolicy policy) {
        this(policy, new SecureRandom());
    }

    /**
     * 使用指定策略和安全随机源创建密码哈希器，主要用于确定性测试和受控熵源。
     *
     * @param policy 密码哈希与验证资源策略
     * @param secureRandom 用于生成每个密码盐的安全随机源
     */
    public PasswordHasher(PasswordHashPolicy policy, SecureRandom secureRandom) {
        this.policy = ValidationUtils.requireNonNull(policy, "policy must not be null");
        this.secureRandom = ValidationUtils.requireNonNull(secureRandom, "secureRandom must not be null");
    }

    /**
     * 使用默认策略创建密码哈希器。
     *
     * @return 新的默认密码哈希器
     */
    public static PasswordHasher create() {
        return new PasswordHasher();
    }

    /**
     * 使用指定策略创建密码哈希器。
     *
     * @param policy 密码哈希与验证资源策略
     * @return 新的密码哈希器
     */
    public static PasswordHasher withPolicy(PasswordHashPolicy policy) {
        return new PasswordHasher(policy);
    }

    /**
     * 返回此哈希器使用的不可变策略。
     *
     * @return 密码哈希与验证资源策略
     */
    public PasswordHashPolicy policy() {
        return policy;
    }

    /**
     * 计算密码哈希，不接管也不修改调用方传入的数组。
     *
     * @param password 待哈希的密码字符；调用方可在调用后自行清零
     * @return Argon2id PHC 格式的密码哈希
     */
    public String hash(char[] password) {
        ValidationUtils.requireNonNull(password, "password must not be null");
        PasswordHashPolicy.Generation generation = policy.generation();
        byte[] salt = new byte[generation.saltLengthBytes()];
        synchronized (secureRandom) {
            secureRandom.nextBytes(salt);
        }
        byte[] hash = null;
        try {
            hash = computeHash(
                    password,
                    salt,
                    generation.memoryKiB(),
                    generation.iterations(),
                    generation.parallelism(),
                    generation.hashLengthBytes());
            return encode(generation, salt, hash);
        } finally {
            Arrays.fill(salt, (byte) 0);
            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
        }
    }

    /**
     * 计算密码字符串的哈希；调用方可清除秘密数据时，应优先使用 {@link #hash(char[])}。
     *
     * @param password 待哈希的密码字符串
     * @return Argon2id PHC 格式的密码哈希
     */
    public String hash(String password) {
        ValidationUtils.requireNonNull(password, "password must not be null");
        char[] chars = password.toCharArray();
        try {
            return hash(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    /**
     * 验证密码是否匹配 Argon2id PHC 哈希。仅在普通密码不匹配时返回 false；畸形 PHC 输入和
     * 资源上限违规会抛出异常。
     *
     * @param password 待验证的密码字符；调用方可在调用后自行清零
     * @param encodedHash Argon2id PHC 格式的密码哈希
     * @return 密码匹配时为 true
     * @throws CryptoException PHC 格式无效或请求资源超过验证上限时抛出
     */
    public boolean verify(char[] password, String encodedHash) {
        ValidationUtils.requireNonNull(password, "password must not be null");
        ParsedHash parsed = parse(encodedHash);
        byte[] computed = null;
        try {
            computed = computeHash(
                    password,
                    parsed.salt(),
                    parsed.memoryKiB(),
                    parsed.iterations(),
                    parsed.parallelism(),
                    parsed.hash().length);
            return MessageDigest.isEqual(parsed.hash(), computed);
        } finally {
            parsed.clear();
            if (computed != null) {
                Arrays.fill(computed, (byte) 0);
            }
        }
    }

    /**
     * 验证密码字符串是否匹配 Argon2id PHC 哈希；对于可变的秘密数据，应优先使用
     * {@link #verify(char[], String)}。
     *
     * @param password 待验证的密码字符串
     * @param encodedHash Argon2id PHC 格式的密码哈希
     * @return 密码匹配时为 true
     * @throws CryptoException PHC 格式无效或请求资源超过验证上限时抛出
     */
    public boolean verify(String password, String encodedHash) {
        ValidationUtils.requireNonNull(password, "password must not be null");
        char[] chars = password.toCharArray();
        try {
            return verify(chars, encodedHash);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    /**
     * 判断有效哈希是否应按此哈希器的生成策略重新生成。
     *
     * @param encodedHash Argon2id PHC 格式的密码哈希
     * @return 参数与当前生成策略不一致时为 true
     * @throws CryptoException PHC 格式无效或请求资源超过验证上限时抛出
     */
    public boolean needsRehash(String encodedHash) {
        ParsedHash parsed = parse(encodedHash);
        try {
            PasswordHashPolicy.Generation generation = policy.generation();
            return parsed.memoryKiB() != generation.memoryKiB()
                    || parsed.iterations() != generation.iterations()
                    || parsed.parallelism() != generation.parallelism()
                    || parsed.salt().length != generation.saltLengthBytes()
                    || parsed.hash().length != generation.hashLengthBytes();
        } finally {
            parsed.clear();
        }
    }

    private ParsedHash parse(String encodedHash) {
        ValidationUtils.requireNonNull(encodedHash, "encodedHash must not be null");
        PasswordHashPolicy.VerificationLimits limits = policy.verificationLimits();
        if (encodedHash.length() > limits.maxEncodedHashLength()) {
            throw new CryptoException("Encoded Argon2id hash exceeds the length limit");
        }

        int algorithmEnd = encodedHash.indexOf('$', 1);
        int versionEnd = algorithmEnd < 0 ? -1 : encodedHash.indexOf('$', algorithmEnd + 1);
        int parametersEnd = versionEnd < 0 ? -1 : encodedHash.indexOf('$', versionEnd + 1);
        int saltEnd = parametersEnd < 0 ? -1 : encodedHash.indexOf('$', parametersEnd + 1);
        if (!encodedHash.startsWith("$")
                || algorithmEnd < 0
                || versionEnd < 0
                || parametersEnd < 0
                || saltEnd < 0
                || encodedHash.indexOf('$', saltEnd + 1) >= 0
                || !encodedHash.regionMatches(1, ALGORITHM, 0, ALGORITHM.length())
                || algorithmEnd != ALGORITHM.length() + 1) {
            throw malformed("Invalid Argon2id PHC structure");
        }

        int version = parseNamedInteger(encodedHash, algorithmEnd + 1, versionEnd, "v");
        if (version != VERSION) {
            throw malformed("Unsupported Argon2id version: " + version);
        }

        ParameterValues parameters = parseParameters(encodedHash, versionEnd + 1, parametersEnd);
        checkLimit(parameters.memoryKiB(), limits.maxMemoryKiB(), "memory");
        checkLimit(parameters.iterations(), limits.maxIterations(), "iterations");
        checkLimit(parameters.parallelism(), limits.maxParallelism(), "parallelism");
        if (parameters.memoryKiB() < 8L * parameters.parallelism()) {
            throw malformed("Argon2id memory must be at least eight times parallelism");
        }

        String saltText = encodedHash.substring(parametersEnd + 1, saltEnd);
        String hashText = encodedHash.substring(saltEnd + 1);
        checkBase64EncodedLength(saltText, limits.maxSaltLengthBytes(), "salt");
        checkBase64EncodedLength(hashText, limits.maxHashLengthBytes(), "hash");
        byte[] salt = decodeBase64(saltText, "salt");
        byte[] hash;
        try {
            hash = decodeBase64(hashText, "hash");
        } catch (RuntimeException e) {
            Arrays.fill(salt, (byte) 0);
            throw e;
        }
        if (salt.length < 8) {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(hash, (byte) 0);
            throw malformed("Argon2id salt must contain at least 8 bytes");
        }
        if (hash.length < 4) {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(hash, (byte) 0);
            throw malformed("Argon2id hash must contain at least 4 bytes");
        }
        if (salt.length > limits.maxSaltLengthBytes()) {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(hash, (byte) 0);
            throw new CryptoException("Argon2id salt exceeds the verification limit");
        }
        if (hash.length > limits.maxHashLengthBytes()) {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(hash, (byte) 0);
            throw new CryptoException("Argon2id hash exceeds the verification limit");
        }
        return new ParsedHash(
                parameters.memoryKiB(),
                parameters.iterations(),
                parameters.parallelism(),
                salt,
                hash);
    }

    private static ParameterValues parseParameters(String text, int start, int end) {
        int firstComma = text.indexOf(',', start);
        int secondComma = firstComma < 0 ? -1 : text.indexOf(',', firstComma + 1);
        if (firstComma < 0 || secondComma < 0 || secondComma >= end
                || (text.indexOf(',', secondComma + 1) >= 0
                        && text.indexOf(',', secondComma + 1) < end)) {
            throw malformed("Invalid Argon2id parameter list");
        }
        int memory = parseNamedInteger(text, start, firstComma, "m");
        int iterations = parseNamedInteger(text, firstComma + 1, secondComma, "t");
        int parallelism = parseNamedInteger(text, secondComma + 1, end, "p");
        return new ParameterValues(memory, iterations, parallelism);
    }

    private static int parseNamedInteger(String text, int start, int end, String name) {
        if (end - start < 3 || text.charAt(start) != name.charAt(0) || text.charAt(start + 1) != '=') {
            throw malformed("Expected Argon2id parameter " + name);
        }
        int value = 0;
        for (int index = start + 2; index < end; index++) {
            char current = text.charAt(index);
            if (current < '0' || current > '9') {
                throw malformed("Invalid Argon2id parameter " + name);
            }
            int digit = current - '0';
            if (value > (Integer.MAX_VALUE - digit) / 10) {
                throw new CryptoException("Argon2id parameter " + name + " is too large");
            }
            value = value * 10 + digit;
        }
        if (value == 0) {
            throw malformed("Argon2id parameter " + name + " must be positive");
        }
        return value;
    }

    private static void checkLimit(int value, int maximum, String name) {
        if (value > maximum) {
            throw new CryptoException(
                    "Argon2id " + name + " exceeds the verification limit");
        }
    }

    private static void checkBase64EncodedLength(String text, int maxDecodedBytes, String name) {
        if (text.isEmpty()) {
            throw malformed("Argon2id " + name + " must not be empty");
        }
        long maximumCharacters = ((long) maxDecodedBytes + 2) / 3 * 4;
        if (text.length() > maximumCharacters) {
            throw new CryptoException(
                    "Argon2id " + name + " exceeds the verification limit");
        }
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            boolean base64 = (current >= 'A' && current <= 'Z')
                    || (current >= 'a' && current <= 'z')
                    || (current >= '0' && current <= '9')
                    || current == '+'
                    || current == '/';
            if (!base64) {
                throw malformed("Invalid unpadded Base64 in Argon2id " + name);
            }
        }
    }

    private static byte[] decodeBase64(String value, String name) {
        try {
            return BASE64_DECODER.decode(value);
        } catch (IllegalArgumentException e) {
            throw new CryptoException(
                    "Invalid Base64 in Argon2id " + name, e);
        }
    }

    private static byte[] computeHash(
            char[] password,
            byte[] salt,
            int memoryKiB,
            int iterations,
            int parallelism,
            int hashLength) {
        byte[] passwordBytes = encodeUtf8(password);
        try {
            Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(VERSION)
                    .withMemoryAsKB(memoryKiB)
                    .withIterations(iterations)
                    .withParallelism(parallelism)
                    .withSalt(salt)
                    .build();
            byte[] result = new byte[hashLength];
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);
            generator.generateBytes(passwordBytes, result);
            return result;
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    private static byte[] encodeUtf8(char[] password) {
        ByteBuffer encoded;
        try {
            encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(password));
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("password contains malformed UTF-16", exception);
        }
        byte[] result = new byte[encoded.remaining()];
        encoded.get(result);
        if (encoded.hasArray()) {
            Arrays.fill(
                    encoded.array(),
                    encoded.arrayOffset(),
                    encoded.arrayOffset() + encoded.capacity(),
                    (byte) 0);
        }
        return result;
    }

    private static String encode(
            PasswordHashPolicy.Generation generation, byte[] salt, byte[] hash) {
        return "$" + ALGORITHM
                + "$v=" + VERSION
                + "$m=" + generation.memoryKiB()
                + ",t=" + generation.iterations()
                + ",p=" + generation.parallelism()
                + "$" + BASE64_ENCODER.encodeToString(salt)
                + "$" + BASE64_ENCODER.encodeToString(hash);
    }

    private static CryptoException malformed(String message) {
        return new CryptoException(message);
    }

    private record ParameterValues(int memoryKiB, int iterations, int parallelism) {
    }

    private static final class ParsedHash {

        private final int memoryKiB;
        private final int iterations;
        private final int parallelism;
        private final byte[] salt;
        private final byte[] hash;

        ParsedHash(
                int memoryKiB,
                int iterations,
                int parallelism,
                byte[] salt,
                byte[] hash) {
            this.memoryKiB = memoryKiB;
            this.iterations = iterations;
            this.parallelism = parallelism;
            this.salt = salt;
            this.hash = hash;
        }

        int memoryKiB() {
            return memoryKiB;
        }

        int iterations() {
            return iterations;
        }

        int parallelism() {
            return parallelism;
        }

        byte[] salt() {
            return salt;
        }

        byte[] hash() {
            return hash;
        }

        void clear() {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(hash, (byte) 0);
        }
    }
}
