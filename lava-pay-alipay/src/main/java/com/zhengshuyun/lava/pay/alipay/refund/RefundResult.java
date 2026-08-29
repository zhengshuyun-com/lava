/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 已验签退款申请结果。
 *
 * @param tradeNo        支付宝交易号
 * @param outTradeNo     商户订单号
 * @param fundChange     原始资金变化标志；没有时为 {@code null}
 * @param refundedAmount 该交易累计退款成功金额，单位为分
 * @param sentBackAmount 本次商户实际退回金额，单位为分；没有时为 {@code null}
 * @param buyerOpenId    买家 OpenID；没有时为 {@code null}
 * @param buyerLogonId   脱敏买家登录账号；没有时为 {@code null}
 * @param fundBills      本次退款资金渠道
 */
public record RefundResult(
        String tradeNo,
        String outTradeNo,
        @Nullable String fundChange,
        long refundedAmount,
        @Nullable Long sentBackAmount,
        @Nullable String buyerOpenId,
        @Nullable String buyerLogonId,
        List<RefundFundBill> fundBills
) {
    /**
     * 仅当支付宝明确返回 {@code fund_change=Y} 时表示本次同步退款成功。
     *
     * @return 是否明确成功
     */
    public boolean succeeded() {
        return "Y".equals(fundChange);
    }
}
