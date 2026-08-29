/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.transaction;

/**
 * 支付宝交易状态常量。响应保留原始字符串以兼容新增状态。
 */
public final class TradeState {
    /** 交易创建，等待买家付款。 */
    public static final String WAIT_BUYER_PAY = "WAIT_BUYER_PAY";
    /** 未付款超时关闭或支付后全额退款。 */
    public static final String TRADE_CLOSED = "TRADE_CLOSED";
    /** 交易支付成功，仍可能退款。 */
    public static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    /** 交易结束，不可退款。 */
    public static final String TRADE_FINISHED = "TRADE_FINISHED";

    /** 禁止实例化交易状态常量容器。 */
    private TradeState() {
        throw new UnsupportedOperationException("Constants class");
    }
}
