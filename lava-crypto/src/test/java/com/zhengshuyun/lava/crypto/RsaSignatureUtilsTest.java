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

        byte[] signature = CryptoUtils.rsaSha256Sign(pair.getPrivate(), data);

        assertTrue(CryptoUtils.rsaSha256Verify(pair.getPublic(), data, signature));
        assertFalse(CryptoUtils.rsaSha256Verify(pair.getPublic(),
                new byte[]{1, 2, 3, 5}, signature));
    }

    @Test
    void nonRsaKeysAreRejected() {
        KeyPair pair = CryptoUtils.ecGenerateKeyPair();
        assertThrows(IllegalArgumentException.class,
                () -> CryptoUtils.rsaSha256Sign(pair.getPrivate(), new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> CryptoUtils.rsaSha256Verify(pair.getPublic(), new byte[0], new byte[0]));
    }
}
