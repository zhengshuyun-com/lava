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

import com.zhengshuyun.lava.http.HttpHeaders;
import com.zhengshuyun.lava.crypto.AesGcmUtils;
import com.zhengshuyun.lava.crypto.CryptoException;
import com.zhengshuyun.lava.crypto.RsaSignatureUtils;
import com.zhengshuyun.lava.pay.wechat.WechatPayProtocolException;
import com.zhengshuyun.lava.pay.wechat.WechatPaySecurityException;
import com.zhengshuyun.lava.pay.wechat.WechatPaySecurityFailure;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 微信支付 APIv3 请求签名、应答验签和回调解密工具。
 */
public final class WechatPayCryptoUtils {
    /** 微信支付 APIv3 RSA-SHA256 鉴权类型。 */
    public static final String AUTHORIZATION_TYPE = "WECHATPAY2-SHA256-RSA2048";
    /** 微信支付通知资源的 AES-GCM 算法标识。 */
    public static final String ENCRYPTION_ALGORITHM = "AEAD_AES_256_GCM";
    /** 微信支付公钥 ID 或平台证书序列号请求头。 */
    public static final String HEADER_SERIAL = "Wechatpay-Serial";
    /** 微信支付消息签名请求头。 */
    public static final String HEADER_SIGNATURE = "Wechatpay-Signature";
    /** 微信支付消息签名时间戳请求头。 */
    public static final String HEADER_TIMESTAMP = "Wechatpay-Timestamp";
    /** 微信支付消息签名随机串请求头。 */
    public static final String HEADER_NONCE = "Wechatpay-Nonce";
    /** 微信支付消息签名类型请求头。 */
    public static final String HEADER_SIGNATURE_TYPE = "Wechatpay-Signature-Type";
    /** 微信支付服务端请求标识响应头。 */
    public static final String HEADER_REQUEST_ID = "Request-ID";
    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 5 * 60L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WechatPayCryptoUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 生成请求使用的 32 位十六进制随机串。
     *
     * @return 随机串
     */
    public static String randomNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        try {
            return HexFormat.of().formatHex(bytes);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * 根据最终请求目标和原始正文生成 Authorization 请求头。
     *
     * @param mchid 商户号
     * @param merchantSerialNo 商户 API 证书序列号
     * @param privateKey 商户私钥
     * @param method HTTP 方法
     * @param uri 最终请求 URI
     * @param body 原始请求正文；无正文时传空字节数组
     * @param timestamp Unix 秒时间戳
     * @param nonce 请求随机串
     * @return Authorization 请求头值
     */
    public static String authorization(String mchid, String merchantSerialNo,
                                       PrivateKey privateKey, String method, URI uri,
                                       byte[] body, long timestamp, String nonce) {
        byte[] message = requestMessage(method, requestTarget(uri), timestamp, nonce, body);
        try {
            String signature = Base64.getEncoder().encodeToString(sign(privateKey, message));
            return AUTHORIZATION_TYPE
                    + " mchid=\"" + mchid + "\""
                    + ",nonce_str=\"" + nonce + "\""
                    + ",signature=\"" + signature + "\""
                    + ",timestamp=\"" + timestamp + "\""
                    + ",serial_no=\"" + merchantSerialNo + "\"";
        } finally {
            Arrays.fill(message, (byte) 0);
        }
    }

    /**
     * 验证微信支付 API 应答或通知的公钥签名及五分钟时间偏差。
     *
     * @param headers 微信支付签名请求头
     * @param body 未修改的原始正文
     * @param expectedPublicKeyId 当前商户配置的微信支付公钥 ID
     * @param publicKey 微信支付公钥
     * @param clock 当前时钟
     * @throws WechatPaySecurityException 请求头缺失、时间过期或签名无效
     */
    public static void verifyMessage(HttpHeaders headers, byte[] body,
                                     String expectedPublicKeyId, PublicKey publicKey,
                                     Clock clock) {
        // 1. 先完整提取签名元数据，任何缺失都不能降级为未验签处理。
        String serial = requiredHeader(headers, HEADER_SERIAL);
        String signature = requiredHeader(headers, HEADER_SIGNATURE);
        String timestampText = requiredHeader(headers, HEADER_TIMESTAMP);
        String nonce = requiredHeader(headers, HEADER_NONCE);
        if (!expectedPublicKeyId.equals(serial)) {
            throw new WechatPaySecurityException(
                    WechatPaySecurityFailure.UNEXPECTED_PUBLIC_KEY_ID);
        }
        String signatureType = headers.get(HEADER_SIGNATURE_TYPE);
        if (signatureType != null && !AUTHORIZATION_TYPE.equals(signatureType)) {
            throw new WechatPaySecurityException(
                    WechatPaySecurityFailure.UNSUPPORTED_SIGNATURE_TYPE);
        }

        // 2. 在执行昂贵的 RSA 验签前拒绝过期或来自未来的消息，降低重放风险。
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampText);
        } catch (NumberFormatException exception) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.INVALID_TIMESTAMP);
        }
        long now = clock.instant().getEpochSecond();
        if (timestamp < now - MAX_TIMESTAMP_SKEW_SECONDS
                || timestamp > now + MAX_TIMESTAMP_SKEW_SECONDS) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.EXPIRED_TIMESTAMP);
        }

        // 3. 使用原始正文构造三行验签串；验签失败的探测流量按普通失败处理。
        byte[] message = responseMessage(timestampText, nonce, body);
        try {
            byte[] decodedSignature;
            try {
                decodedSignature = Base64.getDecoder().decode(signature);
            } catch (IllegalArgumentException exception) {
                throw new WechatPaySecurityException(
                        WechatPaySecurityFailure.INVALID_SIGNATURE);
            }
            try {
                if (!RsaSignatureUtils.verifySha256(publicKey, message, decodedSignature)) {
                    throw new WechatPaySecurityException(
                            WechatPaySecurityFailure.INVALID_SIGNATURE);
                }
            } finally {
                Arrays.fill(decodedSignature, (byte) 0);
            }
        } catch (CryptoException exception) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.INVALID_SIGNATURE);
        } finally {
            Arrays.fill(message, (byte) 0);
        }
    }

    /**
     * 解密微信支付回调中的 AEAD_AES_256_GCM 资源。
     *
     * @param apiV3Key 32 字节 APIv3 密钥
     * @param algorithm 回调声明的算法
     * @param nonce GCM 随机串
     * @param associatedData 可选附加数据
     * @param ciphertext Base64 密文和认证标签
     * @return UTF-8 明文 JSON
     */
    public static byte[] decrypt(byte[] apiV3Key, String algorithm, String nonce,
                                 @Nullable String associatedData, String ciphertext) {
        if (!ENCRYPTION_ALGORITHM.equals(algorithm)) {
            throw new WechatPaySecurityException(
                    WechatPaySecurityFailure.UNSUPPORTED_ENCRYPTION_ALGORITHM);
        }
        if (nonce == null || nonce.isBlank() || ciphertext == null
                || ciphertext.isBlank()) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.DECRYPTION_FAILED);
        }
        byte[] encrypted;
        try {
            encrypted = Base64.getDecoder().decode(ciphertext);
        } catch (IllegalArgumentException exception) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.DECRYPTION_FAILED);
        }
        try {
            return AesGcmUtils.decrypt(apiV3Key,
                    nonce.getBytes(StandardCharsets.UTF_8),
                    (associatedData == null ? "" : associatedData)
                            .getBytes(StandardCharsets.UTF_8),
                    encrypted);
        } catch (CryptoException | IllegalArgumentException exception) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.DECRYPTION_FAILED);
        } finally {
            Arrays.fill(encrypted, (byte) 0);
        }
    }

    /**
     * 返回最终 URI 参与签名的路径和查询串。
     *
     * @param uri 最终请求 URI
     * @return path 与 query 的原始编码文本
     */
    public static String requestTarget(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        return uri.getRawQuery() == null ? path : path + '?' + uri.getRawQuery();
    }

    private static byte[] sign(PrivateKey privateKey, byte[] message) {
        try {
            return RsaSignatureUtils.sha256(privateKey, message);
        } catch (CryptoException exception) {
            throw new WechatPayProtocolException("无法生成微信支付请求签名");
        }
    }

    private static byte[] requestMessage(String method, String target, long timestamp,
                                         String nonce, byte[] body) {
        return lines(method, target, Long.toString(timestamp), nonce, body);
    }

    private static byte[] responseMessage(String timestamp, String nonce, byte[] body) {
        return lines(timestamp, nonce, body);
    }

    private static byte[] lines(String first, String second, String third, String fourth,
                                byte[] body) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(body.length + 128);
        writeLine(output, first);
        writeLine(output, second);
        writeLine(output, third);
        writeLine(output, fourth);
        output.writeBytes(body);
        output.write('\n');
        return output.toByteArray();
    }

    private static byte[] lines(String first, String second, byte[] body) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(body.length + 64);
        writeLine(output, first);
        writeLine(output, second);
        output.writeBytes(body);
        output.write('\n');
        return output.toByteArray();
    }

    private static void writeLine(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.UTF_8));
        output.write('\n');
    }

    private static String requiredHeader(HttpHeaders headers, String name) {
        String value = headers.get(name);
        if (value == null || value.isBlank()) {
            throw new WechatPaySecurityException(
                    WechatPaySecurityFailure.MISSING_SIGNATURE_HEADER);
        }
        return value;
    }
}
