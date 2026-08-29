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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * {@link CryptoUtils} 使用的严格 PEM 编解码实现。
 *
 * <p>导出入口当前只支持 EC 密钥；读取入口分别提供 EC 与 RSA 类型安全方法。解析时只接受单个、
 * 首尾完整匹配的 PEM 块，并限制 PEM 文本与 DER 编码的大小。</p>
 */
final class PemKeyUtils {

    /**
     * 单个 PEM 输入允许的最大字符数。
     */
    static final int DEFAULT_MAX_PEM_CHARACTERS = 65_536;

    /**
     * 单个密钥允许的最大 DER 编码字节数。
     */
    static final int DEFAULT_MAX_DER_BYTES = 16_384;

    private static final String PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_FOOTER = "-----END PUBLIC KEY-----";
    private static final int LINE_WIDTH = 64;
    private static final int MAX_BASE64_CHARACTERS = ((DEFAULT_MAX_DER_BYTES + 2) / 3) * 4;

    /**
     * 工具类不允许实例化。
     */
    private PemKeyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 将 EC 私钥导出为 PKCS#8 PEM，或将 EC 公钥导出为 X.509 PEM。
     *
     * @param key 待导出的 EC 密钥
     * @return 含头尾边界和换行的 PEM 文本
     * @throws IllegalArgumentException key 为 null 时抛出
     * @throws CryptoException 密钥类型、格式或编码不受支持时抛出
     */
    static String toPem(Key key) {
        // 1. 先限定支持的密钥类型和算法，避免仅凭编码格式误判密钥用途。
        ValidationUtils.requireNonNull(key, "key must not be null");

        boolean isEcPrivateKey = key instanceof ECPrivateKey;
        boolean isEcPublicKey = key instanceof ECPublicKey;
        if (!isEcPrivateKey && !isEcPublicKey) {
            throw new CryptoException("Only EC private and public keys are supported");
        }
        if (!"EC".equalsIgnoreCase(key.getAlgorithm())) {
            throw new CryptoException("Expected an EC key algorithm");
        }

        // 2. 确认 Provider 能按约定格式导出，并取得受大小限制保护的 DER 编码。
        String expectedFormat = isEcPrivateKey ? "PKCS#8" : "X.509";
        String actualFormat;
        try {
            actualFormat = key.getFormat();
        } catch (RuntimeException exception) {
            throw new CryptoException("The EC key format is not exportable", exception);
        }
        if (actualFormat == null || actualFormat.isBlank()) {
            throw new CryptoException("The EC key has no exportable format");
        }
        if (!expectedFormat.equalsIgnoreCase(actualFormat)) {
            throw new CryptoException("Expected key format " + expectedFormat);
        }

        byte[] encodedKey;
        try {
            encodedKey = key.getEncoded();
        } catch (RuntimeException exception) {
            throw new CryptoException("The EC key cannot be exported", exception);
        }
        if (encodedKey == null) {
            throw new CryptoException("The EC key has no exportable encoding");
        }

        // 3. 生成固定行宽的规范 PEM；无论成功或失败，都清零临时 DER 密钥材料。
        try {
            if (encodedKey.length == 0) {
                throw new CryptoException("The EC key has no exportable encoding");
            }
            if (encodedKey.length > DEFAULT_MAX_DER_BYTES) {
                throw new CryptoException("The encoded EC key exceeds the size limit");
            }

            String header = isEcPrivateKey ? PRIVATE_HEADER : PUBLIC_HEADER;
            String footer = isEcPrivateKey ? PRIVATE_FOOTER : PUBLIC_FOOTER;
            String base64 = Base64.getEncoder().encodeToString(encodedKey);
            StringBuilder pem = new StringBuilder(header.length() + footer.length() + base64.length() + 16);
            pem.append(header).append('\n');
            for (int offset = 0; offset < base64.length(); offset += LINE_WIDTH) {
                int lineEnd = Math.min(offset + LINE_WIDTH, base64.length());
                pem.append(base64, offset, lineEnd).append('\n');
            }
            return pem.append(footer).append('\n').toString();
        } finally {
            clearEncodedKey(encodedKey);
        }
    }

    /**
     * 严格解析 PKCS#8 EC 私钥 PEM。
     *
     * @param pem 仅含一个私钥边界块的 PEM 文本
     * @return 解析出的 EC 私钥
     * @throws IllegalArgumentException pem 为 null 时抛出
     * @throws CryptoException PEM 格式、大小或密钥内容无效时抛出
     */
    static ECPrivateKey readEcPrivateKey(String pem) {
        byte[] encodedKey = decodeStrict(pem, PRIVATE_HEADER, PRIVATE_FOOTER);
        try {
            Key key = KeyFactory.getInstance("EC", EcKeyUtils.SUN_EC_PROVIDER)
                    .generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
            if (!(key instanceof ECPrivateKey ecPrivateKey)) {
                throw new CryptoException("PEM does not contain a PKCS#8 EC private key");
            }
            requireKeyMetadata(ecPrivateKey, "EC", "PKCS#8",
                    "PEM does not contain a PKCS#8 EC private key");
            return ecPrivateKey;
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Invalid PKCS#8 EC private key", exception);
        } finally {
            clearEncodedKey(encodedKey);
        }
    }

