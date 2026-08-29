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

package com.zhengshuyun.lava.pay.wechat;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.pay.wechat.bill.BillClient;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayCryptoUtils;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayPemUtils;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayTransport;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayValidationUtils;
import com.zhengshuyun.lava.pay.wechat.notification.NotificationParser;
import com.zhengshuyun.lava.pay.wechat.refund.RefundClient;
import com.zhengshuyun.lava.pay.wechat.transaction.TransactionClient;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 线程安全的微信支付 APIv3 普通商户根客户端。
 *
 * <p>根客户端绑定一套商户凭证并共享 HTTP 连接资源。支付产品通过轻量应用上下文使用，
 * 查单、退款、账单和通知等商户级能力直接从根客户端获取。</p>
 */
public final class WechatPayClient implements AutoCloseable {
    /**
     * 微信支付 API 主域名。
     */
    public static final URI DEFAULT_API_BASE_URL = URI.create("https://api.mch.weixin.qq.com/");
    /**
     * 微信支付 API 备用域名。
     */
    public static final URI BACKUP_API_BASE_URL = URI.create("https://api2.mch.weixin.qq.com/");

    private final WechatPayTransport transport;
    private final HttpClient httpClient;
    private final boolean ownsHttpClient;
    private final TransactionClient transactionClient;
    private final RefundClient refundClient;
    private final BillClient billClient;
    private final NotificationParser notificationParser;
    private final AtomicBoolean closed = new AtomicBoolean();

    private WechatPayClient(WechatPayTransport transport, HttpClient httpClient,
                            boolean ownsHttpClient) {
        this.transport = transport;
        this.httpClient = httpClient;
        this.ownsHttpClient = ownsHttpClient;
        transactionClient = new TransactionClient(transport, this::ensureOpen);
        refundClient = new RefundClient(transport, this::ensureOpen);
        billClient = new BillClient(transport, this::ensureOpen);
        notificationParser = new NotificationParser(transport, this::ensureOpen);
    }

    /**
     * 创建一次性客户端构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建绑定 APPID 与支付通知地址的轻量应用上下文。
     *
     * @param appid 已与当前商户号绑定的应用 ID
     * @param notifyUrl 支付结果通知地址
     * @return 可复用应用上下文
     */
    public WechatPayApplication application(String appid, URI notifyUrl) {
        ensureOpen();
        return new WechatPayApplication(transport,
                WechatPayValidationUtils.requireAppid(appid),
                WechatPayValidationUtils.requireNotifyUrl(notifyUrl, 255),
                this::ensureOpen);
    }

    /**
     * 使用字符串通知地址创建应用上下文。
     *
     * @param appid 应用 ID
     * @param notifyUrl 支付通知地址
     * @return 可复用应用上下文
     */
    public WechatPayApplication application(String appid, String notifyUrl) {
        ValidationUtils.requireNotBlank(notifyUrl, "notifyUrl must not be blank");
        try {
            return application(appid, new URI(notifyUrl));
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("notifyUrl must be a valid URI");
        }
    }

    /**
     * 返回普通支付交易查单和关单入口。
     *
     * @return 交易客户端
     */
    public TransactionClient transactions() {
        ensureOpen();
        return transactionClient;
    }

    /**
     * 返回普通支付退款入口。
     *
     * @return 退款客户端
     */
    public RefundClient refunds() {
        ensureOpen();
        return refundClient;
    }

    /**
     * 返回交易账单与资金账单入口。
     *
     * @return 账单客户端
     */
    public BillClient bills() {
        ensureOpen();
        return billClient;
    }

    /**
     * 返回框架无关的支付和退款通知解析器。
     *
     * @return 通知解析器
     */
    public NotificationParser notifications() {
        ensureOpen();
        return notificationParser;
    }

    /**
     * 关闭客户端。自建 HTTP 资源会被关闭，调用方传入的 HTTP 客户端保持可用。
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            transport.clearSecret();
            if (ownsHttpClient) {
                httpClient.close();
            }
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("WechatPayClient is closed");
        }
    }

    /**
     * 微信支付普通商户客户端的一次性 fluent 构建器。
     */
    public static final class Builder {
        private static final Pattern PUBLIC_KEY_ID = Pattern.compile("PUB_KEY_ID_[0-9]+");

        private @Nullable String mchid;
        private @Nullable PrivateKey merchantPrivateKey;
        private @Nullable X509Certificate merchantCertificate;
        private @Nullable String merchantSerialNo;
        private @Nullable byte[] apiV3Key;
        private @Nullable String wechatPayPublicKeyId;
        private @Nullable PublicKey wechatPayPublicKey;
        private @Nullable HttpClient httpClient;
        private URI apiBaseUrl = DEFAULT_API_BASE_URL;
        private Clock clock = Clock.systemUTC();
        private Supplier<String> nonceSupplier = WechatPayCryptoUtils::randomNonce;
        private boolean built;

        private Builder() {
        }

        /**
         * 配置普通商户号。
         *
         * @param value 商户号
         * @return 当前构建器
         */
        public Builder mchid(String value) {
            mchid = requireHeaderValue(
                    WechatPayValidationUtils.requireMchid(value), "mchid");
            return this;
        }

