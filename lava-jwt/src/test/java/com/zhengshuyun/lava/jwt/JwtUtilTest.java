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

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JwtUtil 测试
 *
 * @author Toint
 * @since 2026/2/8
 */
class JwtUtilTest {

    @Test
    @DisplayName("创建与解码")
    void testCreateAndDecode() {
        Algorithm algorithm = Algorithm.HMAC256("lava-jwt-secret");

        String token = JwtUtil.create()
                .withIssuer("lava")
                .withSubject("user-1001")
                .sign(algorithm);

        DecodedJWT decoded = JwtUtil.decode(token);
        assertEquals("lava", decoded.getIssuer());
        assertEquals("user-1001", decoded.getSubject());
    }

    @Test
    @DisplayName("验签与验证")
    void testRequireAndVerify() {
        Algorithm algorithm = Algorithm.HMAC256("lava-jwt-secret");

        String token = JwtUtil.create()
                .withIssuer("lava")
                .withSubject("user-1002")
                .sign(algorithm);

        DecodedJWT verified = JwtUtil.require(algorithm)
                .withIssuer("lava")
                .build()
                .verify(token);

        assertEquals("user-1002", verified.getSubject());
    }
}
