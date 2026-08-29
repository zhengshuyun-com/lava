/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.pay.wechat.transaction;

/**
 * 微信支付交易状态常量。响应模型保留原始字符串，以兼容微信后续新增状态。
 */
public final class TradeState {
    /** 支付成功。 */
    public static final String SUCCESS = "SUCCESS";
    /** 转入退款。 */
    public static final String REFUND = "REFUND";
    /** 尚未支付。 */
    public static final String NOTPAY = "NOTPAY";
    /** 订单已关闭。 */
    public static final String CLOSED = "CLOSED";
    /** 订单已撤销。 */
    public static final String REVOKED = "REVOKED";
    /** 用户支付中。 */
    public static final String USERPAYING = "USERPAYING";
    /** 支付失败。 */
    public static final String PAYERROR = "PAYERROR";

    /** 禁止实例化交易状态常量容器。 */
    private TradeState() {
    }
}
