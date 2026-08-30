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
import com.zhengshuyun.lava.http.OkHttpInterop;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.pay.alipay.bill.BillClient;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayJsonUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayKeyUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayPagePayRedirectFactory;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayRuntime;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayTransport;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
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
 * 线程安全的支付宝 OpenAPI V3 普通商户公钥模式根客户端。
 *
 * <p>根客户端固定绑定应用 ID、卖家 ID、应用私钥和支付宝公钥，并共享 HTTP 连接资源。
 * 页面支付由轻量上下文绑定通知地址；查单、退款、账单和通知解析直接复用根客户端协议能力。
 * 其中服务端 API 使用 REST V3，电脑网站页面支付按支付宝当前唯一支持的 AOP 页面跳转协议生成表单。</p>
 *
 * <p>客户端应作为长生命周期对象复用，{@link #close()} 可幂等调用。关闭后，业务入口及此前取得的
 * 子客户端均不可继续发起协议操作；应用 ID 和卖家 ID 仍可读取。默认 HTTP 客户端不会自动重试请求或跟随重定向；
 * 借入外部客户端时，调用方必须保持相同配置。</p>
 */
public final class AlipayClient implements AutoCloseable {
    /** 支付宝生产 OpenAPI 基础地址。 */
    public static final URI DEFAULT_BASE_URL = URI.create("https://openapi.alipay.com");
    /** 支付宝沙箱 OpenAPI 基础地址。 */
    public static final URI SANDBOX_BASE_URL =
            URI.create("https://openapi-sandbox.dl.alipaydev.com");

    /** 当前客户端绑定的支付宝应用 ID。 */
    private final String appId;
    /** 当前客户端绑定的卖家支付宝用户 ID。 */
    private final String sellerId;
    /** 各业务入口共享的协议传输资源与关闭状态。 */
    private final AlipayRuntime runtime;
    /** 交易查询与关闭入口。 */
    private final TransactionClient transactionClient;
    /** 退款申请与查询入口。 */
    private final RefundClient refundClient;
    /** 账单下载地址查询入口。 */
    private final BillClient billClient;
    /** 支付与退款冲退通知解析器。 */
    private final NotificationParser notificationParser;

    /**
     * 使用已经校验的商户配置和共享运行时创建根客户端。
     *
     * @param appId           支付宝应用 ID
     * @param sellerId        卖家支付宝用户 ID
     * @param alipayPublicKey 支付宝公钥
     * @param runtime         共享协议运行时
     */
    private AlipayClient(
            String appId,
            String sellerId,
            PublicKey alipayPublicKey,
            AlipayRuntime runtime
    ) {
        this.appId = appId;
        this.sellerId = sellerId;
        this.runtime = runtime;
        transactionClient = new TransactionClient(runtime);
        refundClient = new RefundClient(runtime);
        billClient = new BillClient(runtime);
        notificationParser = new NotificationParser(
                runtime,
                appId,
                sellerId,
                alipayPublicKey
        );
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
     * @throws IllegalArgumentException 地址不是符合支付宝要求的绝对 HTTP 或 HTTPS URI
     * @throws IllegalStateException    根客户端已经关闭
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
     * @throws IllegalArgumentException 地址为空、语法无效或不符合支付宝回调地址要求
     * @throws IllegalStateException    根客户端已经关闭
     */
    public PagePayClient pagePay(String notifyUrl, String returnUrl) {
        return pagePay(parseUri(notifyUrl, "notifyUrl"), parseUri(returnUrl, "returnUrl"));
    }

    /**
     * 获取交易查询与关闭入口。
     *
     * @return 交易查询与关闭入口
     * @throws IllegalStateException 根客户端已经关闭
     */
    public TransactionClient transactions() {
        runtime.ensureOpen();
        return transactionClient;
    }

    /**
     * 获取退款申请与查询入口。
     *
     * @return 退款申请与查询入口
     * @throws IllegalStateException 根客户端已经关闭
     */
    public RefundClient refunds() {
        runtime.ensureOpen();
        return refundClient;
    }

    /**
     * 获取对账单下载地址查询入口。
     *
     * @return 对账单下载地址查询入口
     * @throws IllegalStateException 根客户端已经关闭
     */
    public BillClient bills() {
        runtime.ensureOpen();
        return billClient;
    }

    /**
     * 获取支付与退款冲退通知解析器。
     *
     * @return 支付与退款冲退通知解析器
     * @throws IllegalStateException 根客户端已经关闭
     */
    public NotificationParser notifications() {
        runtime.ensureOpen();
        return notificationParser;
    }

    /**
     * 幂等关闭客户端。自建 HTTP 资源会被关闭，调用方借入的 HTTP 客户端保持可用。
     */
    @Override
    public void close() {
        runtime.close();
    }

    /**
     * 将字符串解析为 URI，并保留具体字段名和语法错误原因。
     *
     * @param value URI 文本
     * @param name  字段名
     * @return 解析后的 URI
     * @throws IllegalArgumentException 文本为空或 URI 语法无效
     */
    private static URI parseUri(String value, String name) {
        value = ValidationUtils.requireNotBlank(value, name + " must not be blank");
        try {
            return new URI(value);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(name + " must be a valid URI", exception);
        }
    }

    /**
     * 支付宝普通商户公钥模式客户端的一次性 fluent 构建器。
     *
     * <p>Java 快速沙箱配置应把原始 PKCS#8 {@code appPrivateKey} 传给
     * {@link #appPrivateKey(String)}，不得改用 PKCS#1 字段或自行转换密钥格式。
     * 每次调用 {@link #build()} 后，构建器都会释放其持有的应用私钥引用；失败后重试必须重新配置私钥。</p>
     *
     * <p>构建成功后，所有配置方法和 {@link #build()} 均会抛出 {@link IllegalStateException}。</p>
     */
    public static final class Builder {
        /** 待绑定的支付宝应用 ID。 */
        private @Nullable String appId;
        /** 待绑定的卖家支付宝用户 ID。 */
        private @Nullable String sellerId;
        /** 用于请求签名的应用私钥。 */
        private @Nullable PrivateKey appPrivateKey;
        /** 用于验签的支付宝公钥。 */
        private @Nullable PublicKey alipayPublicKey;
        /** 可选的调用方托管 HTTP 客户端。 */
        private @Nullable HttpClient httpClient;
        /** 支付宝 OpenAPI 基础地址。 */
        private URI baseUrl = DEFAULT_BASE_URL;
        /** 生成支付宝协议时间戳所使用的时钟。 */
        private Clock clock = Clock.systemUTC();
        /** 构建器是否已经成功创建根客户端。 */
        private boolean built;

        /** 创建使用生产 OpenAPI 地址和系统时钟的空构建器。 */
        private Builder() {
        }

        /**
         * 配置支付宝应用 ID。
         *
         * @param value 支付宝应用 ID
         * @return 当前构建器
         * @throws IllegalArgumentException 应用 ID 为空或格式无效
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder appId(String value) {
            ensureNotBuilt();
            appId = AlipayValidationUtils.requireAppId(value);
            return this;
        }

        /**
         * 配置卖家支付宝用户 ID。
         *
         * @param value 以 2088 开头的卖家支付宝用户 ID
         * @return 当前构建器
         * @throws IllegalArgumentException 卖家用户 ID 为空或格式无效
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder sellerId(String value) {
            ensureNotBuilt();
            sellerId = AlipayValidationUtils.requireSellerId(value);
            return this;
        }

        /**
         * 从原始 Base64 或 PKCS#8 PEM 文本读取应用私钥。
         *
         * @param value Java 使用的 PKCS#8 应用私钥
         * @return 当前构建器
         * @throws IllegalArgumentException 私钥为空、格式无效、不是 RSA 或长度不足 2048 位
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder appPrivateKey(String value) {
            ensureNotBuilt();
            appPrivateKey = AlipayKeyUtils.readPrivateKey(value);
            return this;
        }

        /**
         * 从文件读取 PKCS#8 应用私钥。
         *
         * @param path PKCS#8 应用私钥文件
         * @return 当前构建器
         * @throws IllegalArgumentException 文件不可读，或私钥格式、算法、长度无效
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder appPrivateKey(Path path) {
            ensureNotBuilt();
            appPrivateKey = AlipayKeyUtils.readPrivateKey(path);
            return this;
        }

        /**
         * 配置已由 HSM 或密钥服务加载的应用私钥。
         *
         * @param value HSM 或密钥服务提供的 RSA 私钥
         * @return 当前构建器
         * @throws IllegalArgumentException 私钥为空、不是 RSA 或长度不足 2048 位
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder appPrivateKey(PrivateKey value) {
            ensureNotBuilt();
            appPrivateKey = AlipayKeyUtils.requirePrivateKey(value);
            return this;
        }

        /**
         * 从文本读取支付宝公钥。
         *
         * @param value 原始 Base64 或 X.509 PEM 支付宝公钥
         * @return 当前构建器
         * @throws IllegalArgumentException 公钥为空、格式无效、不是 RSA 或长度不足 2048 位
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder alipayPublicKey(String value) {
            ensureNotBuilt();
            alipayPublicKey = AlipayKeyUtils.readPublicKey(value);
            return this;
        }

        /**
         * 从文件读取支付宝公钥。
         *
         * @param path 支付宝公钥文件
         * @return 当前构建器
         * @throws IllegalArgumentException 文件不可读，或公钥格式、算法、长度无效
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder alipayPublicKey(Path path) {
            ensureNotBuilt();
            alipayPublicKey = AlipayKeyUtils.readPublicKey(path);
            return this;
        }

        /**
         * 配置已加载的支付宝公钥。
         *
         * @param value HSM 或配置中心提供的支付宝 RSA 公钥
         * @return 当前构建器
         * @throws IllegalArgumentException 公钥为空、不是 RSA 或长度不足 2048 位
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder alipayPublicKey(PublicKey value) {
            ensureNotBuilt();
            alipayPublicKey = AlipayKeyUtils.requirePublicKey(value);
            return this;
        }

        /**
         * 借入调用方管理生命周期的 HTTP 客户端。关闭支付宝客户端不会关闭该对象。
         * 调用方必须关闭该客户端的连接失败重试、普通重定向和跨协议重定向，避免支付请求被隐式重放或改写目标。
         *
         * @param value HTTP 客户端
         * @return 当前构建器
         * @throws IllegalArgumentException HTTP 客户端为空
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder httpClient(HttpClient value) {
            ensureNotBuilt();
            value = ValidationUtils.requireNonNull(value, "httpClient must not be null");
            ValidationUtils.requireTrue(
                    !OkHttpInterop.unwrap(value).retryOnConnectionFailure(),
                    "httpClient must disable connection failure retries"
            );
            ValidationUtils.requireTrue(
                    !OkHttpInterop.unwrap(value).followRedirects(),
                    "httpClient must disable redirects"
            );
            ValidationUtils.requireTrue(
                    !OkHttpInterop.unwrap(value).followSslRedirects(),
                    "httpClient must disable cross-protocol redirects"
            );
            httpClient = value;
            return this;
        }

        /**
         * 配置支付宝 OpenAPI 基础地址。生产环境必须使用 HTTPS；仅本地环回协议测试允许使用 HTTP。
         * 地址不得包含用户信息、查询参数或片段。
         *
         * @param value 支付宝 OpenAPI 基础地址
         * @return 当前构建器
         * @throws IllegalArgumentException 地址为空或不符合 OpenAPI 安全约束
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder baseUrl(URI value) {
            ensureNotBuilt();
            baseUrl = AlipayValidationUtils.requireBaseUrl(value);
            return this;
        }

        /**
         * 使用字符串配置支付宝 OpenAPI 基础地址。生产环境必须使用 HTTPS；仅本地环回协议测试允许使用 HTTP。
         *
         * @param value 支付宝 OpenAPI 基础地址
         * @return 当前构建器
         * @throws IllegalArgumentException 地址为空、语法无效或不符合 OpenAPI 安全约束
         * @throws IllegalStateException    构建器已经成功使用
         */
        public Builder baseUrl(String value) {
            ensureNotBuilt();
            baseUrl = AlipayValidationUtils.requireBaseUrl(
                    parseUri(value, "baseUrl")
            );
            return this;
        }

        /**
         * 构建可复用根客户端。构建器成功使用后不能再次使用；内部创建的 HTTP 客户端由根客户端接管。
         *
         * @return 根客户端
         * @throws IllegalArgumentException 缺少必需配置
         * @throws IllegalStateException    构建器已经成功使用
         */
        public AlipayClient build() {
            ensureNotBuilt();
            try {
                // 1. 一次性读取并校验必需配置，避免半初始化客户端进入协议流程。
                String checkedAppId = ValidationUtils.requireNonNull(appId, "appId is required");
                String checkedSellerId = ValidationUtils.requireNonNull(sellerId, "sellerId is required");
                PrivateKey checkedPrivateKey = ValidationUtils.requireNonNull(appPrivateKey, "appPrivateKey is required");
                PublicKey checkedPublicKey = ValidationUtils.requireNonNull(alipayPublicKey, "alipayPublicKey is required");

                // 2. 未借用外部客户端时创建专属 HTTP 客户端，并显式禁用重试和重定向。
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
                    // 3. 将协议能力和资源所有权封装为共享运行时，再创建只暴露业务入口的根客户端。
                    JsonCodec jsonCodec = AlipayJsonUtils.codec();
                    AlipayTransport transport = new AlipayTransport(
                            checkedAppId,
                            checkedPrivateKey,
                            checkedPublicKey,
                            effectiveHttpClient,
                            baseUrl,
                            clock,
                            jsonCodec
                    );
                    AlipayPagePayRedirectFactory pagePayRedirectFactory =
                            new AlipayPagePayRedirectFactory(
                                    checkedAppId,
                                    checkedPrivateKey,
                                    baseUrl,
                                    clock,
                                    jsonCodec
                            );
                    AlipayRuntime runtime = new AlipayRuntime(
                            transport,
                            pagePayRedirectFactory,
                            effectiveHttpClient,
                            ownsHttpClient
                    );
                    AlipayClient client = new AlipayClient(
                            checkedAppId,
                            checkedSellerId,
                            checkedPublicKey,
                            runtime
                    );
                    built = true;
                    return client;
                } catch (RuntimeException exception) {
                    // 仅关闭本构建器创建的资源，调用方借出的客户端仍由调用方管理。
                    if (ownsHttpClient) {
                        effectiveHttpClient.close();
                    }
                    throw exception;
                }
            } finally {
                // 4. 每次构建尝试后释放私钥引用，缩短敏感对象被构建器持有的时间。
                appPrivateKey = null;
            }
        }

        /**
         * 配置协议测试时钟。该入口保持包可见，仅供同包测试构造稳定时间戳。
         *
         * @param value 协议时钟
         * @return 当前构建器
         * @throws IllegalArgumentException 时钟为空
         * @throws IllegalStateException    构建器已经成功使用
         */
        Builder clock(Clock value) {
            ensureNotBuilt();
            clock = ValidationUtils.requireNonNull(value, "clock must not be null");
            return this;
        }

        /**
         * 确认构建器尚未成功创建客户端。
         *
         * @throws IllegalStateException 构建器已经成功使用
         */
        private void ensureNotBuilt() {
            if (built) {
                throw new IllegalStateException("AlipayClient.Builder cannot be reused");
            }
        }
    }
}
