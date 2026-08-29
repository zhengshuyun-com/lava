/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.transaction;

/**
 * 交易查询可选扩展字段常量。
 */
public final class TradeQueryOption {
    /** 交易结算信息。 */
    public static final String TRADE_SETTLE_INFO = "trade_settle_info";
    /** 支付资金渠道。 */
    public static final String FUND_BILL_LIST = "fund_bill_list";
    /** 支付优惠券信息。 */
    public static final String VOUCHER_DETAIL_LIST = "voucher_detail_list";
    /** 单品券商品优惠信息。 */
    public static final String DISCOUNT_GOODS_DETAIL = "discount_goods_detail";
    /** 商家优惠金额。 */
    public static final String MDISCOUNT_AMOUNT = "mdiscount_amount";
    /** 医保信息。 */
    public static final String MEDICAL_INSURANCE_INFO = "medical_insurance_info";

    /** 禁止实例化交易查询选项常量容器。 */
    private TradeQueryOption() {
        throw new UnsupportedOperationException("Constants class");
    }
}
