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

package com.zhengshuyun.common.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

import java.util.function.Consumer;

/**
 * JWT 工具类
 * <p>
 * 提供基于 auth0 java-jwt 的简化封装, 统一异常处理和减少仪式代码.
 *
 * @author Toint
 * @since 2026/2/6
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    /**
     * 签发 JWT token
     *
     * @param algorithm  签名算法, 不能为 null
     * @param customizer 用于配置 JWT 内容的自定义函数 (如设置 issuer, claims, expiresAt 等)
     * @return 签名后的 JWT 字符串
     * @throws JwtException 签发失败时抛出
     */
    public static String sign(Algorithm algorithm, Consumer<JWTCreator.Builder> customizer) {
        try {
            JWTCreator.Builder builder = JWT.create();
            if (customizer != null) {
                customizer.accept(builder);
            }
            return builder.sign(algorithm);
        } catch (Exception e) {
            throw new JwtException("Failed to sign JWT", e);
        }
    }

    /**
     * 验证并解码 JWT token
     *
     * @param algorithm 签名算法, 必须与签发时使用的算法一致
     * @param token     待验证的 JWT 字符串
     * @return 验证成功后的解码结果
     * @throws JwtException 验证失败或解码失败时抛出
     */
    public static DecodedJWT verify(Algorithm algorithm, String token) {
        return verify(algorithm, token, null);
    }

    /**
     * 验证并解码 JWT token (支持额外校验配置)
     *
     * @param algorithm  签名算法, 必须与签发时使用的算法一致
     * @param token      待验证的 JWT 字符串
     * @param customizer 用于配置额外校验规则的自定义函数 (如校验 issuer, audience 等)
     * @return 验证成功后的解码结果
     * @throws JwtException 验证失败或解码失败时抛出
     */
    public static DecodedJWT verify(Algorithm algorithm, String token, Consumer<Verification> customizer) {
        try {
            Verification verification = JWT.require(algorithm);
            if (customizer != null) {
                customizer.accept(verification);
            }
            return verification.build().verify(token);
        } catch (Exception e) {
            throw new JwtException("Failed to verify JWT", e);
        }
    }

    /**
     * 解码 JWT token (不验证签名)
     * <p>
     * 警告: 此方法仅解析 token 结构, 不验证签名有效性, 不应用于安全敏感场景.
     *
     * @param token JWT 字符串
     * @return 解码结果
     * @throws JwtException 解码失败时抛出
     */
    public static DecodedJWT decode(String token) {
        try {
            return JWT.decode(token);
        } catch (Exception e) {
            throw new JwtException("Failed to decode JWT", e);
        }
    }
}
