/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.pay.wechat.internal;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.crypto.CryptoException;
import com.zhengshuyun.lava.crypto.PemKeyUtils;
import com.zhengshuyun.lava.crypto.RsaSignatureUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 微信支付使用的 RSA PEM 和 X.509 商户证书解析工具。
 */
public final class WechatPayPemUtils {
    private static final int MAX_PEM_BYTES = 64 * 1024;
    private static final int MIN_RSA_BITS = 2048;

    private WechatPayPemUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 从 PKCS#8 PEM 文件读取 RSA 私钥。
     *
     * @param path 私钥文件
     * @return RSA 私钥
     */
    public static PrivateKey readPrivateKey(Path path) {
        try {
            PrivateKey key = PemKeyUtils.readRsaPrivateKey(readPem(path));
            requireRsaKey(key, "merchantPrivateKey");
            return key;
        } catch (CryptoException exception) {
            throw new IllegalArgumentException("merchantPrivateKey is not a valid PKCS#8 RSA key");
        }
    }

    /**
     * 从 X.509 SubjectPublicKeyInfo PEM 文件读取 RSA 公钥。
     *
     * @param path 公钥文件
     * @return RSA 公钥
     */
    public static PublicKey readPublicKey(Path path) {
        try {
            PublicKey key = PemKeyUtils.readRsaPublicKey(readPem(path));
            requireRsaKey(key, "wechatPayPublicKey");
            return key;
        } catch (CryptoException exception) {
            throw new IllegalArgumentException("wechatPayPublicKey is not a valid X.509 RSA key");
        }
    }

    /**
     * 读取商户 API X.509 证书。
     *
     * @param path 证书 PEM 文件
     * @return 商户证书
     */
    public static X509Certificate readCertificate(Path path) {
        byte[] pem = readPem(path).getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream input = new ByteArrayInputStream(pem)) {
            X509Certificate certificate = (X509Certificate) CertificateFactory
                    .getInstance("X.509").generateCertificate(input);
            requireRsaKey(certificate.getPublicKey(), "merchantCertificate");
            certificate.checkValidity();
            return certificate;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalArgumentException("merchantCertificate is not a valid current X.509 certificate");
        } finally {
            Arrays.fill(pem, (byte) 0);
        }
    }

    /**
     * 将商户证书序列号转换为微信支付要求的大写十六进制文本。
     *
     * @param certificate 商户证书
     * @return 证书序列号
     */
    public static String serialNo(X509Certificate certificate) {
        return ValidationUtils.requireNonNull(certificate, "certificate")
                .getSerialNumber().toString(16).toUpperCase(Locale.ROOT);
    }

    /**
     * 校验调用方传入的 JCA 私钥确实适用于微信支付 RSA2048 签名。
     *
     * @param privateKey 私钥
     * @return 原私钥
     */
    public static PrivateKey requirePrivateKey(PrivateKey privateKey) {
        requireRsaKey(ValidationUtils.requireNonNull(privateKey,
                "merchantPrivateKey must not be null"), "merchantPrivateKey");
        return privateKey;
    }

    /**
     * 校验调用方传入的 JCA 公钥确实适用于微信支付 RSA2048 验签。
     *
     * @param publicKey 公钥
     * @return 原公钥
     */
    public static PublicKey requirePublicKey(PublicKey publicKey) {
        requireRsaKey(ValidationUtils.requireNonNull(publicKey,
                "wechatPayPublicKey must not be null"), "wechatPayPublicKey");
        return publicKey;
    }

    /**
     * 校验调用方传入的商户 API 证书使用 RSA2048 或更高强度公钥。
     *
     * @param certificate 商户 API 证书
     * @return 原证书
     */
    public static X509Certificate requireMerchantCertificate(X509Certificate certificate) {
        X509Certificate checked = ValidationUtils.requireNonNull(certificate,
                "merchantCertificate must not be null");
        requireRsaKey(checked.getPublicKey(), "merchantCertificate");
        try {
            checked.checkValidity();
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("merchantCertificate must be currently valid");
        }
        return checked;
    }

    /**
     * 通过一次内存签名验证商户证书与私钥是否配对。
     *
     * @param privateKey 商户私钥
     * @param certificate 商户证书
     */
    public static void requireKeyPair(PrivateKey privateKey, X509Certificate certificate) {
        byte[] probe = HexFormat.of().parseHex("6c6176612d7061792d776563686174");
        try {
            byte[] signature = RsaSignatureUtils.sha256(privateKey, probe);
            try {
                ValidationUtils.requireTrue(RsaSignatureUtils.verifySha256(
                                certificate.getPublicKey(), probe, signature),
                        "merchantPrivateKey does not match merchantCertificate");
            } finally {
                Arrays.fill(signature, (byte) 0);
            }
        } catch (CryptoException exception) {
            throw new IllegalArgumentException("could not validate merchant key pair");
        } finally {
            Arrays.fill(probe, (byte) 0);
        }
    }

    private static String readPem(Path path) {
        ValidationUtils.requireNonNull(path, "PEM path must not be null");
        try {
            ValidationUtils.requireTrue(Files.isRegularFile(path),
                    "PEM path must be a regular file");
            long size = Files.size(path);
            ValidationUtils.requireTrue(size > 0 && size <= MAX_PEM_BYTES,
                    "PEM file size is out of range");
            return Files.readString(path, StandardCharsets.US_ASCII);
        } catch (IOException exception) {
            throw new IllegalArgumentException("could not read PEM file");
        }
    }

    private static void requireRsaKey(java.security.Key key, String name) {
        ValidationUtils.requireTrue("RSA".equalsIgnoreCase(key.getAlgorithm()),
                name + " must use RSA");
        if (key instanceof RSAKey rsaKey) {
            ValidationUtils.requireTrue(rsaKey.getModulus().bitLength() >= MIN_RSA_BITS,
                    name + " must use at least RSA2048");
        }
    }
}
