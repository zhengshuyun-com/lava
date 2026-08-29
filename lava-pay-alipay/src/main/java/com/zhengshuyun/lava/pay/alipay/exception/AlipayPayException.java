/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.exception;

/**
 * 支付宝支付协议异常的公共基类。
 */
public abstract class AlipayPayException extends RuntimeException {
    /**
     * 使用不含敏感协议内容的消息创建异常。
     *
     * @param message 安全错误消息
     */
    protected AlipayPayException(String message) {
        super(message);
    }

    /**
     * 使用安全消息和底层原因创建异常。
     *
     * @param message 安全错误消息
     * @param cause   底层异常
     */
    protected AlipayPayException(String message, Throwable cause) {
        super(message, cause);
    }
}
