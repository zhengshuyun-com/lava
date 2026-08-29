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
 * 微信支付交易类型常量。响应模型保留原始字符串，以兼容微信后续新增类型。
 */
public final class TradeType {
    /** 公众号或小程序支付。 */
    public static final String JSAPI = "JSAPI";
    /** Native 扫码支付。 */
    public static final String NATIVE = "NATIVE";
    /** APP 支付。 */
    public static final String APP = "APP";
    /** 付款码支付。 */
    public static final String MICROPAY = "MICROPAY";
    /** H5 支付。 */
    public static final String MWEB = "MWEB";
    /** 刷脸支付。 */
    public static final String FACEPAY = "FACEPAY";

    private TradeType() {
    }
}
