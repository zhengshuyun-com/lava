/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.notification;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityFailure;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import com.zhengshuyun.lava.pay.alipay.refund.DepositBackStatus;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 已验签退款银行卡冲退完成通知。
 *
 * @param notifyId             通知幂等 ID
 * @param sentAt               消息发送时间
 * @param appId                接收应用 ID
 * @param tradeNo              支付宝交易号
 * @param outTradeNo           商户订单号
 * @param outRequestNo         退款请求号
 * @param depositBackState     银行卡冲退状态
 * @param depositBackAmount    银行卡冲退金额，单位为分；没有时为 {@code null}
 * @param bankAckTime          银行响应时间；没有时为 {@code null}
 * @param estimatedReceiptTime 预计银行入账时间；没有时为 {@code null}
 */
public record RefundDepositBackNotification(
        String notifyId,
        Instant sentAt,
        String appId,
        String tradeNo,
        String outTradeNo,
        String outRequestNo,
        String depositBackState,
        @Nullable Long depositBackAmount,
        @Nullable LocalDateTime bankAckTime,
        @Nullable LocalDateTime estimatedReceiptTime
) {

    /**
     * 使用后端可信退款记录核对商户订单号、退款请求号和冲退金额。
     *
     * @param expectedOutTradeNo 可信商户订单号
     * @param expectedOutRequestNo 可信退款请求号
     * @param expectedAmount 可信银行卡冲退金额，单位为分
     * @return 当前通知
     * @throws AlipaySecurityException 任一关键字段不匹配
     */
    public RefundDepositBackNotification requireRefund(
            String expectedOutTradeNo,
            String expectedOutRequestNo,
            long expectedAmount
    ) {
        expectedOutTradeNo = AlipayValidationUtils.requireOutTradeNo(
                expectedOutTradeNo
        );
        expectedOutRequestNo = AlipayValidationUtils.requireOutRequestNo(
                expectedOutRequestNo
        );
        ValidationUtils.requireTrue(
                expectedAmount > 0,
                "expectedAmount must be positive"
        );
        if (!expectedOutTradeNo.equals(outTradeNo)
                || !expectedOutRequestNo.equals(outRequestNo)
                || !Long.valueOf(expectedAmount).equals(depositBackAmount)) {
            throw new AlipaySecurityException(
                    AlipaySecurityFailure.RESPONSE_MISMATCH
            );
        }
        return this;
    }

    /**
     * 判断银行卡冲退是否已明确成功。
     *
     * @return 银行卡冲退是否明确成功
     */
    public boolean bankDepositSucceeded() {
        return DepositBackStatus.SUCCESS.equals(depositBackState);
    }
}
