/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

/**
 * 退款与退款查询扩展返回字段常量。
 */
public final class RefundQueryOption {
    /** 本次退款使用的资金渠道。 */
    public static final String REFUND_DETAIL_ITEM_LIST = "refund_detail_item_list";
    /** 退款执行成功时间。 */
    public static final String GMT_REFUND_PAY = "gmt_refund_pay";
    /** 银行卡冲退信息，同时用于订阅冲退完成通知。 */
    public static final String DEPOSIT_BACK_INFO = "deposit_back_info";
    /** 本次退款退回的优惠券信息。 */
    public static final String REFUND_VOUCHER_DETAIL_LIST = "refund_voucher_detail_list";

    /** 禁止实例化退款查询选项常量容器。 */
    private RefundQueryOption() {
        throw new UnsupportedOperationException("Constants class");
    }
}
