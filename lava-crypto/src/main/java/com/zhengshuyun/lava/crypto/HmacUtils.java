/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.crypto;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * HMAC 消息认证码工具。
 *
 * <p>标准算法通过 JCA 获取，不绑定具体 Provider，也不修改 JVM 全局 Provider 列表。
 * 也可通过 {@link CryptoUtils} 使用统一便捷入口。</p>
 */
public final class HmacUtils {

    /** JCA 定义的 HMAC-SHA-256 标准算法名。 */
    private static final String HMAC_SHA_256 = "HmacSHA256";

    /** 小写十六进制编码器。 */
    private static final HexFormat LOWERCASE_HEX = HexFormat.of();

    /** 禁止实例化工具类。 */
    private HmacUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 计算 HMAC-SHA-256，并返回原始的 32 字节结果。
     *
     * <p>方法不会修改调用方传入的密钥和数据数组。密钥不能为空；数据可以为空数组。
     *
     * @param key HMAC 密钥
     * @param data 待认证数据
     * @return 新创建的 32 字节 HMAC 结果
     * @throws IllegalArgumentException 密钥或数据为 {@code null}，或者密钥为空时抛出
     * @throws CryptoException 当前 JCA 环境无法提供 HMAC-SHA-256 时抛出
     */
    public static byte[] sha256(byte[] key, byte[] data) {
        // 1. 在创建 JCA 对象前校验输入，明确区分参数错误与 Provider 执行失败。
        ValidationUtils.requireNonNull(key, "key must not be null");
        ValidationUtils.requireTrue(key.length > 0, "key must not be empty");
        ValidationUtils.requireNonNull(data, "data must not be null");

        try {
            // 2. 使用固定算法和调用方密钥初始化 Mac，不绑定或修改全局 Provider。
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(key, HMAC_SHA_256));

            // 3. 一次性认证完整数据并返回独立结果数组，不修改调用方输入。
            return mac.doFinal(data);
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Failed to compute HMAC-SHA-256", exception);
        }
    }

    /**
     * 计算 HMAC-SHA-256，并编码为小写十六进制字符串。
     *
     * @param key HMAC 密钥
     * @param data 待认证数据
     * @return 长度为 64 的小写十六进制结果
     * @throws IllegalArgumentException 密钥或数据为 {@code null}，或者密钥为空时抛出
     * @throws CryptoException 当前 JCA 环境无法提供 HMAC-SHA-256 时抛出
     */
    public static String sha256Hex(byte[] key, byte[] data) {
        return LOWERCASE_HEX.formatHex(sha256(key, data));
    }

    /**
     * 将字符串按 UTF-8 编码后计算 HMAC-SHA-256，并返回小写十六进制字符串。
     *
     * @param key HMAC 密钥文本
     * @param data 待认证文本
     * @return 长度为 64 的小写十六进制结果
     * @throws IllegalArgumentException 密钥或数据为 {@code null}，或者密钥为空时抛出
     * @throws CryptoException 当前 JCA 环境无法提供 HMAC-SHA-256 时抛出
     */
    public static String sha256Hex(String key, String data) {
        ValidationUtils.requireNonNull(key, "key must not be null");
        ValidationUtils.requireNonNull(data, "data must not be null");
        return sha256Hex(
                key.getBytes(StandardCharsets.UTF_8),
                data.getBytes(StandardCharsets.UTF_8));
    }
}
