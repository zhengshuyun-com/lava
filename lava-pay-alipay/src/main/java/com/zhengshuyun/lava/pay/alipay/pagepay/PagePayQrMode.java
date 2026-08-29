/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.pagepay;

/**
 * 电脑网站支付二维码展示模式常量。
 */
public final class PagePayQrMode {
    /** 简约前置模式。 */
    public static final String SIMPLE_FRONT = "0";
    /** 前置模式。 */
    public static final String FRONT = "1";
    /** 支付宝页面跳转模式。 */
    public static final String REDIRECT = "2";
    /** 迷你前置模式。 */
    public static final String MINI_FRONT = "3";
    /** 可自定义宽度的嵌入式二维码。 */
    public static final String CUSTOM_WIDTH = "4";

    /** 禁止实例化二维码模式常量容器。 */
    private PagePayQrMode() {
        throw new UnsupportedOperationException("Constants class");
    }
}
