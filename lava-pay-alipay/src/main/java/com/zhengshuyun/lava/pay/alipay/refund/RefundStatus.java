/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

/**
 * 支付宝退款查询状态常量。
 */
public final class RefundStatus {
    /** 退款处理成功。 */
    public static final String REFUND_SUCCESS = "REFUND_SUCCESS";

    /** 禁止实例化退款状态常量容器。 */
    private RefundStatus() {
        throw new UnsupportedOperationException("Constants class");
    }
}
