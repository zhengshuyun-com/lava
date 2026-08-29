/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.refund;

/**
 * 退款资金账户常量。
 */
public final class RefundFundsAccount {
    /** 未结算资金。 */
    public static final String UNSETTLED = "UNSETTLED";
    /** 可用余额。 */
    public static final String AVAILABLE = "AVAILABLE";
    /** 不可用余额。 */
    public static final String UNAVAILABLE = "UNAVAILABLE";
    /** 运营账户。 */
    public static final String OPERATION = "OPERATION";
    /** 基本账户。 */
    public static final String BASIC = "BASIC";
    /** 数字人民币基本账户。 */
    public static final String ECNY_BASIC = "ECNY_BASIC";

    private RefundFundsAccount() {
    }
}
