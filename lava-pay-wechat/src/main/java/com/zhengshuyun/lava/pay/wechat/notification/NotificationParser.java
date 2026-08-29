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
import com.zhengshuyun.lava.pay.wechat.WechatPayProtocolException;
import com.zhengshuyun.lava.pay.wechat.WechatPaySecurityException;
import com.zhengshuyun.lava.pay.wechat.WechatPaySecurityFailure;
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
    private static final int MAX_NOTIFICATION_BYTES = 2 * 1024 * 1024;
    private static final String RESOURCE_TYPE = "encrypt-resource";
    private static final String TRANSACTION_EVENT = "TRANSACTION.SUCCESS";
    private static final Set<String> REFUND_EVENTS = Set.of(
            "REFUND.SUCCESS", "REFUND.ABNORMAL", "REFUND.CLOSED");

    private final WechatPayTransport transport;
    private final Runnable openCheck;
    private final JsonCodec jsonCodec = JsonCodec.defaultCodec();

    /**
     * 由根客户端创建通知解析器。
     *
     * @param transport 共享协议传输层
     * @param openCheck 根客户端存活检查
     */
    public NotificationParser(WechatPayTransport transport, Runnable openCheck) {
        this.transport = ValidationUtils.requireNonNull(transport, "transport");
        this.openCheck = ValidationUtils.requireNonNull(openCheck, "openCheck");
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
        openCheck.run();
        VerifiedEnvelope envelope = verifiedEnvelope(headers, body);
        requireEnvelope(envelope, TRANSACTION_EVENT, "transaction");
        byte[] plaintext = decrypt(envelope.resource());
        try {
            Transaction transaction = read(plaintext, Transaction.class,
                    "支付通知资源不是预期的 JSON 结构");
            requireMchid(transaction.mchid());
            requireSuccessfulTransaction(transaction);
            return new TransactionNotification(envelope.id(), envelope.createTime(),
                    envelope.eventType(), envelope.summary(), transaction);
        } finally {
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
        openCheck.run();
        VerifiedEnvelope envelope = verifiedEnvelope(headers, body);
        if (!REFUND_EVENTS.contains(envelope.eventType())) {
            throw new WechatPayProtocolException("退款通知 eventType 不受支持");
        }
        requireEnvelope(envelope, envelope.eventType(), "refund");
        byte[] plaintext = decrypt(envelope.resource());
        try {
            RefundNotification.Resource refund = read(plaintext,
                    RefundNotification.Resource.class,
                    "退款通知资源不是预期的 JSON 结构");
            requireMchid(refund.mchid());
            requireRefundStatus(envelope.eventType(), refund);
            return new RefundNotification(envelope.id(), envelope.createTime(),
                    envelope.eventType(), envelope.summary(), refund);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private VerifiedEnvelope verifiedEnvelope(HttpHeaders headers, byte[] body) {
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
                        requireText(resource.nonce, "resource.nonce")));
    }

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

    private byte[] decrypt(VerifiedResource resource) {
        // 3. 仅在信封类型校验通过后解密，并由 AES-GCM 认证标签验证密文完整性。
        return transport.decrypt(resource.algorithm(), resource.nonce(),
                resource.associatedData(), resource.ciphertext());
    }

    private void requireMchid(String actualMchid) {
        if (!transport.mchid().equals(actualMchid)) {
            throw new WechatPaySecurityException(WechatPaySecurityFailure.MERCHANT_MISMATCH);
        }
    }

    private static void requireSuccessfulTransaction(Transaction transaction) {
        if (!TradeState.SUCCESS.equals(transaction.tradeState())) {
            throw new WechatPayProtocolException("支付成功通知的 tradeState 必须为 SUCCESS");
        }
        requireText(transaction.transactionId(), "resource.transaction_id");
        requireText(transaction.tradeType(), "resource.trade_type");
        requireText(transaction.bankType(), "resource.bank_type");
        requireField(transaction.successTime(), "resource.success_time");
        Transaction.Payer payer = requireField(transaction.payer(), "resource.payer");
        requireText(payer.openid(), "resource.payer.openid");
        Transaction.Amount amount = requireField(transaction.amount(), "resource.amount");
        Long total = requireField(amount.total(), "resource.amount.total");
        Long payerTotal = requireField(amount.payerTotal(), "resource.amount.payer_total");
        if (total <= 0 || payerTotal < 0) {
            throw new WechatPayProtocolException("支付通知金额字段超出有效范围");
        }
        requireText(amount.currency(), "resource.amount.currency");
        requireText(amount.payerCurrency(), "resource.amount.payer_currency");
    }

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

    private static String requireText(@Nullable String value, String field) {
        if (value == null || value.isBlank()) {
            throw new WechatPayProtocolException("微信支付通知缺少必填字段 " + field);
        }
        return value;
    }

    private static <T> T requireField(@Nullable T value, String field) {
        if (value == null) {
            throw new WechatPayProtocolException("微信支付通知缺少必填字段 " + field);
        }
        return value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class NotificationEnvelope {
        @JsonProperty("id")
        public @Nullable String id;
        @JsonProperty("create_time")
        public @Nullable OffsetDateTime createTime;
        @JsonProperty("event_type")
        public @Nullable String eventType;
        @JsonProperty("resource_type")
        public @Nullable String resourceType;
        @JsonProperty("summary")
        public @Nullable String summary;
        @JsonProperty("resource")
        public @Nullable EncryptedResource resource;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class EncryptedResource {
        @JsonProperty("algorithm")
        public @Nullable String algorithm;
        @JsonProperty("ciphertext")
        public @Nullable String ciphertext;
        @JsonProperty("associated_data")
        public @Nullable String associatedData;
        @JsonProperty("original_type")
        public @Nullable String originalType;
        @JsonProperty("nonce")
        public @Nullable String nonce;
    }

    private record VerifiedEnvelope(String id, OffsetDateTime createTime, String eventType,
                                    String resourceType, String summary,
                                    VerifiedResource resource) {
    }

    private record VerifiedResource(String algorithm, String ciphertext,
                                    @Nullable String associatedData, String originalType,
                                    String nonce) {
    }
}
