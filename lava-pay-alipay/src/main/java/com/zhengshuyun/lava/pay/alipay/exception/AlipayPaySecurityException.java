/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.exception;

/**
 * 支付宝消息来源或业务一致性校验失败。
 */
public final class AlipayPaySecurityException extends AlipayPayException {
    /** 稳定的安全校验失败分类。 */
    private final AlipayPaySecurityFailure failure;

    /**
     * 创建安全异常。
     *
     * @param failure 稳定失败分类
     */
    public AlipayPaySecurityException(AlipayPaySecurityFailure failure) {
        super("支付宝安全校验失败：" + failure);
        this.failure = failure;
    }

    /**
     * 获取稳定的安全校验失败分类。
     *
     * @return 稳定失败分类
     */
    public AlipayPaySecurityFailure failure() {
        return failure;
    }
}
