/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.json.JsonException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayProtocolException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityFailure;
import com.zhengshuyun.lava.pay.alipay.internal.*;
import com.zhengshuyun.lava.pay.alipay.refund.DepositBackStatus;
import org.jspecify.annotations.Nullable;

import java.security.PublicKey;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 与 Web 框架无关的支付宝支付和退款冲退通知解析器。
 *
 * <p>调用方必须传入 Web 框架完成一次表单 URL 解码后的参数，不能再次 URL 解码。
 * 支付宝将这两类通知定义为表单通知，仍使用 V1 参数排序验签，不使用 REST V3 响应头验签。
 * 验签通过仅证明通知来自支付宝，支付通知仍需调用
 * {@link TradeNotification#requireOrder(String, long)} 与可信订单记录匹配。</p>
 */
public final class NotificationParser {
    /** 通知处理成功后返回支付宝的固定响应。 */
    public static final String SUCCESS = "success";
    /** 通知处理失败后返回支付宝的固定响应。 */
    public static final String FAILURE = "fail";

    private static final String TRADE_NOTIFY_TYPE = "trade_status_sync";
    private static final String DEPOSIT_BACK_METHOD =
            "alipay.trade.refund.depositback.completed";
    private static final Set<String> DEPOSIT_BACK_STATES = Set.of(
            DepositBackStatus.SUCCESS, DepositBackStatus.FAILED);

    private final AlipayRuntime runtime;
    private final String appId;
    private final String sellerId;
    private final PublicKey alipayPublicKey;

    /**
     * 由根客户端创建通知解析器。
     *
     * @param runtime         共享运行时
     * @param appId           期望应用 ID
     * @param sellerId        期望卖家用户 ID
     * @param alipayPublicKey 支付宝公钥
     */
    public NotificationParser(
            AlipayRuntime runtime,
            String appId,
            String sellerId,
            PublicKey alipayPublicKey
    ) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
        this.appId = AlipayValidationUtils.requireAppId(appId);
        this.sellerId = AlipayValidationUtils.requireSellerId(sellerId);
        this.alipayPublicKey = AlipayKeyUtils.requirePublicKey(alipayPublicKey);
    }

    /**
     * 验签并解析交易状态通知。
     *
     * @param params 已完成一次表单 URL 解码的参数
     * @return 已验签通知
     */
    public TradeNotification parseTrade(Map<String, String> params) {
        // 1. 复制完成一次 URL 解码的表单参数，并在解释任何业务字段前执行 RSA2 验签。
        runtime.ensureOpen();
        Map<String, String> copy = copyParams(params);
        AlipayCryptoUtils.verifyNotification(copy, alipayPublicKey);
        // 2. 将应用、卖家和通知类型绑定到当前客户端，拒绝跨应用或错误路由的通知。
        requireSame(appId, copy.get("app_id"),
                AlipaySecurityFailure.APPLICATION_MISMATCH);
        requireSame(sellerId, copy.get("seller_id"),
                AlipaySecurityFailure.SELLER_MISMATCH);
        requireSame(TRADE_NOTIFY_TYPE, copy.get("notify_type"),
                AlipaySecurityFailure.NOTIFICATION_TYPE_MISMATCH);

        // 3. 验证关键时间和金额字段后映射为不可变通知；订单号与金额仍由业务调用 requireOrder 核对。
        return new TradeNotification(
                required(copy, "notify_id"),
                AlipayDateTimeUtils.parseRequired(copy.get("notify_time"), "notify_time"),
                appId,
                sellerId,
                required(copy, "trade_no"),
                required(copy, "out_trade_no"),
                required(copy, "trade_status"),
                AlipayMoneyUtils.parse(required(copy, "total_amount"), "total_amount"),
                optionalMoney(copy.get("receipt_amount"), "receipt_amount"),
                optionalMoney(copy.get("buyer_pay_amount"), "buyer_pay_amount"),
                optionalMoney(copy.get("refund_fee"), "refund_fee"),
                copy.get("buyer_open_id"),
                copy.get("subject"),
                copy.get("body"),
                copy.get("passback_params"),
                AlipayDateTimeUtils.parseOptional(copy.get("gmt_payment"), "gmt_payment"),
                AlipayDateTimeUtils.parseOptional(copy.get("gmt_close"), "gmt_close")
        );
    }

    /**
     * 验签并解析退款银行卡冲退完成通知。
     *
     * @param params 已完成一次表单 URL 解码的参数
     * @return 已验签通知
     */
    public RefundDepositBackNotification parseRefundDepositBack(
            Map<String, String> params) {
        // 1. 复制表单参数并先验签，再校验应用和蚂蚁消息方法名。
        runtime.ensureOpen();
        Map<String, String> copy = copyParams(params);
        AlipayCryptoUtils.verifyNotification(copy, alipayPublicKey);
        requireSame(appId, copy.get("app_id"),
                AlipaySecurityFailure.APPLICATION_MISMATCH);
        requireSame(DEPOSIT_BACK_METHOD, copy.get("msg_method"),
                AlipaySecurityFailure.NOTIFICATION_TYPE_MISMATCH);

        // 2. 严格解析消息时间戳和 biz_content，拒绝结构或业务状态不完整的通知。
        long timestamp;
        try {
            timestamp = Long.parseLong(required(copy, "utc_timestamp"));
        } catch (NumberFormatException exception) {
            throw new AlipayProtocolException("退款冲退通知时间戳无效");
        }
        if (timestamp < 0) {
            throw new AlipayProtocolException("退款冲退通知时间戳无效");
        }
        DepositBackPayload payload;
        try {
            payload = AlipayJsonUtils.codec().read(
                    required(copy, "biz_content"), DepositBackPayload.class);
        } catch (JsonException exception) {
            throw new AlipayProtocolException("退款冲退通知 biz_content 结构无效");
        }
        String state = required(payload.state, "dback_status");
        if (!DEPOSIT_BACK_STATES.contains(state)) {
            throw new AlipayProtocolException("退款冲退通知状态无效");
        }
        Long amount = optionalMoney(payload.amount, "dback_amount");
        if (DepositBackStatus.SUCCESS.equals(state) && amount == null) {
            throw new AlipayProtocolException("冲退成功通知缺少 dback_amount");
        }
        // 3. 验证冲退状态及成功金额后映射为不可变通知，供业务做幂等与可信退款记录核对。
        return new RefundDepositBackNotification(
                required(copy, "notify_id"),
                Instant.ofEpochMilli(timestamp),
                appId,
                required(payload.tradeNo, "trade_no"),
                required(payload.outTradeNo, "out_trade_no"),
                required(payload.outRequestNo, "out_request_no"),
                state,
                amount,
                AlipayDateTimeUtils.parseOptional(payload.bankAckTime, "bank_ack_time"),
                AlipayDateTimeUtils.parseOptional(
                        payload.estimatedReceiptTime, "est_bank_receipt_time")
        );
    }

    /**
     * 防御性复制并校验通知表单参数。
     *
     * @param params 原始参数
     * @return 可安全修改的参数副本
     */
    private static Map<String, String> copyParams(Map<String, String> params) {
        ValidationUtils.requireNonNull(params, "params must not be null");
        Map<String, String> copy = new LinkedHashMap<>();
        params.forEach((name, value) -> {
            ValidationUtils.requireNotBlank(name, "notification parameter name must not be blank");
            ValidationUtils.requireNonNull(value,
                    "notification parameter value must not be null");
            copy.put(name, value);
        });
        return copy;
    }

    /** 从参数表读取必填文本。 */
    private static String required(Map<String, String> params, String name) {
        return required(params.get(name), name);
    }

    /** 校验协议模型中的必填文本。 */
    private static String required(@Nullable String value, String name) {
        return AlipayValidationUtils.requireResponseText(value, name);
    }

    /** 按指定安全失败类型校验可信值一致性。 */
    private static void requireSame(String expected, @Nullable String actual,
                                    AlipaySecurityFailure failure) {
        if (!expected.equals(actual)) {
            throw new AlipaySecurityException(failure);
        }
    }

    /** 解析可选通知金额。 */
    private static @Nullable Long optionalMoney(@Nullable String value, String name) {
        return value == null || value.isBlank() ? null : AlipayMoneyUtils.parse(value, name);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DepositBackPayload(
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("out_request_no") @Nullable String outRequestNo,
            @JsonProperty("dback_status") @Nullable String state,
            @JsonProperty("dback_amount") @Nullable String amount,
            @JsonProperty("bank_ack_time") @Nullable String bankAckTime,
            @JsonProperty("est_bank_receipt_time") @Nullable String estimatedReceiptTime
    ) {
    }
}
