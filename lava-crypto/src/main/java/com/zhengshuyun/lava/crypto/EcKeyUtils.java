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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

/**
 * {@link CryptoUtils} 使用的 EC 密钥生成实现，且不修改全局 Provider 列表。
 */
final class EcKeyUtils {

    static final String SUN_EC_PROVIDER = "SunEC";

    /**
     * 工具类不允许实例化。
     */
    private EcKeyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 使用新的安全随机源生成 P-256 密钥对。
     *
     * @return P-256 EC 密钥对
     */
    static KeyPair generate() {
        return generate(CryptoUtils.EcCurve.P256);
    }

    /**
     * 使用新的安全随机源生成指定曲线的密钥对。
     *
     * @param curve EC 曲线
     * @return 指定曲线的 EC 密钥对
     */
    static KeyPair generate(CryptoUtils.EcCurve curve) {
        return generate(curve, new SecureRandom());
    }

    /**
     * 使用指定随机源生成指定曲线的密钥对。
     *
     * @param curve        EC 曲线
     * @param secureRandom 用于生成密钥材料的安全随机源
     * @return 指定曲线的 EC 密钥对
     * @throws CryptoException JDK Provider 不支持所需 EC 操作时抛出
     */
    static KeyPair generate(CryptoUtils.EcCurve curve, SecureRandom secureRandom) {
        // 1. 先校验曲线和随机源，避免将调用方参数错误包装成密码学执行失败。
        ValidationUtils.requireNonNull(curve, "curve must not be null");
        ValidationUtils.requireNonNull(secureRandom, "secureRandom must not be null");

        try {
            // 2. 显式选择 JDK SunEC 和标准曲线，不修改 JVM 全局 Provider 列表。
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", SUN_EC_PROVIDER);
            generator.initialize(new ECGenParameterSpec(curve.standardName()), secureRandom);

            // 3. 由调用方提供或新建的安全随机源生成完整密钥对。
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Failed to generate an EC key pair for " + curve, exception);
        }
    }
}
