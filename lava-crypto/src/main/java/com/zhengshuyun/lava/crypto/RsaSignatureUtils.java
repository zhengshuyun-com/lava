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

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * {@link CryptoUtils} 使用的 JCA RSA SHA-256 签名与验签实现。
 *
 * <p>固定使用 {@code SHA256withRSA}，即 RSASSA-PKCS1-v1_5；私钥可以来自软件 Provider 或 HSM。
 * 所有输入数组均由调用方持有，本工具不会修改。</p>
 */
final class RsaSignatureUtils {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * 工具类不允许实例化。
     */
    private RsaSignatureUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 使用 RSA 私钥生成 SHA-256 签名。
     *
     * <p>方法借用且不修改调用方传入的数据。返回值是新创建的签名字节数组。</p>
     *
     * @param privateKey RSA 私钥，可来自软件 Provider 或 HSM
     * @param data       待签名数据
     * @return 原始签名字节
     * @throws IllegalArgumentException 参数为空或私钥算法不是 RSA 时抛出
     * @throws CryptoException          JCA Provider 无法完成签名时抛出
     */
    static byte[] sha256(PrivateKey privateKey, byte[] data) {
        // 1. 在调用 Provider 前完成参数校验，使调用方错误与密码学执行失败保持明确边界。
        requireRsa(privateKey, "privateKey");
        ValidationUtils.requireNonNull(data, "data must not be null");

        try {
            // 2. 通过 JCA 初始化固定算法，允许支持该私钥的 Provider 执行签名操作。
            Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
            signer.initSign(privateKey);

            // 3. 一次性送入完整消息并生成独立签名数组，不修改调用方的数据。
            signer.update(data);
            return signer.sign();
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Failed to create RSA-SHA256 signature", exception);
        }
    }

    /**
     * 使用 RSA 公钥验证 SHA-256 签名。
     *
     * <p>签名内容不匹配返回 {@code false}；签名格式无法处理、JCA 环境或密钥配置错误抛出异常。</p>
     *
     * @param publicKey RSA 公钥
     * @param data      已签名数据
     * @param signature 原始签名字节
     * @return 签名是否有效
     * @throws IllegalArgumentException 参数为空或公钥算法不是 RSA 时抛出
     * @throws CryptoException          JCA Provider 无法完成验签时抛出
     */
    static boolean verifySha256(PublicKey publicKey, byte[] data, byte[] signature) {
        // 1. 先校验密钥和输入，签名不匹配仍由 verify 返回 false，不与参数错误混淆。
        requireRsa(publicKey, "publicKey");
        ValidationUtils.requireNonNull(data, "data must not be null");
        ValidationUtils.requireNonNull(signature, "signature must not be null");

        try {
            // 2. 使用公钥初始化与签名端完全一致的算法。
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);

            // 3. 验证完整消息；内容不匹配返回 false，无法完成验签的 JCA 错误转换为异常。
            verifier.update(data);
            return verifier.verify(signature);
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Failed to verify RSA-SHA256 signature", exception);
        }
    }

    /**
     * 要求密钥存在且由 RSA 算法实现。
     *
     * @param key           待校验密钥
     * @param parameterName 参数名称，用于生成明确的校验消息
     * @throws IllegalArgumentException 密钥为空或算法不是 RSA 时抛出
     */
    private static void requireRsa(Key key, String parameterName) {
        ValidationUtils.requireNonNull(key, parameterName + " must not be null");
        ValidationUtils.requireTrue("RSA".equalsIgnoreCase(key.getAlgorithm()),
                parameterName + " must use RSA");
    }
}
