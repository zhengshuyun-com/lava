/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.wechat.exception.WechatPaySecurityException;
import com.zhengshuyun.lava.pay.wechat.exception.WechatPaySecurityFailure;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

/**
 * 已验签并解密的退款状态变更通知。
 *
 * @param id 通知唯一编号
 * @param createTime 通知创建时间
 * @param eventType REFUND.SUCCESS、REFUND.ABNORMAL 或 REFUND.CLOSED
 * @param summary 通知摘要
 * @param refund 退款结果资源
 */
public record RefundNotification(
        String id,
        OffsetDateTime createTime,
        String eventType,
        String summary,
        Resource refund
) {
    /**
     * 校验通知必填字段。
     */
    public RefundNotification {
        ValidationUtils.requireNotBlank(id, "id must not be blank");
        ValidationUtils.requireNonNull(createTime, "createTime must not be null");
        ValidationUtils.requireNotBlank(eventType, "eventType must not be blank");
        ValidationUtils.requireNonNull(summary, "summary must not be null");
        ValidationUtils.requireNonNull(refund, "refund must not be null");
    }

    /**
     * 使用后端可信退款记录核对订单号、退款单号和金额。
     *
     * @param expectedOutTradeNo  可信商户订单号
     * @param expectedOutRefundNo 可信商户退款单号
     * @param expectedTotal       可信原订单金额，单位为分
     * @param expectedRefund      可信退款金额，单位为分
     * @return 当前通知
     * @throws WechatPaySecurityException 任一关键字段不匹配
     */
    public RefundNotification requireRefund(
            String expectedOutTradeNo,
            String expectedOutRefundNo,
            long expectedTotal,
            long expectedRefund
    ) {
        ValidationUtils.requireNotBlank(
                expectedOutTradeNo,
                "expectedOutTradeNo must not be blank"
        );
        ValidationUtils.requireNotBlank(
                expectedOutRefundNo,
                "expectedOutRefundNo must not be blank"
        );
        WechatPayValidationUtils.requirePositive(expectedTotal, "expectedTotal");
        WechatPayValidationUtils.requirePositive(expectedRefund, "expectedRefund");
        if (!expectedOutTradeNo.equals(refund.outTradeNo)
                || !expectedOutRefundNo.equals(refund.outRefundNo)
                || expectedTotal != refund.amount.total
                || expectedRefund != refund.amount.refund) {
            throw new WechatPaySecurityException(
                    WechatPaySecurityFailure.RESPONSE_MISMATCH
            );
        }
        return this;
    }

    /**
     * 使用后端已保存的微信侧标识完整核对退款通知。
     *
     * @param expectedOutTradeNo  可信商户订单号
     * @param expectedTransactionId 可信微信支付订单号
     * @param expectedOutRefundNo 可信商户退款单号
     * @param expectedRefundId    可信微信支付退款单号
     * @param expectedTotal       可信原订单金额，单位为分
     * @param expectedRefund      可信退款金额，单位为分
     * @return 当前通知
     * @throws WechatPaySecurityException 任一关键字段不匹配
     */
    public RefundNotification requireRefund(
            String expectedOutTradeNo,
            String expectedTransactionId,
            String expectedOutRefundNo,
            String expectedRefundId,
            long expectedTotal,
            long expectedRefund
    ) {
        requireRefund(
                expectedOutTradeNo,
                expectedOutRefundNo,
                expectedTotal,
                expectedRefund
        );
        expectedTransactionId = WechatPayValidationUtils.requireId(
                expectedTransactionId,
                "expectedTransactionId",
                32
        );
        expectedRefundId = WechatPayValidationUtils.requireId(
                expectedRefundId,
                "expectedRefundId",
                32
        );
        if (!expectedTransactionId.equals(refund.transactionId)
                || !expectedRefundId.equals(refund.refundId)) {
            throw new WechatPaySecurityException(
                    WechatPaySecurityFailure.RESPONSE_MISMATCH
            );
        }
        return this;
    }

    /**
     * 解密后的退款结果。
     *
     * @param mchid 商户号
     * @param outTradeNo 商户订单号
     * @param transactionId 微信支付订单号
     * @param outRefundNo 商户退款单号
     * @param refundId 微信支付退款单号
     * @param refundStatus 退款状态原始值
     * @param successTime 退款成功时间
     * @param userReceivedAccount 退款入账账户
     * @param amount 退款金额
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resource(
            @JsonProperty("mchid") String mchid,
            @JsonProperty("out_trade_no") String outTradeNo,
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("out_refund_no") String outRefundNo,
            @JsonProperty("refund_id") String refundId,
            @JsonProperty("refund_status") String refundStatus,
            @JsonProperty("success_time") @Nullable OffsetDateTime successTime,
            @JsonProperty("user_received_account") String userReceivedAccount,
            @JsonProperty("amount") Amount amount
    ) {
        /**
         * 校验退款结果必填字段。
         */
        public Resource {
            ValidationUtils.requireNotBlank(mchid, "mchid must not be blank");
            ValidationUtils.requireNotBlank(outTradeNo, "outTradeNo must not be blank");
            ValidationUtils.requireNotBlank(transactionId, "transactionId must not be blank");
            ValidationUtils.requireNotBlank(outRefundNo, "outRefundNo must not be blank");
            ValidationUtils.requireNotBlank(refundId, "refundId must not be blank");
            ValidationUtils.requireNotBlank(refundStatus, "refundStatus must not be blank");
            ValidationUtils.requireNonNull(userReceivedAccount,
                    "userReceivedAccount must not be null");
            ValidationUtils.requireNonNull(amount, "amount must not be null");
        }
    }

    /**
     * 退款通知金额。
     *
     * @param total 原订单金额
     * @param refund 退款金额
     * @param payerTotal 用户实际支付金额
     * @param payerRefund 用户实际退款金额
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(
            @JsonProperty("total") long total,
            @JsonProperty("refund") long refund,
            @JsonProperty("payer_total") long payerTotal,
            @JsonProperty("payer_refund") long payerRefund
    ) {
        /**
         * 校验退款通知金额。
         */
        public Amount {
            WechatPayValidationUtils.requirePositive(total, "amount.total");
            WechatPayValidationUtils.requirePositive(refund, "amount.refund");
            ValidationUtils.requireTrue(refund <= total,
                    "amount.refund must not exceed amount.total");
            WechatPayValidationUtils.requireNonNegative(payerTotal,
                    "amount.payerTotal");
            WechatPayValidationUtils.requireNonNegative(payerRefund,
                    "amount.payerRefund");
        }
    }
}
