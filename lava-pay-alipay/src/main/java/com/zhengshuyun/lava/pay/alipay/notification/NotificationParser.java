/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.json.JsonException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayPayProtocolException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayPaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayPaySecurityFailure;
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

    private final AlipayPayRuntime runtime;
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
    public NotificationParser(AlipayPayRuntime runtime, String appId, String sellerId,
                              PublicKey alipayPublicKey) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
        this.appId = appId;
        this.sellerId = sellerId;
        this.alipayPublicKey = alipayPublicKey;
    }

    /**
     * 验签并解析交易状态通知。
     *
     * @param params 已完成一次表单 URL 解码的参数
     * @return 已验签通知
     */
    public TradeNotification parseTrade(Map<String, String> params) {
        runtime.ensureOpen();
        Map<String, String> copy = copyParams(params);
        AlipayPayCryptoUtils.verifyNotification(copy, alipayPublicKey);
        requireSame(appId, copy.get("app_id"),
                AlipayPaySecurityFailure.APPLICATION_MISMATCH);
        requireSame(sellerId, copy.get("seller_id"),
                AlipayPaySecurityFailure.SELLER_MISMATCH);
        requireSame(TRADE_NOTIFY_TYPE, copy.get("notify_type"),
                AlipayPaySecurityFailure.NOTIFICATION_TYPE_MISMATCH);

        return new TradeNotification(
                required(copy, "notify_id"),
                AlipayPayDateTimeUtils.parseRequired(copy.get("notify_time"), "notify_time"),
                appId,
                sellerId,
                required(copy, "trade_no"),
                required(copy, "out_trade_no"),
                required(copy, "trade_status"),
                AlipayPayMoneyUtils.parse(required(copy, "total_amount"), "total_amount"),
                optionalMoney(copy.get("receipt_amount"), "receipt_amount"),
                optionalMoney(copy.get("buyer_pay_amount"), "buyer_pay_amount"),
                optionalMoney(copy.get("refund_fee"), "refund_fee"),
                copy.get("buyer_open_id"),
                copy.get("subject"),
                copy.get("body"),
                copy.get("passback_params"),
                AlipayPayDateTimeUtils.parseOptional(copy.get("gmt_payment"), "gmt_payment"),
                AlipayPayDateTimeUtils.parseOptional(copy.get("gmt_close"), "gmt_close"));
    }

    /**
     * 验签并解析退款银行卡冲退完成通知。
     *
     * @param params 已完成一次表单 URL 解码的参数
     * @return 已验签通知
     */
    public RefundDepositBackNotification parseRefundDepositBack(
            Map<String, String> params) {
        runtime.ensureOpen();
        Map<String, String> copy = copyParams(params);
        AlipayPayCryptoUtils.verifyNotification(copy, alipayPublicKey);
        requireSame(appId, copy.get("app_id"),
                AlipayPaySecurityFailure.APPLICATION_MISMATCH);
        requireSame(DEPOSIT_BACK_METHOD, copy.get("msg_method"),
                AlipayPaySecurityFailure.NOTIFICATION_TYPE_MISMATCH);

        long timestamp;
        try {
            timestamp = Long.parseLong(required(copy, "utc_timestamp"));
        } catch (NumberFormatException exception) {
            throw new AlipayPayProtocolException("退款冲退通知时间戳无效");
        }
        if (timestamp < 0) {
            throw new AlipayPayProtocolException("退款冲退通知时间戳无效");
        }
        DepositBackPayload payload;
        try {
            payload = AlipayPayJsonUtils.codec().read(
                    required(copy, "biz_content"), DepositBackPayload.class);
        } catch (JsonException exception) {
            throw new AlipayPayProtocolException("退款冲退通知 biz_content 结构无效");
        }
        String state = required(payload.state, "dback_status");
        if (!DEPOSIT_BACK_STATES.contains(state)) {
            throw new AlipayPayProtocolException("退款冲退通知状态无效");
        }
        Long amount = optionalMoney(payload.amount, "dback_amount");
        if (DepositBackStatus.SUCCESS.equals(state) && amount == null) {
            throw new AlipayPayProtocolException("冲退成功通知缺少 dback_amount");
        }
        return new RefundDepositBackNotification(
                required(copy, "notify_id"), Instant.ofEpochMilli(timestamp), appId,
                required(payload.tradeNo, "trade_no"),
                required(payload.outTradeNo, "out_trade_no"),
                required(payload.outRequestNo, "out_request_no"),
                state, amount,
                AlipayPayDateTimeUtils.parseOptional(payload.bankAckTime, "bank_ack_time"),
                AlipayPayDateTimeUtils.parseOptional(
                        payload.estimatedReceiptTime, "est_bank_receipt_time"));
    }

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

    private static String required(Map<String, String> params, String name) {
        return required(params.get(name), name);
    }

    private static String required(@Nullable String value, String name) {
        return AlipayPayValidationUtils.requireResponseText(value, name);
    }

    private static void requireSame(String expected, @Nullable String actual,
                                    AlipayPaySecurityFailure failure) {
        if (!expected.equals(actual)) {
            throw new AlipayPaySecurityException(failure);
        }
    }

    private static @Nullable Long optionalMoney(@Nullable String value, String name) {
        return value == null || value.isBlank() ? null : AlipayPayMoneyUtils.parse(value, name);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DepositBackPayload(
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("out_request_no") @Nullable String outRequestNo,
            @JsonProperty("dback_status") @Nullable String state,
            @JsonProperty("dback_amount") @Nullable String amount,
            @JsonProperty("bank_ack_time") @Nullable String bankAckTime,
            @JsonProperty("est_bank_receipt_time") @Nullable String estimatedReceiptTime) {
    }
}
