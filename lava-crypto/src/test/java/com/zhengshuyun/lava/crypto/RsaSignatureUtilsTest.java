/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.crypto;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

class RsaSignatureUtilsTest {

    @Test
    void sha256SignatureRoundTripsAndRejectsChangedData() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        byte[] data = {1, 2, 3, 4};

        byte[] signature = RsaSignatureUtils.sha256(pair.getPrivate(), data);

        assertTrue(RsaSignatureUtils.verifySha256(pair.getPublic(), data, signature));
        assertFalse(RsaSignatureUtils.verifySha256(pair.getPublic(),
                new byte[]{1, 2, 3, 5}, signature));
    }

    @Test
    void nonRsaKeysAreRejected() {
        KeyPair pair = EcKeyUtils.generate();
        assertThrows(IllegalArgumentException.class,
                () -> RsaSignatureUtils.sha256(pair.getPrivate(), new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> RsaSignatureUtils.verifySha256(pair.getPublic(), new byte[0], new byte[0]));
    }
}