    /**
     * 严格解析 X.509 EC 公钥 PEM。
     *
     * @param pem 仅含一个公钥边界块的 PEM 文本
     * @return 解析出的 EC 公钥
     * @throws IllegalArgumentException pem 为 null 时抛出
     * @throws CryptoException PEM 格式、大小或密钥内容无效时抛出
     */
    static ECPublicKey readEcPublicKey(String pem) {
        byte[] encodedKey = decodeStrict(pem, PUBLIC_HEADER, PUBLIC_FOOTER);
        try {
            Key key = KeyFactory.getInstance("EC", EcKeyUtils.SUN_EC_PROVIDER)
                    .generatePublic(new X509EncodedKeySpec(encodedKey));
            if (!(key instanceof ECPublicKey ecPublicKey)) {
                throw new CryptoException("PEM does not contain an X.509 EC public key");
            }
            requireKeyMetadata(ecPublicKey, "EC", "X.509",
                    "PEM does not contain an X.509 EC public key");
            return ecPublicKey;
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Invalid X.509 EC public key", exception);
        } finally {
            clearEncodedKey(encodedKey);
        }
    }

    /**
     * 严格解析 PKCS#8 RSA 私钥 PEM。
     *
     * @param pem 仅含一个私钥边界块的 PEM 文本
     * @return 解析出的 RSA 私钥
     * @throws IllegalArgumentException pem 为 null 时抛出
     * @throws CryptoException PEM 格式、大小或密钥内容无效时抛出
     */
    static RSAPrivateKey readRsaPrivateKey(String pem) {
        byte[] encodedKey = decodeStrict(pem, PRIVATE_HEADER, PRIVATE_FOOTER);
        try {
            Key key = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
            if (!(key instanceof RSAPrivateKey rsaPrivateKey)) {
                throw new CryptoException("PEM does not contain a PKCS#8 RSA private key");
            }
            requireKeyMetadata(rsaPrivateKey, "RSA", "PKCS#8",
                    "PEM does not contain a PKCS#8 RSA private key");
            return rsaPrivateKey;
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Invalid PKCS#8 RSA private key", exception);
        } finally {
            clearEncodedKey(encodedKey);
        }
    }

