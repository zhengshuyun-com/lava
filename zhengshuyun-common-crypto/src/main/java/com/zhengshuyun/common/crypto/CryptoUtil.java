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

import com.zhengshuyun.common.core.lang.Validate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Key;
import java.security.KeyFactory;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密工具类
 * <p>
 * 提供密码哈希、EC 密钥对生成、PEM 编解码等加密操作的统一入口.
 *
 * @author Toint
 * @since 2026/2/7
 */
public final class CryptoUtil {

    /**
     * PEM 私钥头
     */
    private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";

    /**
     * PEM 私钥尾
     */
    private static final String PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----";

    /**
     * PEM 公钥头
     */
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";

    /**
     * PEM 公钥尾
     */
    private static final String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";

    /**
     * PEM 行宽
     */
    private static final int PEM_LINE_WIDTH = 64;

    /**
     * 默认参数的密码哈希执行器单例
     */
    private static final PasswordHasher DEFAULT_PASSWORD_HASHER = PasswordHasher.builder().build();

    /**
     * BC Provider 注册标志
     */
    private static volatile boolean bcProviderRegistered;

    private CryptoUtil() {
    }

    /**
     * 创建密码哈希执行器构建器
     * <p>
     * 适用于需要自定义参数的场景(如 memory/iterations/parallelism).
     *
     * @return PasswordHasher.Builder 实例
     */
    public static PasswordHasher.Builder passwordHasher() {
        return PasswordHasher.builder();
    }

    /**
     * 获取默认参数的密码哈希执行器单例
     * <p>
     * 默认参数为 {@code m=65536,t=3,p=1,saltLengthBytes=16,hashLengthBytes=32}.
     * 普通登录场景优先复用该单例即可; 如需特殊参数, 请使用 {@link #passwordHasher()} 自定义构建.
     *
     * @return 默认参数的 PasswordHasher 单例
     */
    public static PasswordHasher defaultPasswordHasher() {
        return DEFAULT_PASSWORD_HASHER;
    }

    /**
     * 创建 EC 密钥对生成执行器构建器
     * <p>
     * 注意: 这里的 EC 指椭圆曲线密钥体系. 生成的密钥可用于 ECDSA 等算法.
     *
     * @return EcKeyGenerator.Builder 实例
     */
    public static EcKeyGenerator.Builder ecKeyGenerator() {
        return EcKeyGenerator.builder();
    }

    /**
     * 将密钥导出为 PEM 格式字符串
     * <p>
     * 私钥使用 PKCS#8 格式, 公钥使用 X.509 格式.
     *
     * @param key 密钥 (支持 PrivateKey 和 PublicKey)
     * @return PEM 格式字符串
     */
    public static String toPem(Key key) {
        Validate.notNull(key, "key must not be null");

        byte[] encoded = key.getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);
        String wrapped = wrapLines(base64, PEM_LINE_WIDTH);

        String format = key.getFormat();
        return switch (format) {
            case "PKCS#8" -> PRIVATE_KEY_HEADER + "\n" + wrapped + "\n" + PRIVATE_KEY_FOOTER + "\n";
            case "X.509" -> PUBLIC_KEY_HEADER + "\n" + wrapped + "\n" + PUBLIC_KEY_FOOTER + "\n";
            default -> throw new CryptoException("Unsupported key format: " + format);
        };
    }

    /**
     * 从 PEM 格式字符串读取 EC 私钥
     *
     * @param pem PEM 格式字符串
     * @return EC 私钥
     * @throws CryptoException 解析失败时抛出
     */
    public static ECPrivateKey readEcPrivateKey(String pem) {
        Validate.notBlank(pem, "pem must not be blank");
        ensureBouncyCastleProvider();
        try {
            byte[] decoded = decodePem(pem, PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            return (ECPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Failed to read EC private key from PEM", e);
        }
    }

    /**
     * 从 PEM 格式字符串读取 EC 公钥
     *
     * @param pem PEM 格式字符串
     * @return EC 公钥
     * @throws CryptoException 解析失败时抛出
     */
    public static ECPublicKey readEcPublicKey(String pem) {
        Validate.notBlank(pem, "pem must not be blank");
        ensureBouncyCastleProvider();
        try {
            byte[] decoded = decodePem(pem, PUBLIC_KEY_HEADER, PUBLIC_KEY_FOOTER);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            return (ECPublicKey) keyFactory.generatePublic(keySpec);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Failed to read EC public key from PEM", e);
        }
    }

    /**
     * 确保 Bouncy Castle Provider 已注册
     */
    static void ensureBouncyCastleProvider() {
        if (!bcProviderRegistered) {
            synchronized (CryptoUtil.class) {
                if (!bcProviderRegistered) {
                    if (Security.getProvider("BC") == null) {
                        Security.addProvider(new BouncyCastleProvider());
                    }
                    bcProviderRegistered = true;
                }
            }
        }
    }

    /**
     * 解码 PEM 内容
     */
    private static byte[] decodePem(String pem, String header, String footer) {
        String trimmed = pem.strip();
        if (!trimmed.startsWith(header) || !trimmed.endsWith(footer)) {
            throw new CryptoException("Invalid PEM format: expected " + header);
        }
        String base64 = trimmed
                .replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }

    /**
     * 按固定宽度折行
     */
    private static String wrapLines(String text, int lineWidth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i += lineWidth) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(text, i, Math.min(i + lineWidth, text.length()));
        }
        return sb.toString();
    }
}
