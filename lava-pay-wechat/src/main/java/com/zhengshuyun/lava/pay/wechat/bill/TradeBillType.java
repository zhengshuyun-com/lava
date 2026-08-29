/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.bill;

/**
 * 交易账单类型。
 */
public enum TradeBillType {
    /**
     * 返回当日所有订单。
     */
    ALL,
    /**
     * 仅返回支付成功订单。
     */
    SUCCESS,
    /**
     * 仅返回退款订单。
     */
    REFUND
}
