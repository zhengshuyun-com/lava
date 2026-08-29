/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmUtilsTest {

    @Test
    void roundTripAuthenticatesPlaintextAndAssociatedData() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] nonce = "0123456789ab".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "resource".getBytes(StandardCharsets.UTF_8);
        byte[] plaintext = "微信支付".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = CryptoUtils.aesGcmEncrypt(key, nonce, aad, plaintext);

        assertArrayEquals(plaintext, CryptoUtils.aesGcmDecrypt(key, nonce, aad, ciphertext));
        ciphertext[0] ^= 1;
        assertThrows(CryptoException.class,
                () -> CryptoUtils.aesGcmDecrypt(key, nonce, aad, ciphertext));
    }

    @Test
    void invalidKeyAndNonceFailBeforeJcaCall() {
        assertThrows(IllegalArgumentException.class,
                () -> CryptoUtils.aesGcmEncrypt(new byte[15], new byte[12], new byte[0], new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> CryptoUtils.aesGcmEncrypt(new byte[16], new byte[0], new byte[0], new byte[0]));
    }
}