        /**
         * 从 PKCS#8 PEM 文件配置商户 API 私钥。
         *
         * @param path 私钥文件
         * @return 当前构建器
         */
        public Builder merchantPrivateKey(Path path) {
            return merchantPrivateKey(WechatPayPemUtils.readPrivateKey(path));
        }

        /**
         * 配置来自密钥管理系统或 JCA Provider 的商户 API 私钥。
         *
         * @param value RSA 私钥
         * @return 当前构建器
         */
        public Builder merchantPrivateKey(PrivateKey value) {
            merchantPrivateKey = WechatPayPemUtils.requirePrivateKey(value);
            return this;
        }

        /**
         * 从 PEM 文件配置商户 API 证书，并在构建时自动提取序列号。
         *
         * @param path 证书文件
         * @return 当前构建器
         */
        public Builder merchantCertificate(Path path) {
            return merchantCertificate(WechatPayPemUtils.readCertificate(path));
        }

        /**
         * 配置商户 API X.509 证书，并在构建时自动提取序列号。
         *
         * @param value 商户证书
         * @return 当前构建器
         */
        public Builder merchantCertificate(X509Certificate value) {
            merchantCertificate = WechatPayPemUtils.requireMerchantCertificate(value);
            return this;
        }

        /**
         * 显式配置商户 API 证书序列号，适用于证书不落盘或硬件密钥场景。
         *
         * @param value 大写十六进制序列号
         * @return 当前构建器
         */
        public Builder merchantSerialNo(String value) {
            value = requireHeaderValue(value, "merchantSerialNo");
            ValidationUtils.requireTrue(value.codePoints().allMatch(
                            codePoint -> codePoint >= '0' && codePoint <= '9'
                                    || codePoint >= 'A' && codePoint <= 'F'
                                    || codePoint >= 'a' && codePoint <= 'f'),
                    "merchantSerialNo must contain hexadecimal characters only");
            merchantSerialNo = value.toUpperCase(Locale.ROOT);
            return this;
        }

