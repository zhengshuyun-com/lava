/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.refund;

/**
 * 微信支付退款状态常量。响应保留原始字符串以兼容新增状态。
 */
public final class RefundStatus {
    /** 退款成功。 */
    public static final String SUCCESS = "SUCCESS";
    /** 退款关闭。 */
    public static final String CLOSED = "CLOSED";
    /** 退款处理中。 */
    public static final String PROCESSING = "PROCESSING";
    /** 退款异常。 */
    public static final String ABNORMAL = "ABNORMAL";

    private RefundStatus() {
    }
}
