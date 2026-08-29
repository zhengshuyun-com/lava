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

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * {@link CryptoUtils} 使用的 JCA AES-GCM 认证加密实现。
 *
 * <p>本工具不会生成或管理 nonce；调用方必须保证同一密钥下 nonce 唯一。所有输入数组均为借用，
 * 不会被修改；加密和解密结果均为新创建的数组。</p>
 */
final class AesGcmUtils {

    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AUTHENTICATION_TAG_LENGTH_BITS = 128;

    /**
     * 工具类不允许实例化。
     */
    private AesGcmUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 使用 AES-GCM 加密并在结果尾部附加 128 位认证标签。
     *
     * @param key            16、24 或 32 字节 AES 密钥
     * @param nonce          非空且对当前密钥唯一的随机串
     * @param associatedData 附加认证数据，可以为空数组
     * @param plaintext      明文，可以为空数组
     * @return 密文与认证标签
     * @throws IllegalArgumentException 参数无效时抛出
     * @throws CryptoException          JCA 环境无法完成加密时抛出
     */
    static byte[] encrypt(
            byte[] key, byte[] nonce, byte[] associatedData, byte[] plaintext) {
        return crypt(Cipher.ENCRYPT_MODE, key, nonce, associatedData, plaintext,
                "Failed to encrypt with AES-GCM");
    }

    /**
     * 验证 128 位认证标签并使用 AES-GCM 解密。
     *
     * @param key            16、24 或 32 字节 AES 密钥
     * @param nonce          加密时使用的随机串
     * @param associatedData 加密时使用的附加认证数据
     * @param ciphertext     密文与认证标签
     * @return 解密后的明文
     * @throws IllegalArgumentException 参数无效时抛出
     * @throws CryptoException          认证标签无效或 JCA 环境无法完成解密时抛出
     */
    static byte[] decrypt(
            byte[] key, byte[] nonce, byte[] associatedData, byte[] ciphertext) {
        return crypt(Cipher.DECRYPT_MODE, key, nonce, associatedData, ciphertext,
                "Failed to authenticate or decrypt AES-GCM data");
    }

    /**
     * 使用指定模式执行 AES-GCM，并统一参数校验与异常转换。
     *
     * @param operationMode  Cipher 加密或解密模式
     * @param key            AES 密钥
     * @param nonce          GCM nonce
     * @param associatedData 附加认证数据
     * @param input          明文，或包含认证标签的密文
     * @param failureMessage 密码学操作失败时的错误消息
     * @return 加密或解密结果
     * @throws IllegalArgumentException 参数无效时抛出
     * @throws CryptoException          认证失败或 JCA Provider 无法执行操作时抛出
     */
    private static byte[] crypt(
            int operationMode, byte[] key, byte[] nonce, byte[] associatedData,
            byte[] input, String failureMessage) {
        // 1. 在创建 JCA 对象前校验所有输入，使参数错误不会被包装成密码学执行失败。
        requireAesKey(key);
        ValidationUtils.requireNonNull(nonce, "nonce must not be null");
        ValidationUtils.requireTrue(nonce.length > 0, "nonce must not be empty");
        ValidationUtils.requireNonNull(associatedData, "associatedData must not be null");
        ValidationUtils.requireNonNull(input, "input must not be null");

        try {
            // 2. 使用固定 128 位认证标签初始化 Cipher；nonce 唯一性由调用方在业务层保证。
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameters = new GCMParameterSpec(AUTHENTICATION_TAG_LENGTH_BITS, nonce);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(operationMode, secretKey, parameters);

            // 3. AAD 必须在正文前送入；解密时 doFinal 同时完成认证标签校验。
            cipher.updateAAD(associatedData);
            return cipher.doFinal(input);
        } catch (GeneralSecurityException exception) {
            throw new CryptoException(failureMessage, exception);
        }
    }

    /**
     * 要求密钥使用 JCA 支持的 AES-128、AES-192 或 AES-256 长度。
     *
     * @param key 待校验密钥
     * @throws IllegalArgumentException 密钥为空或长度无效时抛出
     */
    private static void requireAesKey(byte[] key) {
        ValidationUtils.requireNonNull(key, "key must not be null");
        boolean supportedLength = key.length == 16 || key.length == 24 || key.length == 32;
        ValidationUtils.requireTrue(supportedLength,
                "key must contain 16, 24, or 32 bytes");
    }
}
