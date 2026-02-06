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

package com.zhengshuyun.common.crypto;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    private static final Logger log = LoggerFactory.getLogger(CryptoUtilTest.class);

    @DisplayName("Argon2id 哈希与验证 - 正确密码")
    @Test
    void testHash_correctPassword() {
        PasswordHasher hasher = CryptoUtil.passwordHasher()
                .setMemoryKiB(1024)
                .setIterations(1)
                .build();

        String hash = hasher.hash("myPassword");
        log.info("hash: {}", hash);

        assertTrue(hash.startsWith("$argon2id$v=19$"));
        assertTrue(hasher.verify("myPassword", hash));
    }

    @DisplayName("Argon2id 哈希与验证 - 错误密码")
    @Test
    void testHash_wrongPassword() {
        PasswordHasher hasher = CryptoUtil.passwordHasher()
                .setMemoryKiB(1024)
                .setIterations(1)
                .build();

        String hash = hasher.hash("myPassword");
        assertFalse(hasher.verify("wrongPassword", hash));
    }

    @DisplayName("Argon2id 哈希 - 不同盐产生不同哈希")
    @Test
    void testHash_differentSalts() {
        PasswordHasher hasher = CryptoUtil.passwordHasher()
                .setMemoryKiB(1024)
                .setIterations(1)
                .build();

        String hash1 = hasher.hash("samePassword");
        String hash2 = hasher.hash("samePassword");
        assertNotEquals(hash1, hash2);

        assertTrue(hasher.verify("samePassword", hash1));
        assertTrue(hasher.verify("samePassword", hash2));
    }

    @DisplayName("Argon2id 哈希 - 自定义参数")
    @Test
    void testHash_customParameters() {
        PasswordHasher hasher = CryptoUtil.passwordHasher()
                .setMemoryKiB(2048)
                .setIterations(2)
                .setParallelism(2)
                .setSaltLengthBytes(32)
                .setHashLengthBytes(64)
                .build();

        String hash = hasher.hash("testPassword");
        log.info("custom hash: {}", hash);

        assertTrue(hash.contains("m=2048,t=2,p=2"));
        assertTrue(hasher.verify("testPassword", hash));
    }

    @DisplayName("Argon2id 验证 - 参数升级后旧哈希仍可验证")
    @Test
    void testVerify_parameterUpgrade() {
        PasswordHasher oldHasher = CryptoUtil.passwordHasher()
                .setMemoryKiB(1024)
                .setIterations(1)
                .build();

        String oldHash = oldHasher.hash("password123");

        PasswordHasher newHasher = CryptoUtil.passwordHasher()
                .setMemoryKiB(2048)
                .setIterations(2)
                .build();

        assertTrue(newHasher.verify("password123", oldHash));
    }

    @DisplayName("EC 密钥对生成 - 默认曲线 P-256")
    @Test
    void testEcKeyGenerator_defaultCurve() {
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .build()
                .generate();

        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
        assertInstanceOf(ECPublicKey.class, keyPair.getPublic());
        assertInstanceOf(ECPrivateKey.class, keyPair.getPrivate());
    }

    @DisplayName("EC 密钥对生成 - P-384 曲线")
    @Test
    void testEcKeyGenerator_secp384r1() {
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP384R1)
                .build()
                .generate();

        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }

    @DisplayName("EC 密钥对生成 - P-521 曲线")
    @Test
    void testEcKeyGenerator_secp521r1() {
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP521R1)
                .build()
                .generate();

        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }

    @DisplayName("PEM 往返编解码 - 私钥")
    @Test
    void testPem_privateKeyRoundTrip() {
        KeyPair keyPair = CryptoUtil.ecKeyGenerator().build().generate();
        ECPrivateKey originalKey = (ECPrivateKey) keyPair.getPrivate();

        String pem = CryptoUtil.toPem(originalKey);
        log.info("private key PEM:\n{}", pem);

        assertTrue(pem.startsWith("-----BEGIN PRIVATE KEY-----"));
        assertTrue(pem.strip().endsWith("-----END PRIVATE KEY-----"));

        ECPrivateKey decodedKey = CryptoUtil.readEcPrivateKey(pem);
        assertEquals(originalKey.getS(), decodedKey.getS());
    }

    @DisplayName("PEM 往返编解码 - 公钥")
    @Test
    void testPem_publicKeyRoundTrip() {
        KeyPair keyPair = CryptoUtil.ecKeyGenerator().build().generate();
        ECPublicKey originalKey = (ECPublicKey) keyPair.getPublic();

        String pem = CryptoUtil.toPem(originalKey);
        log.info("public key PEM:\n{}", pem);

        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertTrue(pem.strip().endsWith("-----END PUBLIC KEY-----"));

        ECPublicKey decodedKey = CryptoUtil.readEcPublicKey(pem);
        assertEquals(originalKey.getW(), decodedKey.getW());
    }

    @DisplayName("PEM 往返编解码 - 三种曲线")
    @Test
    void testPem_allCurves() {
        for (String curve : new String[]{EcCurves.SECP256R1, EcCurves.SECP384R1, EcCurves.SECP521R1}) {
            KeyPair keyPair = CryptoUtil.ecKeyGenerator().setCurve(curve).build().generate();

            String privatePem = CryptoUtil.toPem(keyPair.getPrivate());
            String publicPem = CryptoUtil.toPem(keyPair.getPublic());

            ECPrivateKey decodedPrivate = CryptoUtil.readEcPrivateKey(privatePem);
            ECPublicKey decodedPublic = CryptoUtil.readEcPublicKey(publicPem);

            assertEquals(((ECPrivateKey) keyPair.getPrivate()).getS(), decodedPrivate.getS());
            assertEquals(((ECPublicKey) keyPair.getPublic()).getW(), decodedPublic.getW());
        }
    }

    @DisplayName("集成测试 - EC 密钥对配合 JWT 签名验证")
    @Test
    void testIntegration_ecWithJwt() {
        KeyPair keyPair = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();

        Algorithm algorithm = Algorithm.ECDSA256(
                (ECPublicKey) keyPair.getPublic(),
                (ECPrivateKey) keyPair.getPrivate());

        String token = JWT.create()
                .withIssuer("test")
                .withSubject("user-123")
                .sign(algorithm);

        log.info("JWT token: {}", token);

        DecodedJWT decoded = JWT.require(algorithm)
                .withIssuer("test")
                .build()
                .verify(token);

        assertEquals("user-123", decoded.getSubject());
    }

    @DisplayName("集成测试 - PEM 持久化后恢复密钥用于 JWT")
    @Test
    void testIntegration_pemPersistenceWithJwt() {
        KeyPair original = CryptoUtil.ecKeyGenerator()
                .setCurve(EcCurves.SECP256R1)
                .build()
                .generate();

        String privatePem = CryptoUtil.toPem(original.getPrivate());
        String publicPem = CryptoUtil.toPem(original.getPublic());

        ECPrivateKey restoredPrivate = CryptoUtil.readEcPrivateKey(privatePem);
        ECPublicKey restoredPublic = CryptoUtil.readEcPublicKey(publicPem);

        Algorithm signAlgorithm = Algorithm.ECDSA256(restoredPublic, restoredPrivate);
        String token = JWT.create()
                .withSubject("restored-user")
                .sign(signAlgorithm);

        Algorithm verifyAlgorithm = Algorithm.ECDSA256(restoredPublic, null);
        DecodedJWT decoded = JWT.require(verifyAlgorithm)
                .build()
                .verify(token);

        assertEquals("restored-user", decoded.getSubject());
    }
}
