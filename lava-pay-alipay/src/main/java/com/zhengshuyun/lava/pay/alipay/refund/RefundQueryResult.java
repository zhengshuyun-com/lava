/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityFailure;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 已验签退款查询结果。
 *
 * @param tradeNo         支付宝交易号；未返回时为 {@code null}
 * @param outTradeNo      商户订单号；未返回时为 {@code null}
 * @param outRequestNo    退款请求号；未返回时为 {@code null}
 * @param totalAmount     原交易金额，单位为分；未返回时为 {@code null}
 * @param refundAmount    本次退款金额，单位为分；未返回时为 {@code null}
 * @param refundStatus    原始退款状态；未返回表示退款请求不存在或未成功
 * @param refundTime      退款成功时间；未返回时为 {@code null}
 * @param sentBackAmount  本次商户实际退回金额，单位为分；未返回时为 {@code null}
 * @param depositBackInfo 银行卡冲退信息；未返回时为 {@code null}
 * @param fundBills       本次退款资金渠道
 */
public record RefundQueryResult(
        @Nullable String tradeNo,
        @Nullable String outTradeNo,
        @Nullable String outRequestNo,
        @Nullable Long totalAmount,
        @Nullable Long refundAmount,
        @Nullable String refundStatus,
        @Nullable LocalDateTime refundTime,
        @Nullable Long sentBackAmount,
        @Nullable DepositBackInfo depositBackInfo,
        List<RefundFundBill> fundBills
) {
    /**
     * 使用后端可信退款记录核对订单号、退款请求号和金额。
     *
     * @param expectedOutTradeNo  可信商户订单号
     * @param expectedOutRequestNo 可信退款请求号
     * @param expectedTotalAmount 可信原订单金额，单位为分
     * @param expectedRefundAmount 可信退款金额，单位为分
     * @return 当前退款查询结果
     * @throws AlipaySecurityException 任一关键字段不匹配
     */
    public RefundQueryResult requireRefund(
            String expectedOutTradeNo,
            String expectedOutRequestNo,
            long expectedTotalAmount,
            long expectedRefundAmount
    ) {
        expectedOutTradeNo = AlipayValidationUtils.requireOutTradeNo(
                expectedOutTradeNo
        );
        expectedOutRequestNo = AlipayValidationUtils.requireOutRequestNo(
                expectedOutRequestNo
        );
        ValidationUtils.requireTrue(
                expectedTotalAmount > 0,
                "expectedTotalAmount must be positive"
        );
        ValidationUtils.requireTrue(
                expectedRefundAmount > 0,
                "expectedRefundAmount must be positive"
        );
        if (!expectedOutTradeNo.equals(outTradeNo)
                || !expectedOutRequestNo.equals(outRequestNo)
                || !Long.valueOf(expectedTotalAmount).equals(totalAmount)
                || !Long.valueOf(expectedRefundAmount).equals(refundAmount)) {
            throw new AlipaySecurityException(
                    AlipaySecurityFailure.RESPONSE_MISMATCH
            );
        }
        return this;
    }

    /**
     * 判断退款查询结果是否已明确成功。
     *
     * @return 仅当状态为 {@link RefundStatus#REFUND_SUCCESS} 时返回 {@code true}
     */
    public boolean succeeded() {
        return RefundStatus.REFUND_SUCCESS.equals(refundStatus);
    }
}
