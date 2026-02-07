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

package com.zhengshuyun.lava.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

/**
 * JWT 工具类
 * <p>
 * 统一委托 {@link JWT} 的常用静态入口, 保持 Lava 工具类 API 风格一致.
 *
 * @author Toint
 * @since 2026/2/8
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    /**
     * 创建 JWT 构建器
     *
     * @return JWT 构建器
     */
    public static JWTCreator.Builder create() {
        return JWT.create();
    }

    /**
     * 创建 JWT 验证器构建器
     *
     * @param algorithm 签名算法
     * @return JWT 验证器构建器
     */
    public static Verification require(Algorithm algorithm) {
        return JWT.require(algorithm);
    }

    /**
     * 解析 JWT (仅解码, 不验签)
     *
     * @param token JWT token
     * @return 解码后的 JWT
     */
    public static DecodedJWT decode(String token) {
        return JWT.decode(token);
    }
}
