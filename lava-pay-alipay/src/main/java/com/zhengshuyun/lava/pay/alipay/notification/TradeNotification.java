/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.notification;

import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityFailure;
import com.zhengshuyun.lava.pay.alipay.transaction.TradeState;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 已验签且已核对应用与卖家身份的交易状态通知。
 *
 * @param notifyId        通知幂等 ID
 * @param notifyTime      通知发送时间
 * @param appId           应用 ID
 * @param sellerId        卖家支付宝用户 ID
 * @param tradeNo         支付宝交易号
 * @param outTradeNo      商户订单号
 * @param tradeState      原始交易状态
 * @param totalAmount     订单金额，单位为分
 * @param receiptAmount   商家实收金额，单位为分；没有时为 {@code null}
 * @param buyerPayAmount  买家实付金额，单位为分；没有时为 {@code null}
 * @param refundAmount    累计退款金额，单位为分；没有时为 {@code null}
 * @param buyerOpenId     买家 OpenID；没有时为 {@code null}
 * @param subject         订单标题；没有时为 {@code null}
 * @param body            订单描述；没有时为 {@code null}
 * @param passbackParams  公用回传参数；没有时为 {@code null}
 * @param paymentTime     付款时间；没有时为 {@code null}
 * @param closeTime       关闭时间；没有时为 {@code null}
 */
public record TradeNotification(
        String notifyId,
        LocalDateTime notifyTime,
        String appId,
        String sellerId,
        String tradeNo,
        String outTradeNo,
        String tradeState,
        long totalAmount,
        @Nullable Long receiptAmount,
        @Nullable Long buyerPayAmount,
        @Nullable Long refundAmount,
        @Nullable String buyerOpenId,
        @Nullable String subject,
        @Nullable String body,
        @Nullable String passbackParams,
        @Nullable LocalDateTime paymentTime,
        @Nullable LocalDateTime closeTime
) {

    /**
     * 使用后端可信订单记录核对订单号和订单金额。
     *
     * @param expectedOutTradeNo 可信商户订单号
     * @param expectedAmount     可信订单金额，单位为分
     * @return 当前通知
     * @throws AlipaySecurityException 任一业务字段不匹配
     */
    public TradeNotification requireOrder(String expectedOutTradeNo, long expectedAmount) {
        if (!outTradeNo.equals(expectedOutTradeNo) || totalAmount != expectedAmount) {
            throw new AlipaySecurityException(
                    AlipaySecurityFailure.RESPONSE_MISMATCH);
        }
        return this;
    }

    /**
     * 判断交易通知是否表示买家已完成付款。
     *
     * @return 仅当状态为支付成功或交易完成时返回 {@code true}
     */
    public boolean paid() {
        return TradeState.TRADE_SUCCESS.equals(tradeState)
                || TradeState.TRADE_FINISHED.equals(tradeState);
    }
}
