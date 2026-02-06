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
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Toint
 * @since 2026/2/6
 */
class JwtUtilTest {

    private static final String SECRET = "test-secret-key";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);

    @Test
    void testSign() {
        String token = JwtUtil.sign(ALGORITHM, builder -> builder
                .withIssuer("test-issuer")
                .withSubject("test-user")
                .withClaim("user-id", 123)
                .withClaim("role", "admin")
                .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        );

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");
    }

    @Test
    void testVerify() {
        Instant now = Instant.now();
        String token = JwtUtil.sign(ALGORITHM, builder -> builder
                .withIssuer("my-app")
                .withSubject("user-123")
                .withClaim("username", "alice")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(1, ChronoUnit.HOURS)))
        );

        DecodedJWT jwt = JwtUtil.verify(ALGORITHM, token);

        assertEquals("my-app", jwt.getIssuer());
        assertEquals("user-123", jwt.getSubject());
        assertEquals("alice", jwt.getClaim("username").asString());
        assertNotNull(jwt.getExpiresAt());
    }

    @Test
    void testVerifyWithCustomizer() {
        String token = JwtUtil.sign(ALGORITHM, builder -> builder
                .withIssuer("my-app")
                .withAudience("my-service")
                .withSubject("user-456")
        );

        DecodedJWT jwt = JwtUtil.verify(ALGORITHM, token, verification -> verification
                .withIssuer("my-app")
                .withAudience("my-service")
        );

        assertEquals("my-app", jwt.getIssuer());
        assertEquals("user-456", jwt.getSubject());
        assertTrue(jwt.getAudience().contains("my-service"));
    }

    @Test
    void testVerifyFailsWithWrongSecret() {
        String token = JwtUtil.sign(ALGORITHM, builder -> builder
                .withSubject("test")
        );

        Algorithm wrongAlgorithm = Algorithm.HMAC256("wrong-secret");

        assertThrows(JwtException.class, () -> JwtUtil.verify(wrongAlgorithm, token));
    }

    @Test
    void testVerifyFailsWithExpiredToken() {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        String token = JwtUtil.sign(ALGORITHM, builder -> builder
                .withSubject("test")
                .withExpiresAt(Date.from(past))
        );

        assertThrows(JwtException.class, () -> JwtUtil.verify(ALGORITHM, token));
    }

    @Test
    void testDecode() {
        String token = JwtUtil.sign(ALGORITHM, builder -> builder
                .withIssuer("test-issuer")
                .withSubject("test-subject")
                .withClaim("custom-claim", "custom-value")
        );

        DecodedJWT jwt = JwtUtil.decode(token);

        assertEquals("test-issuer", jwt.getIssuer());
        assertEquals("test-subject", jwt.getSubject());
        assertEquals("custom-value", jwt.getClaim("custom-claim").asString());
    }

    @Test
    void testDecodeInvalidToken() {
        assertThrows(JwtException.class, () -> JwtUtil.decode("invalid.token.here"));
    }

    @Test
    void testSignWithNullCustomizer() {
        String token = JwtUtil.sign(ALGORITHM, null);
        assertNotNull(token);

        DecodedJWT jwt = JwtUtil.decode(token);
        assertNotNull(jwt);
    }

    @Test
    void testVerifyWithNullCustomizer() {
        String token = JwtUtil.sign(ALGORITHM, builder -> builder
                .withSubject("test")
        );

        DecodedJWT jwt = JwtUtil.verify(ALGORITHM, token, null);
        assertEquals("test", jwt.getSubject());
    }
}
