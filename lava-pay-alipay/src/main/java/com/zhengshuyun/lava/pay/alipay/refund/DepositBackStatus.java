/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

/**
 * 银行卡冲退状态常量。模型保留原始字符串以兼容新增状态。
 */
public final class DepositBackStatus {
    /** 银行卡冲退成功。 */
    public static final String SUCCESS = "S";
    /** 银行卡冲退失败，资金转入用户支付宝余额。 */
    public static final String FAILED = "F";
    /** 银行卡冲退处理中。 */
    public static final String PROCESSING = "P";

    private DepositBackStatus() {
        throw new UnsupportedOperationException("Constants class");
    }
}
