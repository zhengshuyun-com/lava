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

import com.zhengshuyun.lava.core.lang.Validate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

/**
 * EC 密钥对生成执行器
 * <p>
 * 使用 Bouncy Castle Provider 生成 EC 密钥对, 输出标准 JCA {@link KeyPair}.
 * 生成结果是 EC 密钥材料, 可用于 ECDSA(ES256/ES384/ES512)签名, 也可用于 ECDH 等其他 EC 场景.
 * EC 是密钥体系, ECDSA 是签名算法.
 * <p>
 * 示例:
 * <pre>{@code
 * KeyPair keyPair = CryptoUtil.ecKeyGenerator()
 *     .setCurve(EcCurves.SECP256R1)
 *     .build()
 *     .generate();
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/7
 */
public final class EcKeyGenerator {

    /**
     * 曲线名称
     */
    private final String curve;

    private EcKeyGenerator(Builder builder) {
        this.curve = Validate.notBlank(builder.curve, "curve must not be blank");
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
     * 生成 EC 密钥对
     * <p>
     * 注意: 该方法生成的是 EC 密钥, 不是"ECDSA 密钥"这一独立算法类型.
     *
     * @return JCA 标准 KeyPair
     * @throws CryptoException 生成失败时抛出
     */
    public KeyPair generate() {
        CryptoUtil.ensureBouncyCastleProvider();
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new ECGenParameterSpec(curve));
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new CryptoException("Failed to generate EC key pair with curve: " + curve, e);
        }
    }

    /**
     * EC 密钥对生成执行器构建器
     *
     * @author Toint
     * @since 2026/2/7
     */
    public static final class Builder {

        /**
         * 曲线名称, 默认 P-256
         */
        private String curve = EcCurves.SECP256R1;

        private Builder() {
        }

        /**
         * 设置曲线名称
         *
         * @param curve 曲线名称, 默认 {@link EcCurves#SECP256R1}
         * @return this
         * @see EcCurves
         */
        public Builder setCurve(String curve) {
            this.curve = curve;
            return this;
        }

        /**
         * 构建 EcKeyGenerator 实例
         *
         * @return EcKeyGenerator 实例
         */
        public EcKeyGenerator build() {
            return new EcKeyGenerator(this);
        }
    }
}
