/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.HttpClient;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 支付宝根客户端与各功能入口共享的传输资源和关闭状态。
 */
public final class AlipayPayRuntime implements AutoCloseable {
    private final AlipayPayTransport transport;
    private final HttpClient httpClient;
    private final boolean ownsHttpClient;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 创建共享运行时。
     *
     * @param transport      协议传输层
     * @param httpClient     HTTP 客户端
     * @param ownsHttpClient 是否负责关闭 HTTP 客户端
     */
    public AlipayPayRuntime(AlipayPayTransport transport, HttpClient httpClient,
                            boolean ownsHttpClient) {
        this.transport = ValidationUtils.requireNonNull(transport, "transport");
        this.httpClient = ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.ownsHttpClient = ownsHttpClient;
    }

    /**
     * 返回仍可用的协议传输层。
     *
     * @return 传输层
     * @throws IllegalStateException 根客户端已经关闭
     */
    public AlipayPayTransport transport() {
        ensureOpen();
        return transport;
    }

    /**
     * 确认根客户端仍可使用。
     */
    public void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("AlipayPayClient is closed");
        }
    }

    /**
     * 幂等关闭自建 HTTP 资源。借入客户端由调用方管理。
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && ownsHttpClient) {
            httpClient.close();
        }
    }
}
