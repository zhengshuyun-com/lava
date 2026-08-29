/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.exception;

/**
 * 支付宝请求无法编码或响应不符合协议结构。
 */
public final class AlipayProtocolException extends AlipayException {
    /**
     * 创建协议异常。
     *
     * @param message 不含敏感内容的错误消息
     */
    public AlipayProtocolException(String message) {
        super(message);
    }
}
