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

package com.zhengshuyun.lava.crypto;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * 面向 EC 密钥的严格 PKCS#8 私钥和 X.509 公钥 PEM 支持。
 */
public final class PemKeyUtils {

    public static final int DEFAULT_MAX_PEM_CHARACTERS = 65_536;
    public static final int DEFAULT_MAX_DER_BYTES = 16_384;

    private static final String PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_FOOTER = "-----END PUBLIC KEY-----";
    private static final int LINE_WIDTH = 64;

    private PemKeyUtils() {
    }

    /**
     * 将 EC 私钥导出为 PKCS#8 PEM，或将 EC 公钥导出为 X.509 PEM。
     *
     * @param key 待导出的 EC 密钥
     * @return 含头尾边界和换行的 PEM 文本
     * @throws CryptoException 密钥类型、格式或编码不受支持时抛出
     */
    public static String toPem(Key key) {
        ValidationUtils.requireNonNull(key, "key must not be null");
        boolean privateKey = key instanceof ECPrivateKey;
        boolean publicKey = key instanceof ECPublicKey;
        if (!privateKey && !publicKey) {
            throw new CryptoException("Only EC private and public keys are supported");
        }
        if (!"EC".equalsIgnoreCase(key.getAlgorithm())) {
            throw new CryptoException("Expected an EC key algorithm");
        }
        String expectedFormat = privateKey ? "PKCS#8" : "X.509";
        String format;
        try {
            format = key.getFormat();
        } catch (RuntimeException exception) {
            throw new CryptoException("The EC key format is not exportable", exception);
        }
        if (format == null || format.isBlank()) {
            throw new CryptoException("The EC key has no exportable format");
        }
        if (!expectedFormat.equalsIgnoreCase(format)) {
            throw new CryptoException("Expected key format " + expectedFormat);
        }

        byte[] encoded = null;
        try {
            encoded = key.getEncoded();
        } catch (RuntimeException e) {
            throw new CryptoException("The EC key cannot be exported", e);
        }
        try {
            if (encoded == null || encoded.length == 0) {
                throw new CryptoException("The EC key has no exportable encoding");
            }
            if (encoded.length > DEFAULT_MAX_DER_BYTES) {
                throw new CryptoException("The encoded EC key exceeds the size limit");
            }

            String header = privateKey ? PRIVATE_HEADER : PUBLIC_HEADER;
            String footer = privateKey ? PRIVATE_FOOTER : PUBLIC_FOOTER;
            String base64 = Base64.getEncoder().encodeToString(encoded);
            StringBuilder result = new StringBuilder(header.length() + footer.length() + base64.length() + 16);
            result.append(header).append('\n');
            for (int offset = 0; offset < base64.length(); offset += LINE_WIDTH) {
                result.append(base64, offset, Math.min(offset + LINE_WIDTH, base64.length())).append('\n');
            }
            return result.append(footer).append('\n').toString();
        } finally {
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }
    }

    /**
     * 严格解析 PKCS#8 EC 私钥 PEM。
     *
     * @param pem 仅含一个私钥边界块的 PEM 文本
     * @return 解析出的 EC 私钥
     * @throws CryptoException PEM 格式、大小或密钥内容无效时抛出
     */
    public static ECPrivateKey readEcPrivateKey(String pem) {
        byte[] encoded = decodeStrict(pem, PRIVATE_HEADER, PRIVATE_FOOTER);
        try {
            Key key = KeyFactory.getInstance("EC", EcKeyUtils.SUN_EC_PROVIDER)
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
            if (!(key instanceof ECPrivateKey ecPrivateKey)
                    || !"EC".equalsIgnoreCase(key.getAlgorithm())
                    || !"PKCS#8".equalsIgnoreCase(key.getFormat())) {
                throw new CryptoException("PEM does not contain a PKCS#8 EC private key");
            }
            return ecPrivateKey;
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Invalid PKCS#8 EC private key", e);
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    /**
     * 严格解析 X.509 EC 公钥 PEM。
     *
     * @param pem 仅含一个公钥边界块的 PEM 文本
     * @return 解析出的 EC 公钥
     * @throws CryptoException PEM 格式、大小或密钥内容无效时抛出
     */
    public static ECPublicKey readEcPublicKey(String pem) {
        byte[] encoded = decodeStrict(pem, PUBLIC_HEADER, PUBLIC_FOOTER);
        try {
            Key key = KeyFactory.getInstance("EC", EcKeyUtils.SUN_EC_PROVIDER)
                    .generatePublic(new X509EncodedKeySpec(encoded));
            if (!(key instanceof ECPublicKey ecPublicKey)
                    || !"EC".equalsIgnoreCase(key.getAlgorithm())
                    || !"X.509".equalsIgnoreCase(key.getFormat())) {
                throw new CryptoException("PEM does not contain an X.509 EC public key");
            }
            return ecPublicKey;
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Invalid X.509 EC public key", e);
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }

    private static byte[] decodeStrict(String pem, String header, String footer) {
        ValidationUtils.requireNonNull(pem, "pem must not be null");
        if (pem.length() > DEFAULT_MAX_PEM_CHARACTERS) {
            throw new CryptoException("PEM input exceeds the size limit");
        }
        String value = pem.strip();
        int headerIndex = value.indexOf(header);
        int footerIndex = value.indexOf(footer);
        if (headerIndex != 0
                || footerIndex <= header.length()
                || footerIndex + footer.length() != value.length()
                || value.lastIndexOf(header) != headerIndex
                || value.lastIndexOf(footer) != footerIndex
                || containsAnyPemBoundary(value, header.length(), footerIndex)) {
            throw new CryptoException("PEM must contain exactly one matching header and footer");
        }

        String body = value.substring(header.length(), footerIndex);
        StringBuilder base64 = new StringBuilder(body.length());
        for (int index = 0; index < body.length(); index++) {
            char current = body.charAt(index);
            if (isBase64(current)) {
                base64.append(current);
            } else if (current != '\r' && current != '\n' && current != ' ' && current != '\t') {
                throw new CryptoException("PEM body contains an invalid character");
            }
        }
        if (base64.isEmpty()) {
            throw new CryptoException("PEM body must not be empty");
        }
        int maximumBase64Characters = ((DEFAULT_MAX_DER_BYTES + 2) / 3) * 4;
        if (base64.length() > maximumBase64Characters) {
            throw new CryptoException("Decoded PEM key exceeds the size limit");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64.toString());
            if (decoded.length == 0 || decoded.length > DEFAULT_MAX_DER_BYTES) {
                Arrays.fill(decoded, (byte) 0);
                throw new CryptoException("Decoded PEM key exceeds the size limit");
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            throw new CryptoException("PEM body is not valid Base64", e);
        }
    }

    private static boolean containsAnyPemBoundary(String value, int start, int end) {
        int begin = value.indexOf("-----BEGIN ", start);
        int finish = value.indexOf("-----END ", start);
        return (begin >= 0 && begin < end) || (finish >= 0 && finish < end);
    }

    private static boolean isBase64(char value) {
        return (value >= 'A' && value <= 'Z')
                || (value >= 'a' && value <= 'z')
                || (value >= '0' && value <= '9')
                || value == '+'
                || value == '/'
                || value == '=';
    }
}
