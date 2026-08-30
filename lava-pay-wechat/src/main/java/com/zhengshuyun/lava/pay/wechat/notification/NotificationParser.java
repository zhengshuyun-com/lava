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

package com.zhengshuyun.lava.pay.wechat.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.HttpHeaders;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.json.JsonException;
import com.zhengshuyun.lava.pay.wechat.exception.WechatPayProtocolException;
import com.zhengshuyun.lava.pay.wechat.exception.WechatPaySecurityException;
import com.zhengshuyun.lava.pay.wechat.exception.WechatPaySecurityFailure;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayRuntime;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayTransport;
import com.zhengshuyun.lava.pay.wechat.transaction.TradeState;
import com.zhengshuyun.lava.pay.wechat.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;

/**
 * 不依赖 Servlet、Spring 或其他 Web 框架的微信支付通知解析器。
 *
 * <p>调用方必须传入框架接收到的原始请求正文。解析成功后应尽快向微信支付返回 200 或 204，
 * 业务幂等、持久化和异步处理不由本工具包负责。</p>
 */
public final class NotificationParser {
    /** 单个通知原始正文允许的最大大小，单位为字节。 */
    private static final int MAX_NOTIFICATION_BYTES = 2 * 1024 * 1024;

    /** 本解析器支持的通知资源类型，表示业务资源需要 AES-GCM 解密。 */
    private static final String RESOURCE_TYPE = "encrypt-resource";

    /** 支付成功通知的唯一支持事件类型。 */
    private static final String TRANSACTION_EVENT = "TRANSACTION.SUCCESS";

    /** 退款通知允许的成功、异常和关闭事件类型。 */
    private static final Set<String> REFUND_EVENTS = Set.of(
            "REFUND.SUCCESS", "REFUND.ABNORMAL", "REFUND.CLOSED");

    /** 根客户端共享的验签、解密能力与关闭状态。 */
    private final WechatPayRuntime runtime;

    /** 验签后的通知信封与解密业务资源使用的 JSON 编解码器。 */
    private final JsonCodec jsonCodec = JsonCodec.defaultCodec();

