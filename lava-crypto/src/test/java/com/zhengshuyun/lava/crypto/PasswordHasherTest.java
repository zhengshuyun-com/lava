/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.crypto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private static final PasswordHashPolicy FAST_POLICY = new PasswordHashPolicy(
            new PasswordHashPolicy.Generation(1_024, 1, 1, 8, 16),
            new PasswordHashPolicy.VerificationLimits(2_048, 3, 2, 32, 32));

    @Test
    void defaultsMatchTheDocumentedSecurityPolicy() {
        PasswordHashPolicy policy = PasswordHashPolicy.defaults();

        assertTrue(policy == PasswordHashPolicy.DEFAULT);
        assertTrue(policy.generation().memoryKiB() == 65_536);
        assertTrue(policy.generation().iterations() == 3);
        assertTrue(policy.generation().parallelism() == 1);
        assertTrue(policy.generation().saltLengthBytes() == 16);
        assertTrue(policy.generation().hashLengthBytes() == 32);
        assertTrue(policy.verificationLimits().maxMemoryKiB() == 262_144);
        assertTrue(policy.verificationLimits().maxIterations() == 10);
        assertTrue(policy.verificationLimits().maxParallelism() == 16);
        assertTrue(policy.verificationLimits().maxSaltLengthBytes() == 64);
        assertTrue(policy.verificationLimits().maxHashLengthBytes() == 64);
    }

    @Test
    void roundTripsUnicodeEmptyAndWhitespacePasswordsWithoutChangingCallerArray() {
        PasswordHasher hasher = PasswordHasher.withPolicy(FAST_POLICY);
        for (String password : List.of("", "   ", "密码🔐\u0000value")) {
            char[] chars = password.toCharArray();
            char[] original = chars.clone();

            String encoded = hasher.hash(chars);

            assertArrayEquals(original, chars);
            assertTrue(hasher.verify(chars, encoded));
            assertFalse(hasher.verify((password + "x").toCharArray(), encoded));
            assertFalse(hasher.needsRehash(encoded));
        }
    }

    @Test
    void rejectsMalformedUtf16InsteadOfCollapsingDistinctPasswords() {
        PasswordHasher hasher = PasswordHasher.withPolicy(FAST_POLICY);

        assertThrows(IllegalArgumentException.class, () -> hasher.hash(new char[]{'a', '\ud800'}));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(new char[]{'a', '\ud801'}));
        String encoded = hasher.hash("valid");
        assertThrows(IllegalArgumentException.class,
                () -> hasher.verify(new char[]{'a', '\udc00'}, encoded));
    }

    @Test
    void verifiesKnownExternalArgon2idVector() {
        String external = "$argon2id$v=19$m=65536,t=3,p=4$"
                + "G+p7YJzjVw/NEAFaqKJnLg$"
                + "3CO8znSvBuO/8j4AvAcTiKXw0tUIOZskYtdKvZceaUQ";

        assertTrue(new PasswordHasher().verify("zhengshuyun", external));
        assertFalse(new PasswordHasher().verify("wrong", external));
    }

    @Test
    void needsRehashComparesGenerationParametersButStillVerifiesOldHashes() {
        PasswordHasher oldHasher = PasswordHasher.withPolicy(FAST_POLICY);
        String encoded = oldHasher.hash("upgrade-me");
        PasswordHashPolicy upgradedPolicy = new PasswordHashPolicy(
                new PasswordHashPolicy.Generation(1_024, 2, 1, 16, 24),
                new PasswordHashPolicy.VerificationLimits(2_048, 3, 2, 32, 32));
        PasswordHasher upgraded = PasswordHasher.withPolicy(upgradedPolicy);

        assertTrue(upgraded.verify("upgrade-me", encoded));
        assertTrue(upgraded.needsRehash(encoded));
    }

    @Test
    void mismatchReturnsFalseAndInvalidHashesUseClearCryptoFailures() {
        PasswordHasher hasher = PasswordHasher.withPolicy(FAST_POLICY);
        String valid = hasher.hash("password");

        assertFalse(hasher.verify("different", valid));
        CryptoException malformed = assertThrows(CryptoException.class,
                () -> hasher.verify("password", "not-a-phc-value"));
        assertTrue(malformed.getMessage().contains("PHC"));
        assertThrows(CryptoException.class,
                () -> hasher.verify("password", valid.replace("argon2id", "argon2i")));
        assertThrows(CryptoException.class,
                () -> hasher.verify("password", valid.replace("v=19", "v=16")));
        assertThrows(CryptoException.class,
                () -> hasher.verify("password", valid.replace("m=1024", "m=x")));
        assertThrows(CryptoException.class,
                () -> hasher.verify(
                        "password", valid.substring(0, valid.lastIndexOf('$') + 1) + "!!!!"));
        CryptoException overLimit = assertThrows(CryptoException.class,
                () -> hasher.verify("password", valid.replace("m=1024", "m=2049")));
        assertTrue(overLimit.getMessage().contains("verification limit"));
        assertThrows(CryptoException.class,
                () -> hasher.verify("password", valid.replace("t=1", "t=4")));
        assertThrows(CryptoException.class,
                () -> hasher.verify("password", valid.replace("p=1", "p=3")));
        assertThrows(CryptoException.class,
                () -> hasher.verify("password", valid.replace("m=1024", "m=999999999999999")));
    }

    @Test
    void base64LengthsAreRejectedBeforeLargeDecodeOrArgonAllocation() {
        PasswordHasher hasher = PasswordHasher.withPolicy(FAST_POLICY);
        String saltTooLarge = phc(1_024, 1, 1, new byte[33], new byte[16]);
        String hashTooLarge = phc(1_024, 1, 1, new byte[8], new byte[33]);
        String tooLong = "$argon2id$v=19$m=1024,t=1,p=1$" + "A".repeat(1_100) + "$AAAAAA";

        assertThrows(CryptoException.class, () -> hasher.verify("x", saltTooLarge));
        assertThrows(CryptoException.class, () -> hasher.verify("x", hashTooLarge));
        assertThrows(CryptoException.class, () -> hasher.verify("x", tooLong));
        assertThrows(CryptoException.class,
                () -> hasher.verify("x", phc(1_024, 1, 1, new byte[7], new byte[16])));
        assertThrows(CryptoException.class,
                () -> hasher.verify("x", phc(1_024, 1, 1, new byte[8], new byte[3])));
    }

    @Test
    void policyRejectsGenerationThatCannotBeVerifiedAndInvalidArgonParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHashPolicy.Generation(7, 1, 1, 8, 16));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHashPolicy.Generation(1_024, 1, 1, 7, 16));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHashPolicy.Generation(1_024, 1, 1, 8, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHashPolicy(
                        new PasswordHashPolicy.Generation(2_048, 1, 1, 8, 16),
                        new PasswordHashPolicy.VerificationLimits(1_024, 2, 2, 32, 32)));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHashPolicy.VerificationLimits(1, 1, 1, 7, 32));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHashPolicy.VerificationLimits(1, 1, 1, 8, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordHashPolicy(
                        new PasswordHashPolicy.Generation(1_024, 1, 1, 8, 32),
                        new PasswordHashPolicy.VerificationLimits(2_048, 3, 2, 32, 32, 64)));
    }

    @Test
    void oneHasherCanBeReusedConcurrently() throws Exception {
        PasswordHasher hasher = PasswordHasher.withPolicy(FAST_POLICY);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
            for (int index = 0; index < 12; index++) {
                int value = index;
                results.add(executor.submit(() -> {
                    String password = "concurrent-" + value;
                    String encoded = hasher.hash(password);
                    return hasher.verify(password, encoded);
                }));
            }
            for (java.util.concurrent.Future<Boolean> result : results) {
                assertTrue(result.get());
            }
        }
    }

    private static String phc(
            int memoryKiB,
            int iterations,
            int parallelism,
            byte[] salt,
            byte[] hash) {
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return "$argon2id$v=19$m=" + memoryKiB
                + ",t=" + iterations
                + ",p=" + parallelism
                + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(hash);
    }
}
