/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.exception;

import com.zhengshuyun.lava.http.HttpFailureKind;
import org.jspecify.annotations.Nullable;

/**
 * 支付宝网关调用未获得可验证协议响应。
 */
public final class AlipayTransportException extends AlipayException {
    /** 底层传输失败类型。 */
    private final @Nullable HttpFailureKind kind;
    /** 支付宝网关返回的非成功 HTTP 状态码。 */
    private final @Nullable Integer statusCode;
    /** 失败请求的 HTTP 方法。 */
    private final @Nullable String method;
    /** 失败请求地址。 */
    private final @Nullable String url;
    /** 底层异常类型。 */
    private final @Nullable String causeType;

    /**
     * 将 Lava HTTP 传输失败转换为支付宝领域异常。
     *
     * @param kind      失败类型
     * @param method    HTTP 方法
     * @param url       请求地址
     * @param causeType 底层异常类型
     */
    public AlipayTransportException(
            HttpFailureKind kind,
            @Nullable String method,
            @Nullable String url,
            @Nullable String causeType
    ) {
        super("支付宝网关传输失败：" + kind);
        this.kind = kind;
        statusCode = null;
        this.method = method;
        this.url = url;
        this.causeType = causeType;
    }

    /**
     * 表示支付宝网关返回了非成功 HTTP 状态。
     *
     * @param statusCode HTTP 状态码
     */
    public AlipayTransportException(int statusCode) {
        super("支付宝网关返回非成功 HTTP 状态：" + statusCode);
        kind = null;
        this.statusCode = statusCode;
        method = null;
        url = null;
        causeType = null;
    }

    /**
     * 获取底层传输失败类型。
     *
     * @return 传输失败类型；HTTP 状态失败时为 {@code null}
     */
    public @Nullable HttpFailureKind kind() {
        return kind;
    }

    /**
     * 获取支付宝网关返回的 HTTP 状态码。
     *
     * @return HTTP 状态码；底层传输失败时为 {@code null}
     */
    public @Nullable Integer statusCode() {
        return statusCode;
    }

    /**
     * 获取失败请求的 HTTP 方法。
     *
     * @return HTTP 方法；没有时为 {@code null}
     */
    public @Nullable String method() {
        return method;
    }

    /**
     * 获取失败请求地址。
     *
     * @return 请求地址；没有时为 {@code null}
     */
    public @Nullable String url() {
        return url;
    }

    /**
     * 获取底层异常类型。
     *
     * @return 底层异常类型；没有时为 {@code null}
     */
    public @Nullable String causeType() {
        return causeType;
    }
}
