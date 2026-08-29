/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.refund;

/**
 * 微信支付退款渠道常量。
 */
public final class RefundChannel {
    /** 原路退款。 */
    public static final String ORIGINAL = "ORIGINAL";
    /** 退回到余额。 */
    public static final String BALANCE = "BALANCE";
    /** 原账户异常时退回其他余额账户。 */
    public static final String OTHER_BALANCE = "OTHER_BALANCE";
    /** 原银行卡异常时退回其他银行卡。 */
    public static final String OTHER_BANKCARD = "OTHER_BANKCARD";

    private RefundChannel() {
    }
}
