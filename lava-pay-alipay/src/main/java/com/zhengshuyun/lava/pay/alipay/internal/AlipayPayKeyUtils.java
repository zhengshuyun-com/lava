/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.crypto.CryptoException;
import com.zhengshuyun.lava.crypto.CryptoUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * 支付宝 RSA2 原始 Base64、PEM 与 JCA 密钥解析工具。
 */
public final class AlipayPayKeyUtils {
    private static final int MAX_KEY_TEXT_BYTES = 64 * 1024;
    private static final int MIN_RSA_BITS = 2048;

    private AlipayPayKeyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 读取支付宝 Java 配置使用的 PKCS#8 应用私钥。
     *
     * @param value 原始 Base64 或 PKCS#8 PEM 文本
     * @return RSA 私钥
     */
    public static PrivateKey readPrivateKey(String value) {
        ValidationUtils.requireNotBlank(value, "appPrivateKey must not be blank");
        try {
            PrivateKey key = value.strip().startsWith("-----BEGIN PRIVATE KEY-----")
                    ? CryptoUtils.pemReadRsaPrivateKey(value)
                    : decodePrivateKey(value);
            return requirePrivateKey(key);
        } catch (CryptoException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "appPrivateKey is not a valid PKCS#8 RSA key");
        }
    }

    /**
     * 从文件读取应用私钥。
     *
     * @param path 密钥文件
     * @return RSA 私钥
     */
    public static PrivateKey readPrivateKey(Path path) {
        return readPrivateKey(readText(path, "appPrivateKey"));
    }

    /**
     * 读取支付宝 X.509 SubjectPublicKeyInfo 公钥。
     *
     * @param value 原始 Base64 或 PEM 文本
     * @return RSA 公钥
     */
    public static PublicKey readPublicKey(String value) {
        ValidationUtils.requireNotBlank(value, "alipayPublicKey must not be blank");
        try {
            PublicKey key = value.strip().startsWith("-----BEGIN PUBLIC KEY-----")
                    ? CryptoUtils.pemReadRsaPublicKey(value)
                    : decodePublicKey(value);
            return requirePublicKey(key);
        } catch (CryptoException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "alipayPublicKey is not a valid X.509 RSA key");
        }
    }

    /**
     * 从文件读取支付宝公钥。
     *
     * @param path 公钥文件
     * @return RSA 公钥
     */
    public static PublicKey readPublicKey(Path path) {
        return readPublicKey(readText(path, "alipayPublicKey"));
    }

    /**
     * 校验私钥使用 RSA 且密钥长度不低于 2048 位。
     *
     * @param value 待校验应用私钥
     * @return 已校验的 RSA2048 或更强私钥
     */
    public static PrivateKey requirePrivateKey(PrivateKey value) {
        requireRsaKey(ValidationUtils.requireNonNull(value,
                "appPrivateKey must not be null"), "appPrivateKey");
        return value;
    }

    /**
     * 校验公钥使用 RSA 且密钥长度不低于 2048 位。
     *
     * @param value 待校验支付宝公钥
     * @return 已校验的 RSA2048 或更强公钥
     */
    public static PublicKey requirePublicKey(PublicKey value) {
        requireRsaKey(ValidationUtils.requireNonNull(value,
                "alipayPublicKey must not be null"), "alipayPublicKey");
        return value;
    }

    private static PrivateKey decodePrivateKey(String value)
            throws GeneralSecurityException {
        byte[] encoded = decodeRawBase64(value, "appPrivateKey");
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    private static PublicKey decodePublicKey(String value)
            throws GeneralSecurityException {
        byte[] encoded = decodeRawBase64(value, "alipayPublicKey");
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(encoded));
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    private static byte[] decodeRawBase64(String value, String name) {
        String stripped = value.strip();
        ValidationUtils.requireTrue(stripped.codePoints().noneMatch(Character::isWhitespace),
                name + " raw Base64 must not contain whitespace");
        try {
            return Base64.getDecoder().decode(stripped);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " is not valid Base64");
        }
    }

    private static String readText(Path path, String name) {
        ValidationUtils.requireNonNull(path, name + " path must not be null");
        try {
            ValidationUtils.requireTrue(Files.isRegularFile(path),
                    name + " path must be a regular file");
            long size = Files.size(path);
            ValidationUtils.requireTrue(size > 0 && size <= MAX_KEY_TEXT_BYTES,
                    name + " file size is out of range");
            return Files.readString(path, StandardCharsets.US_ASCII);
        } catch (IOException exception) {
            throw new IllegalArgumentException("could not read " + name + " file");
        }
    }

    private static void requireRsaKey(Key value, String name) {
        ValidationUtils.requireTrue("RSA".equalsIgnoreCase(value.getAlgorithm()),
                name + " must use RSA");
        if (value instanceof RSAKey rsaKey) {
            ValidationUtils.requireTrue(rsaKey.getModulus().bitLength() >= MIN_RSA_BITS,
                    name + " must use at least RSA2048");
        }
    }
}