    /**
     * 由根客户端创建通知解析器。
     *
     * @param runtime 共享运行时
     */
    public NotificationParser(WechatPayRuntime runtime) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
    }

    /**
     * 验签、解密并解析支付成功通知。
     *
     * @param headers 原始 HTTP 请求头
     * @param body 未修改的原始 UTF-8 请求正文
     * @return 支付成功通知
     */
    public TransactionNotification parseTransaction(HttpHeaders headers, String body) {
        ValidationUtils.requireNonNull(body, "body must not be null");
        return parseTransaction(headers, body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验签、解密并解析支付成功通知。
     *
     * @param headers 原始 HTTP 请求头
     * @param body 未修改的原始请求正文字节
     * @return 支付成功通知
     */
    public TransactionNotification parseTransaction(HttpHeaders headers, byte[] body) {
        // 1. 使用原始请求头和正文验签，再解析并校验支付通知信封类型。
        WechatPayTransport transport = runtime.transport();
        VerifiedEnvelope envelope = verifiedEnvelope(transport, headers, body);
        requireEnvelope(envelope, TRANSACTION_EVENT, "transaction");

        // 2. 仅对已经验签且类型匹配的资源执行 AES-GCM 解密和业务 JSON 解析。
        byte[] plaintext = decrypt(transport, envelope.resource());
        try {
            Transaction transaction = read(
                    plaintext,
                    Transaction.class,
                    "支付通知资源不是预期的 JSON 结构"
            );

            // 3. 将商户号和成功状态绑定到当前客户端，再返回可信通知模型。
            requireMchid(transport, transaction.mchid());
            requireSuccessfulTransaction(transaction);
            return new TransactionNotification(
                    envelope.id(),
                    envelope.createTime(),
                    envelope.eventType(),
                    envelope.summary(),
                    transaction
            );
        } finally {
            // 4. 无论解析是否成功都清除解密明文，缩短敏感业务数据的内存驻留时间。
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    /**
     * 验签、解密并解析退款状态变更通知。
     *
     * @param headers 原始 HTTP 请求头
     * @param body 未修改的原始 UTF-8 请求正文
     * @return 退款通知
     */
    public RefundNotification parseRefund(HttpHeaders headers, String body) {
        ValidationUtils.requireNonNull(body, "body must not be null");
        return parseRefund(headers, body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验签、解密并解析退款状态变更通知。
     *
     * @param headers 原始 HTTP 请求头
     * @param body 未修改的原始请求正文字节
     * @return 退款通知
     */
    public RefundNotification parseRefund(HttpHeaders headers, byte[] body) {
        // 1. 使用原始请求头和正文验签，再校验退款通知事件与资源类型。
        WechatPayTransport transport = runtime.transport();
        VerifiedEnvelope envelope = verifiedEnvelope(transport, headers, body);
        if (!REFUND_EVENTS.contains(envelope.eventType())) {
            throw new WechatPayProtocolException("退款通知 eventType 不受支持");
        }
        requireEnvelope(envelope, envelope.eventType(), "refund");

        // 2. 仅对已经验签且类型匹配的资源执行 AES-GCM 解密和业务 JSON 解析。
        byte[] plaintext = decrypt(transport, envelope.resource());
        try {
            RefundNotification.Resource refund = read(plaintext,
                    RefundNotification.Resource.class,
                    "退款通知资源不是预期的 JSON 结构");
            requireMchid(transport, refund.mchid());
            requireRefundStatus(envelope.eventType(), refund);
            // 3. 将商户号和退款状态绑定到当前客户端及事件类型，再返回可信通知模型。
            return new RefundNotification(
                    envelope.id(),
                    envelope.createTime(),
                    envelope.eventType(),
                    envelope.summary(),
                    refund
            );
        } finally {
            // 4. 无论解析是否成功都清除解密明文，缩短敏感业务数据的内存驻留时间。
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    /**
     * 验证原始消息并解析出字段完整的通知信封。
     *
     * @param transport 当前根客户端的验签传输层
     * @param headers 未修改的通知请求头
     * @param body 未修改的通知正文字节，大小必须在 1 字节至 2 MiB 之间
     * @return 已验签且必填字段完整的通知信封
     */
    private VerifiedEnvelope verifiedEnvelope(WechatPayTransport transport,
                                              HttpHeaders headers,
                                              byte[] body) {
        ValidationUtils.requireNonNull(headers, "headers must not be null");
        ValidationUtils.requireNonNull(body, "body must not be null");
        ValidationUtils.requireTrue(body.length > 0 && body.length <= MAX_NOTIFICATION_BYTES,
                "notification body size is out of range");

        // 1. 必须使用原始正文完成验签，未经验证的 JSON 不进入后续分支判断。
        transport.verify(headers, body);

        // 2. 验签通过后再解释通知信封，防止攻击者驱动解密或类型路由。
        NotificationEnvelope envelope = read(body, NotificationEnvelope.class,
                "微信支付通知信封不是预期的 JSON 结构");
        EncryptedResource resource = requireField(envelope.resource, "resource");
        return new VerifiedEnvelope(
                requireText(envelope.id, "id"),
                requireField(envelope.createTime, "create_time"),
                requireText(envelope.eventType, "event_type"),
                requireText(envelope.resourceType, "resource_type"),
                requireText(envelope.summary, "summary"),
                new VerifiedResource(
                        requireText(resource.algorithm, "resource.algorithm"),
                        requireText(resource.ciphertext, "resource.ciphertext"),
                        resource.associatedData,
                        requireText(resource.originalType, "resource.original_type"),
                        requireText(resource.nonce, "resource.nonce")
                )
        );
    }

    /**
     * 校验通知事件、资源类型和业务资源原始类型。
     *
     * @param envelope 已验签的通知信封
     * @param eventType 当前解析入口期望的事件类型
     * @param originalType 当前解析入口期望的解密资源类型
     */
    private static void requireEnvelope(VerifiedEnvelope envelope, String eventType,
                                        String originalType) {
        if (!eventType.equals(envelope.eventType())) {
            throw new WechatPayProtocolException("微信支付通知 eventType 不受支持");
        }
        if (!RESOURCE_TYPE.equals(envelope.resourceType())) {
            throw new WechatPayProtocolException("微信支付通知 resourceType 不受支持");
        }
        if (!originalType.equals(envelope.resource().originalType())) {
            throw new WechatPayProtocolException("微信支付通知 originalType 不匹配");
        }
    }

    /**
     * 解密已经完成类型校验的通知资源。
     *
     * @param transport 提供 APIv3 密钥和 AES-GCM 解密能力的传输层
     * @param resource 已校验必填字段的加密资源
     * @return 待解析的 UTF-8 JSON 明文字节，由调用方负责清零
     */
    private static byte[] decrypt(WechatPayTransport transport,
                                  VerifiedResource resource) {
        // 3. 仅在信封类型校验通过后解密，并由 AES-GCM 认证标签验证密文完整性。
        return transport.decrypt(
                resource.algorithm(),
                resource.nonce(),
                resource.associatedData(),
                resource.ciphertext()
        );
    }

    /**
     * 将解密资源中的商户号绑定到当前根客户端。
     *
     * @param transport 持有期望商户号的传输层
     * @param actualMchid 解密业务资源中的实际商户号
     */
    private static void requireMchid(WechatPayTransport transport,
                                     String actualMchid) {
        if (!transport.mchid().equals(actualMchid)) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.MERCHANT_MISMATCH);
        }
    }

    /**
     * 校验支付成功通知确实承载 {@code SUCCESS} 交易。
     *
     * @param transaction 解密并解析得到的交易模型
     */
    private static void requireSuccessfulTransaction(Transaction transaction) {
        if (!TradeState.SUCCESS.equals(transaction.tradeState())) {
            throw new WechatPayProtocolException("支付成功通知的 tradeState 必须为 SUCCESS");
        }
    }

    /**
     * 校验退款事件类型与解密资源状态一致，并要求成功事件携带成功时间。
     *
     * @param eventType 已验签通知信封中的退款事件类型
     * @param refund 解密并解析得到的退款资源
     */
    private static void requireRefundStatus(String eventType,
                                            RefundNotification.Resource refund) {
        String expectedStatus = eventType.substring("REFUND.".length());
        if (!expectedStatus.equals(refund.refundStatus())) {
            throw new WechatPayProtocolException("退款通知 eventType 与 refundStatus 不匹配");
        }
        if ("SUCCESS".equals(expectedStatus) && refund.successTime() == null) {
            throw new WechatPayProtocolException("退款成功通知缺少 successTime");
        }
    }

    /**
     * 将已验证字节严格解码为指定通知模型，统一隐藏 JSON 底层解析细节。
     *
     * @param body 已验签的信封字节或已认证的解密明文
     * @param type 目标通知模型类型
     * @param failureMessage JSON 格式不符合预期时的安全错误文本
     * @param <T> 目标模型类型
     * @return 非空的通知模型
     */
    private <T> T read(byte[] body, Class<T> type, String failureMessage) {
        try {
            T result = jsonCodec.read(body, type);
            if (result == null) {
                throw new WechatPayProtocolException(failureMessage);
            }
            return result;
        } catch (JsonException | IllegalArgumentException exception) {
            throw new WechatPayProtocolException(failureMessage);
        }
    }

    /**
     * 读取通知中的必填非空白文本字段。
     *
     * @param value JSON 映射得到的可空文本
     * @param field 用于协议报错的字段路径
     * @return 非空白的原文本
     */
    private static String requireText(@Nullable String value, String field) {
        if (value == null || value.isBlank()) {
            throw new WechatPayProtocolException("微信支付通知缺少必填字段 " + field);
        }
        return value;
    }

    /**
     * 读取通知中的必填对象字段。
     *
     * @param value JSON 映射得到的可空对象
     * @param field 用于协议报错的字段路径
     * @param <T> 字段类型
     * @return 非空的原对象
     */
    private static <T> T requireField(@Nullable T value, String field) {
        if (value == null) {
            throw new WechatPayProtocolException("微信支付通知缺少必填字段 " + field);
        }
        return value;
    }

    /**
     * 承载尚未完成字段校验的通知信封 JSON；仅在原始消息验签通过后解析。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class NotificationEnvelope {
        /** 通知唯一标识；JSON 缺失时为 {@code null}，后续校验将拒绝。 */
        @JsonProperty("id")
        public @Nullable String id;
        /** 通知创建时间；JSON 缺失或无法解析时不会进入业务处理。 */
        @JsonProperty("create_time")
        public @Nullable OffsetDateTime createTime;
        /** 通知事件类型，用于选择支付或退款解析分支。 */
        @JsonProperty("event_type")
        public @Nullable String eventType;
        /** 通知资源类型，本解析器仅接受 {@code encrypt-resource}。 */
        @JsonProperty("resource_type")
        public @Nullable String resourceType;
        /** 微信支付返回的通知摘要，缺失或空白时拒绝通知。 */
        @JsonProperty("summary")
        public @Nullable String summary;
        /** 待校验类型并解密的加密业务资源；缺失时拒绝通知。 */
        @JsonProperty("resource")
        public @Nullable EncryptedResource resource;
    }

    /** 承载通知信封内尚未验证和解密的 AES-GCM 资源字段。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class EncryptedResource {
        /** 资源加密算法，必须为 {@code AEAD_AES_256_GCM}。 */
        @JsonProperty("algorithm")
        public @Nullable String algorithm;
        /** Base64 编码的业务资源密文与 GCM 认证标签。 */
        @JsonProperty("ciphertext")
        public @Nullable String ciphertext;
        /** GCM 认证使用的可选附加数据；JSON 缺失时为 {@code null}。 */
        @JsonProperty("associated_data")
        public @Nullable String associatedData;
        /** 解密后的业务资源类型，必须与当前解析入口一致。 */
        @JsonProperty("original_type")
        public @Nullable String originalType;
        /** AES-GCM 解密使用的随机串；缺失或空白时拒绝通知。 */
        @JsonProperty("nonce")
        public @Nullable String nonce;
    }

    /**
     * 已完成原始消息验签与必填字段校验的不可变通知信封。
     *
     * @param id 通知唯一标识
     * @param createTime 通知创建时间
     * @param eventType 用于通知路由的事件类型
     * @param resourceType 资源封装类型，必须为 {@code encrypt-resource}
     * @param summary 微信支付提供的通知摘要
     * @param resource 已校验必填字段的加密资源
     */
    private record VerifiedEnvelope(
            String id,
            OffsetDateTime createTime,
            String eventType,
            String resourceType,
            String summary,
            VerifiedResource resource
    ) {
    }

    /**
     * 已完成必填字段校验、尚未解密的不可变通知资源。
     *
     * @param algorithm 加密算法，解密层仅接受 {@code AEAD_AES_256_GCM}
     * @param ciphertext Base64 编码的密文与 GCM 认证标签
     * @param associatedData GCM 认证使用的可选附加数据
     * @param originalType 解密后的业务资源类型
     * @param nonce AES-GCM 解密使用的随机串
     */
    private record VerifiedResource(
            String algorithm,
            String ciphertext,
            @Nullable String associatedData,
            String originalType,
            String nonce
    ) {
    }
}
