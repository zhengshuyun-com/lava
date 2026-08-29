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

package com.zhengshuyun.lava.pay.alipay;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.pay.alipay.bill.BillClient;
import com.zhengshuyun.lava.pay.alipay.internal.*;
import com.zhengshuyun.lava.pay.alipay.notification.NotificationParser;
import com.zhengshuyun.lava.pay.alipay.pagepay.PagePayClient;
import com.zhengshuyun.lava.pay.alipay.refund.RefundClient;
import com.zhengshuyun.lava.pay.alipay.transaction.TransactionClient;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;

/**
 * 线程安全的支付宝 OpenAPI 普通商户公钥模式根客户端。
 *
 * <p>根客户端固定绑定应用 ID、卖家 ID、应用私钥和支付宝公钥，并共享 HTTP 连接资源。
 * 页面支付由轻量上下文绑定通知地址；查单、退款、账单和通知解析直接复用根客户端协议能力。</p>
 */
public final class AlipayPayClient implements AutoCloseable {
    /** 支付宝生产网关。 */
    public static final URI DEFAULT_GATEWAY_URL =
            URI.create("https://openapi.alipay.com/gateway.do");
    /** 支付宝沙箱网关。 */
    public static final URI SANDBOX_GATEWAY_URL =
            URI.create("https://openapi-sandbox.dl.alipaydev.com/gateway.do");

    private final String appId;
    private final String sellerId;
    private final AlipayPayRuntime runtime;
    private final TransactionClient transactionClient;
    private final RefundClient refundClient;
    private final BillClient billClient;
    private final NotificationParser notificationParser;

    private AlipayPayClient(String appId, String sellerId,
                            PublicKey alipayPublicKey,
                            AlipayPayRuntime runtime) {
        this.appId = appId;
        this.sellerId = sellerId;
        this.runtime = runtime;
        transactionClient = new TransactionClient(runtime);
        refundClient = new RefundClient(runtime);
        billClient = new BillClient(runtime);
        notificationParser = new NotificationParser(
                runtime, appId, sellerId, alipayPublicKey);
    }

    /**
     * 创建一次性 fluent 构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取当前客户端绑定的支付宝应用 ID。
     *
     * @return 当前应用 ID
     */
    public String appId() {
        return appId;
    }

    /**
     * 获取当前客户端绑定的卖家支付宝用户 ID。
     *
     * @return 当前卖家支付宝用户 ID
     */
    public String sellerId() {
        return sellerId;
    }

    /**
     * 创建绑定异步通知与同步返回地址的页面支付入口。
     *
     * @param notifyUrl 异步支付通知地址
     * @param returnUrl 支付完成同步返回地址
     * @return 可复用页面支付入口
     */
    public PagePayClient pagePay(URI notifyUrl, URI returnUrl) {
        runtime.ensureOpen();
        return new PagePayClient(runtime, notifyUrl, returnUrl);
    }

    /**
     * 使用字符串地址创建页面支付入口。
     *
     * @param notifyUrl 异步支付通知地址
     * @param returnUrl 支付完成同步返回地址
     * @return 可复用页面支付入口
     */
    public PagePayClient pagePay(String notifyUrl, String returnUrl) {
        try {
            return pagePay(new URI(notifyUrl), new URI(returnUrl));
        } catch (URISyntaxException | NullPointerException exception) {
            throw new IllegalArgumentException("notifyUrl and returnUrl must be valid URIs");
        }
    }

    /**
     * 获取交易查询与关闭入口。
     *
     * @return 交易查询与关闭入口
     */
    public TransactionClient transactions() {
        runtime.ensureOpen();
        return transactionClient;
    }

    /**
     * 获取退款申请与查询入口。
     *
     * @return 退款申请与查询入口
     */
    public RefundClient refunds() {
        runtime.ensureOpen();
        return refundClient;
    }

    /**
     * 获取对账单下载地址查询入口。
     *
     * @return 对账单下载地址查询入口
     */
    public BillClient bills() {
        runtime.ensureOpen();
        return billClient;
    }

    /**
     * 获取支付与退款冲退通知解析器。
     *
     * @return 支付与退款冲退通知解析器
     */
    public NotificationParser notifications() {
        runtime.ensureOpen();
        return notificationParser;
    }

    /**
     * 关闭客户端。自建 HTTP 资源会被关闭，调用方借入的 HTTP 客户端保持可用。
     */
    @Override
    public void close() {
        runtime.close();
    }

    /**
     * 支付宝普通商户公钥模式客户端的一次性 fluent 构建器。
     *
     * <p>Java 快速沙箱配置应把原始 PKCS#8 {@code appPrivateKey} 传给
     * {@link #appPrivateKey(String)}，不得改用 PKCS#1 字段或自行转换密钥格式。</p>
     */
    public static final class Builder {
        private @Nullable String appId;
        private @Nullable String sellerId;
        private @Nullable PrivateKey appPrivateKey;
        private @Nullable PublicKey alipayPublicKey;
        private @Nullable HttpClient httpClient;
        private URI gatewayUrl = DEFAULT_GATEWAY_URL;
        private Clock clock = Clock.systemUTC();
        private boolean built;

        private Builder() {
        }

        /**
         * 配置支付宝应用 ID。
         *
         * @param value 支付宝应用 ID
         * @return 当前构建器
         */
        public Builder appId(String value) {
            ensureNotBuilt();
            appId = AlipayPayValidationUtils.requireAppId(value);
            return this;
        }

