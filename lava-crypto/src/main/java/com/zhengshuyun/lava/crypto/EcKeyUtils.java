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
 * 使用 JDK Provider 生成 EC 密钥，且不修改全局 Provider 列表。
 */
public final class EcKeyUtils {

    static final String SUN_EC_PROVIDER = "SunEC";

    /**
     * JDK SunEC 实现及本 API 支持的曲线。
     */
    public enum Curve {
        /** NIST P-256 曲线。 */
        P256("secp256r1"),
        /** NIST P-384 曲线。 */
        P384("secp384r1"),
        /** NIST P-521 曲线。 */
        P521("secp521r1");

        private final String standardName;

        Curve(String standardName) {
            this.standardName = standardName;
        }

        /**
         * 返回 JCA 使用的标准曲线名称。
         *
         * @return 标准曲线名称
         */
        public String standardName() {
            return standardName;
        }
    }

    private EcKeyUtils() {
    }

    /**
     * 使用新的安全随机源生成 P-256 密钥对。
     *
     * @return P-256 EC 密钥对
     */
    public static KeyPair generate() {
        return generate(Curve.P256);
    }

    /**
     * 使用新的安全随机源生成指定曲线的密钥对。
     *
     * @param curve EC 曲线
     * @return 指定曲线的 EC 密钥对
     */
    public static KeyPair generate(Curve curve) {
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
    public static KeyPair generate(Curve curve, SecureRandom secureRandom) {
        ValidationUtils.requireNonNull(curve, "curve must not be null");
        ValidationUtils.requireNonNull(secureRandom, "secureRandom must not be null");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", SUN_EC_PROVIDER);
            generator.initialize(new ECGenParameterSpec(curve.standardName()), secureRandom);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Failed to generate an EC key pair for " + curve, e);
        }
    }
}
