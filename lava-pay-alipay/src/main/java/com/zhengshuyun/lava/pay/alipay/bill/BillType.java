/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.bill;

/**
 * 普通商户可查询的账单类型常量。
 */
public final class BillType {
    /** 支付宝交易收单业务账单。 */
    public static final String TRADE = "trade";
    /** 商户支付宝余额收支账务账单。 */
    public static final String SIGN_CUSTOMER = "signcustomer";
    /** 营销活动发放和核销账单。 */
    public static final String MERCHANT_ACTIVITY = "merchant_act";
    /** 直付通二级商户交易账单。 */
    public static final String TRADE_ZFT_MERCHANT = "trade_zft_merchant";
    /** 直付通平台商户账务账单。 */
    public static final String ZFT_ACCOUNT = "zft_acc";
    /** 汇总批次结算资金到账账单。 */
    public static final String SETTLEMENT_MERGE = "settlementMerge";

    /** 禁止实例化账单类型常量容器。 */
    private BillType() {
        throw new UnsupportedOperationException("Constants class");
    }
}