        /**
         * 配置卖家支付宝用户 ID。
         *
         * @param value 以 2088 开头的卖家支付宝用户 ID
         * @return 当前构建器
         */
        public Builder sellerId(String value) {
            ensureNotBuilt();
            sellerId = AlipayPayValidationUtils.requireSellerId(value);
            return this;
        }

        /**
         * 从原始 Base64 或 PKCS#8 PEM 文本读取应用私钥。
         *
         * @param value Java 使用的 PKCS#8 应用私钥
         * @return 当前构建器
         */
        public Builder appPrivateKey(String value) {
            ensureNotBuilt();
            appPrivateKey = AlipayPayKeyUtils.readPrivateKey(value);
            return this;
        }

        /**
         * 从文件读取 PKCS#8 应用私钥。
         *
         * @param path PKCS#8 应用私钥文件
         * @return 当前构建器
         */
        public Builder appPrivateKey(Path path) {
            ensureNotBuilt();
            appPrivateKey = AlipayPayKeyUtils.readPrivateKey(path);
            return this;
        }

        /**
         * 配置已由 HSM 或密钥服务加载的应用私钥。
         *
         * @param value HSM 或密钥服务提供的 RSA 私钥
         * @return 当前构建器
         */
        public Builder appPrivateKey(PrivateKey value) {
            ensureNotBuilt();
            appPrivateKey = AlipayPayKeyUtils.requirePrivateKey(value);
            return this;
        }

        /**
         * 从文本读取支付宝公钥。
         *
         * @param value 原始 Base64 或 X.509 PEM 支付宝公钥
         * @return 当前构建器
         */
        public Builder alipayPublicKey(String value) {
            ensureNotBuilt();
            alipayPublicKey = AlipayPayKeyUtils.readPublicKey(value);
            return this;
        }

        /**
         * 从文件读取支付宝公钥。
         *
         * @param path 支付宝公钥文件
         * @return 当前构建器
         */
        public Builder alipayPublicKey(Path path) {
            ensureNotBuilt();
            alipayPublicKey = AlipayPayKeyUtils.readPublicKey(path);
            return this;
        }

        /**
         * 配置已加载的支付宝公钥。
         *
         * @param value HSM 或配置中心提供的支付宝 RSA 公钥
         * @return 当前构建器
         */
        public Builder alipayPublicKey(PublicKey value) {
            ensureNotBuilt();
            alipayPublicKey = AlipayPayKeyUtils.requirePublicKey(value);
            return this;
        }

        /**
         * 借入调用方管理生命周期的 HTTP 客户端。
         *
         * @param value HTTP 客户端
         * @return 当前构建器
         */
        public Builder httpClient(HttpClient value) {
            ensureNotBuilt();
            httpClient = ValidationUtils.requireNonNull(value, "httpClient must not be null");
            return this;
        }

        /**
         * 配置支付宝网关地址。
         *
         * @param value 支付宝网关地址
         * @return 当前构建器
         */
        public Builder gatewayUrl(URI value) {
            ensureNotBuilt();
            gatewayUrl = AlipayPayValidationUtils.requireGatewayUrl(value);
            return this;
        }

        /**
         * 使用字符串配置支付宝网关地址。
         *
         * @param value 支付宝网关地址
         * @return 当前构建器
         */
        public Builder gatewayUrl(String value) {
            ensureNotBuilt();
            try {
                return gatewayUrl(new URI(value));
            } catch (URISyntaxException | NullPointerException exception) {
                throw new IllegalArgumentException("gatewayUrl must be a valid URI");
            }
        }

        /**
         * 构建可复用根客户端。构建器成功使用后不能再次使用。
         *
         * @return 根客户端
         */
        public AlipayPayClient build() {
            ensureNotBuilt();
            String checkedAppId = ValidationUtils.requireNonNull(appId, "appId is required");
            String checkedSellerId = ValidationUtils.requireNonNull(
                    sellerId, "sellerId is required");
            PrivateKey checkedPrivateKey = ValidationUtils.requireNonNull(
                    appPrivateKey, "appPrivateKey is required");
            PublicKey checkedPublicKey = ValidationUtils.requireNonNull(
                    alipayPublicKey, "alipayPublicKey is required");
            HttpClient effectiveHttpClient = httpClient;
            boolean ownsHttpClient = effectiveHttpClient == null;
            if (effectiveHttpClient == null) {
                effectiveHttpClient = HttpClient.builder()
                        .retryOnConnectionFailure(false)
                        .followRedirects(false)
                        .followSslRedirects(false)
                        .build();
            }

            try {
                AlipayPayTransport transport = new AlipayPayTransport(
                        checkedAppId, checkedPrivateKey, checkedPublicKey,
                        effectiveHttpClient, gatewayUrl, clock,
                        AlipayPayJsonUtils.codec());
                AlipayPayRuntime runtime = new AlipayPayRuntime(
                        transport, effectiveHttpClient, ownsHttpClient);
                built = true;
                return new AlipayPayClient(checkedAppId, checkedSellerId,
                        checkedPublicKey, runtime);
            } catch (RuntimeException exception) {
                if (ownsHttpClient) {
                    effectiveHttpClient.close();
                }
                throw exception;
            }
        }

        Builder clock(Clock value) {
            ensureNotBuilt();
            clock = ValidationUtils.requireNonNull(value, "clock must not be null");
            return this;
        }

        private void ensureNotBuilt() {
            if (built) {
                throw new IllegalStateException("AlipayPayClient.Builder cannot be reused");
            }
        }
    }
}