    /**
     * 严格解析 X.509 SubjectPublicKeyInfo RSA 公钥 PEM。
     *
     * @param pem 仅含一个公钥边界块的 PEM 文本
     * @return 解析出的 RSA 公钥
     * @throws IllegalArgumentException pem 为 null 时抛出
     * @throws CryptoException PEM 格式、大小或密钥内容无效时抛出
     */
    static RSAPublicKey readRsaPublicKey(String pem) {
        byte[] encodedKey = decodeStrict(pem, PUBLIC_HEADER, PUBLIC_FOOTER);
        try {
            Key key = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(encodedKey));
            if (!(key instanceof RSAPublicKey rsaPublicKey)) {
                throw new CryptoException("PEM does not contain an X.509 RSA public key");
            }
            requireKeyMetadata(rsaPublicKey, "RSA", "X.509",
                    "PEM does not contain an X.509 RSA public key");
            return rsaPublicKey;
        } catch (GeneralSecurityException exception) {
            throw new CryptoException("Invalid X.509 RSA public key", exception);
        } finally {
            clearEncodedKey(encodedKey);
        }
    }

    /**
     * 严格解码单个 PEM 块，并在分配 DER 数组前后分别执行大小校验。
     *
     * @param pem    PEM 文本
     * @param header 期望的 PEM 头边界
     * @param footer 期望的 PEM 尾边界
     * @return 解码后的 DER 字节；调用方负责在使用后清零
     * @throws IllegalArgumentException pem 为 null 时抛出
     * @throws CryptoException PEM 边界、正文或大小不符合要求时抛出
     */
    private static byte[] decodeStrict(String pem, String header, String footer) {
        // 1. 在复制和扫描文本前限制原始输入，避免超大 PEM 消耗过多内存与处理时间。
        ValidationUtils.requireNonNull(pem, "pem must not be null");
        if (pem.length() > DEFAULT_MAX_PEM_CHARACTERS) {
            throw new CryptoException("PEM input exceeds the size limit");
        }

        // 2. 只接受一个完整的外层边界，拒绝拼接、重复或正文中嵌套的其他 PEM 块。
        String normalizedPem = pem.strip();
        int headerIndex = normalizedPem.indexOf(header);
        int footerIndex = normalizedPem.indexOf(footer);
        boolean hasExpectedOuterBoundaries = headerIndex == 0
                && footerIndex > header.length()
                && footerIndex + footer.length() == normalizedPem.length();
        boolean hasDuplicateExpectedBoundary = normalizedPem.lastIndexOf(header) != headerIndex
                || normalizedPem.lastIndexOf(footer) != footerIndex;
        boolean hasNestedBoundary = containsNestedPemBoundary(normalizedPem, header.length(), footerIndex);
        if (!hasExpectedOuterBoundaries || hasDuplicateExpectedBoundary || hasNestedBoundary) {
            throw new CryptoException("PEM must contain exactly one matching header and footer");
        }

        // 3. 规范化 Base64 正文；只忽略 PEM 空白，提前拒绝元数据、注释及其他混入内容。
        String body = normalizedPem.substring(header.length(), footerIndex);
        StringBuilder base64Body = new StringBuilder(body.length());
        for (int index = 0; index < body.length(); index++) {
            char character = body.charAt(index);
            if (isBase64Character(character)) {
                base64Body.append(character);
            } else if (!isPemWhitespace(character)) {
                throw new CryptoException("PEM body contains an invalid character");
            }
        }
        if (base64Body.isEmpty()) {
            throw new CryptoException("PEM body must not be empty");
        }
        if (base64Body.length() > MAX_BASE64_CHARACTERS) {
            throw new CryptoException("Decoded PEM key exceeds the size limit");
        }

        // 4. 解码后再次校验真实 DER 大小，防止仅依赖 Base64 长度估算形成边界偏差。
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(base64Body.toString());
        } catch (IllegalArgumentException exception) {
            throw new CryptoException("PEM body is not valid Base64", exception);
        }
        if (decodedKey.length == 0 || decodedKey.length > DEFAULT_MAX_DER_BYTES) {
            clearEncodedKey(decodedKey);
            throw new CryptoException("Decoded PEM key exceeds the size limit");
        }
        return decodedKey;
    }

    /**
     * 校验 Provider 返回密钥的算法和编码格式，防止接受与入口声明不一致的实现。
     *
     * @param key               待校验密钥
     * @param expectedAlgorithm 期望的密钥算法
     * @param expectedFormat    期望的编码格式
     * @param failureMessage    校验失败时的错误消息
     * @throws CryptoException 算法或编码格式不匹配时抛出
     */
    private static void requireKeyMetadata(
            Key key, String expectedAlgorithm, String expectedFormat, String failureMessage) {
        boolean algorithmMatches = expectedAlgorithm.equalsIgnoreCase(key.getAlgorithm());
        boolean formatMatches = expectedFormat.equalsIgnoreCase(key.getFormat());
        if (!algorithmMatches || !formatMatches) {
            throw new CryptoException(failureMessage);
        }
    }

    /**
     * 判断 PEM 正文中是否嵌套了任意 BEGIN 或 END 边界。
     *
     * @param pem       已去除首尾空白的 PEM 文本
     * @param bodyStart 正文起始下标
     * @param bodyEnd   正文结束下标
     * @return 正文中存在嵌套边界时返回 true
     */
    private static boolean containsNestedPemBoundary(String pem, int bodyStart, int bodyEnd) {
        int nestedHeader = pem.indexOf("-----BEGIN ", bodyStart);
        int nestedFooter = pem.indexOf("-----END ", bodyStart);
        return (nestedHeader >= 0 && nestedHeader < bodyEnd)
                || (nestedFooter >= 0 && nestedFooter < bodyEnd);
    }

    /**
     * 判断字符是否属于标准 Base64 字符集。
     *
     * @param character 待判断字符
     * @return 属于标准 Base64 字符集时返回 true
     */
    private static boolean isBase64Character(char character) {
        return (character >= 'A' && character <= 'Z')
                || (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9')
                || character == '+'
                || character == '/'
                || character == '=';
    }

    /**
     * 判断字符是否为 PEM 正文允许忽略的空白字符。
     *
     * @param character 待判断字符
     * @return 为换行、空格或制表符时返回 true
     */
    private static boolean isPemWhitespace(char character) {
        return character == '\r' || character == '\n' || character == ' ' || character == '\t';
    }

    /**
     * 尽早清零临时 DER 数组，缩短密钥材料在内存中的保留时间。
     *
     * @param encodedKey 待清零的 DER 编码
     */
    private static void clearEncodedKey(byte[] encodedKey) {
        Arrays.fill(encodedKey, (byte) 0);
    }
}