        /**
         * 以 32 字节 UTF-8 文本配置 APIv3 密钥。
         *
         * @param value APIv3 密钥
         * @return 当前构建器
         */
        public Builder apiV3Key(String value) {
            ValidationUtils.requireNonNull(value, "apiV3Key must not be null");
            return apiV3Key(value.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * 以字节数组配置 APIv3 密钥。构建器会立即复制输入。
         *
         * @param value 32 字节密钥
         * @return 当前构建器
         */
        public Builder apiV3Key(byte[] value) {
            ValidationUtils.requireNonNull(value, "apiV3Key must not be null");
            ValidationUtils.requireTrue(value.length == 32,
                    "apiV3Key must contain exactly 32 bytes");
            if (apiV3Key != null) {
                Arrays.fill(apiV3Key, (byte) 0);
            }
            apiV3Key = value.clone();
            return this;
        }

        /**
         * 配置微信支付公钥 ID。
         *
         * @param value 形如 {@code PUB_KEY_ID_数字串} 的 ID
         * @return 当前构建器
         */
        public Builder wechatPayPublicKeyId(String value) {
            value = requireHeaderValue(value, "wechatPayPublicKeyId");
            ValidationUtils.requireTrue(PUBLIC_KEY_ID.matcher(value).matches(),
                    "wechatPayPublicKeyId format is invalid");
            wechatPayPublicKeyId = value;
            return this;
        }

        /**
         * 从 PEM 文件配置微信支付公钥。
         *
         * @param path 公钥文件
         * @return 当前构建器
         */
        public Builder wechatPayPublicKey(Path path) {
            return wechatPayPublicKey(WechatPayPemUtils.readPublicKey(path));
        }

        /**
         * 配置 JCA 微信支付公钥。
         *
         * @param value RSA 公钥
         * @return 当前构建器
         */
        public Builder wechatPayPublicKey(PublicKey value) {
            wechatPayPublicKey = WechatPayPemUtils.requirePublicKey(value);
            return this;
        }

        /**
         * 借用调用方管理的 HTTP 客户端。关闭微信支付客户端不会关闭该对象。
         * 调用方应关闭该客户端的连接失败重试、普通重定向和跨协议重定向。
         *
         * @param value HTTP 客户端
         * @return 当前构建器
         */
        public Builder httpClient(HttpClient value) {
            httpClient = ValidationUtils.requireNonNull(value, "httpClient must not be null");
            return this;
        }

        /**
         * 配置微信支付 API 根地址。默认使用主域名，可显式传入 {@link #BACKUP_API_BASE_URL}。
         *
         * <p>生产地址必须使用 HTTPS；为支持本地协议测试，仅允许环回主机使用 HTTP。</p>
         *
         * @param value 不含用户信息、查询参数和片段的绝对 URI
         * @return 当前构建器
         */
        public Builder apiBaseUrl(URI value) {
            apiBaseUrl = requireApiBaseUrl(value);
            return this;
        }

        /**
         * 使用字符串配置微信支付 API 根地址。
         *
         * @param value 根地址
         * @return 当前构建器
         */
        public Builder apiBaseUrl(String value) {
            ValidationUtils.requireNotBlank(value, "apiBaseUrl must not be blank");
            try {
                return apiBaseUrl(new URI(value));
            } catch (URISyntaxException exception) {
                throw new IllegalArgumentException("apiBaseUrl must be a valid URI");
            }
        }

        /**
         * 创建不可变客户端。构建器只能成功构建一次，APIv3 密钥副本随后会从构建器清除。
         *
         * @return 微信支付根客户端
         */
        public WechatPayClient build() {
            ValidationUtils.requireTrue(!built, "builder has already been used");
            String configuredMchid = ValidationUtils.requireNonNull(mchid, "mchid is required");
            PrivateKey configuredPrivateKey = ValidationUtils.requireNonNull(
                    merchantPrivateKey, "merchantPrivateKey is required");
            String configuredPublicKeyId = ValidationUtils.requireNonNull(
                    wechatPayPublicKeyId, "wechatPayPublicKeyId is required");
            PublicKey configuredPublicKey = ValidationUtils.requireNonNull(
                    wechatPayPublicKey, "wechatPayPublicKey is required");
            byte[] configuredApiV3Key = ValidationUtils.requireNonNull(
                    apiV3Key, "apiV3Key is required");

            String certificateSerial = merchantCertificate == null
                    ? null : WechatPayPemUtils.serialNo(merchantCertificate);
            if (merchantCertificate != null) {
                WechatPayPemUtils.requireKeyPair(configuredPrivateKey, merchantCertificate);
            }
            if (certificateSerial != null && merchantSerialNo != null
                    && !certificateSerial.equalsIgnoreCase(merchantSerialNo)) {
                throw new IllegalArgumentException(
                        "merchantSerialNo does not match merchantCertificate");
            }
            String configuredSerial = merchantSerialNo == null
                    ? certificateSerial : merchantSerialNo;
            ValidationUtils.requireNonNull(configuredSerial,
                    "merchantCertificate or merchantSerialNo is required");

            boolean ownsClient = httpClient == null;
            HttpClient configuredHttpClient = ownsClient
                    ? HttpClient.builder()
                    .retryOnConnectionFailure(false)
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()
                    : httpClient;
            try {
                WechatPayTransport transport = new WechatPayTransport(configuredMchid,
                        configuredSerial, configuredPrivateKey, configuredPublicKeyId,
                        configuredPublicKey, configuredApiV3Key, configuredHttpClient,
                        apiBaseUrl, clock, nonceSupplier, JsonCodec.defaultCodec());
                built = true;
                return new WechatPayClient(transport, configuredHttpClient, ownsClient);
            } catch (RuntimeException exception) {
                if (ownsClient) {
                    configuredHttpClient.close();
                }
                throw exception;
            } finally {
                Arrays.fill(configuredApiV3Key, (byte) 0);
                apiV3Key = null;
            }
        }

        Builder clock(Clock value) {
            clock = ValidationUtils.requireNonNull(value, "clock must not be null");
            return this;
        }

        Builder nonceSupplier(Supplier<String> value) {
            nonceSupplier = ValidationUtils.requireNonNull(value,
                    "nonceSupplier must not be null");
            return this;
        }

        private static String requireHeaderValue(String value, String name) {
            ValidationUtils.requireNotBlank(value, name + " must not be blank");
            ValidationUtils.requireTrue(value.codePoints().noneMatch(
                            codePoint -> codePoint <= 0x20 || codePoint == '"'
                                    || codePoint == '\\' || codePoint == 0x7F),
                    name + " contains a character invalid in an HTTP quoted value");
            return value;
        }

        private static URI requireApiBaseUrl(URI value) {
            ValidationUtils.requireNonNull(value, "apiBaseUrl must not be null");
            ValidationUtils.requireTrue(value.isAbsolute(),
                    "apiBaseUrl must be absolute");
            ValidationUtils.requireTrue(value.getHost() != null && !value.getHost().isBlank(),
                    "apiBaseUrl must contain a host");
            String scheme = value.getScheme();
            boolean secure = "https".equalsIgnoreCase(scheme);
            boolean localTest = "http".equalsIgnoreCase(scheme)
                    && isLoopbackHost(value.getHost());
            ValidationUtils.requireTrue(secure || localTest,
                    "apiBaseUrl must use HTTPS; HTTP is allowed only for a loopback host");
            ValidationUtils.requireTrue(value.getRawQuery() == null
                            && value.getRawFragment() == null
                            && value.getUserInfo() == null,
                    "apiBaseUrl must not contain user information, query, or fragment");
            ValidationUtils.requireTrue(value.getRawPath() == null
                            || value.getRawPath().isEmpty()
                            || "/".equals(value.getRawPath()),
                    "apiBaseUrl must not contain a path");
            String text = value.toString();
            return URI.create(text.endsWith("/") ? text : text + '/');
        }

        private static boolean isLoopbackHost(String host) {
            return "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host)
                    || "[::1]".equals(host);
        }
    }
}
