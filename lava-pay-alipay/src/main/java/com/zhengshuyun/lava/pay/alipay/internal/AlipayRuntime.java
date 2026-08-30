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
public final class AlipayRuntime implements AutoCloseable {
    /** 各业务入口共用的 OpenAPI V3 传输层。 */
    private final AlipayTransport transport;
    /** 页面支付专用的 AOP 签名表单生成器。 */
    private final AlipayPagePayFormFactory pagePayFormFactory;
    /** 传输层实际使用的 HTTP 客户端。 */
    private final HttpClient httpClient;
    /** 是否由运行时负责关闭 HTTP 客户端。 */
    private final boolean ownsHttpClient;
    /** 根客户端关闭状态，保证关闭幂等并拒绝关闭后的业务调用。 */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 创建共享运行时。
     *
     * @param transport          V3 协议传输层
     * @param pagePayFormFactory 页面支付表单生成器
     * @param httpClient         HTTP 客户端
     * @param ownsHttpClient     是否负责关闭 HTTP 客户端
     */
    public AlipayRuntime(
            AlipayTransport transport,
            AlipayPagePayFormFactory pagePayFormFactory,
            HttpClient httpClient,
            boolean ownsHttpClient
    ) {
        this.transport = ValidationUtils.requireNonNull(transport, "transport");
        this.pagePayFormFactory = ValidationUtils.requireNonNull(
                pagePayFormFactory,
                "pagePayFormFactory"
        );
        this.httpClient = ValidationUtils.requireNonNull(httpClient, "httpClient");
        this.ownsHttpClient = ownsHttpClient;
    }

    /**
     * 返回仍可用的协议传输层。
     *
     * @return 传输层
     * @throws IllegalStateException 根客户端已经关闭
     */
    public AlipayTransport transport() {
        ensureOpen();
        return transport;
    }

    /**
     * 返回仍可用的页面支付表单生成器。
     *
     * @return 页面支付表单生成器
     * @throws IllegalStateException 根客户端已经关闭
     */
    public AlipayPagePayFormFactory pagePayForms() {
        ensureOpen();
        return pagePayFormFactory;
    }

    /**
     * 确认根客户端仍可使用。
     */
    public void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("AlipayClient is closed");
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
