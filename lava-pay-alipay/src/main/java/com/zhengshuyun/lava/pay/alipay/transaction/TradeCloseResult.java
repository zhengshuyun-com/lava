/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.transaction;

import org.jspecify.annotations.Nullable;

/**
 * 已验签交易关闭结果。
 *
 * @param tradeNo    支付宝交易号；没有时为 {@code null}
 * @param outTradeNo 商户订单号；没有时为 {@code null}
 */
public record TradeCloseResult(@Nullable String tradeNo, @Nullable String outTradeNo) {
}
