/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import org.jspecify.annotations.Nullable;

/**
 * 凭证安全的 HTTP 传输或响应缓冲失败。
 */
public final class HttpException extends RuntimeException {
    /**
     * 供调用方稳定判断的失败分类。
     */
    private final HttpFailureKind kind;
    /**
     * 出错请求的方法；无法确定时为 null。
     */
    private final @Nullable String method;
    /**
     * 已脱敏的出错请求 URL；无法确定时为 null。
     */
    private final @Nullable String url;
    /**
     * 原始传输异常的类名，避免保留可能含凭证的异常对象。
     */
    private final @Nullable String transportCauseType;

    HttpException(HttpFailureKind kind, @Nullable String method, @Nullable String rawUrl,
                  String detail, @Nullable Throwable cause) {
        super(format(kind, method, rawUrl, detail));
        this.kind = kind;
        this.method = method;
        this.url = rawUrl == null ? null : HttpRedactionUtils.redactUrl(rawUrl);
        transportCauseType = cause == null ? null : cause.getClass().getName();
    }

    /**
     * 返回稳定的失败分类。
     *
     * @return 失败分类
     */
    public HttpFailureKind getKind() {
        return kind;
    }

    /**
     * 返回出错请求的方法。
     *
     * @return HTTP 方法；无法确定时为 null
     */
    public @Nullable String getMethod() {
        return method;
    }

    /**
     * 返回已脱敏用户信息和敏感查询参数的 URL。
     */
    public @Nullable String getUrl() {
        return url;
    }

    /**
     * 返回原始传输异常的类型名，但不保留该异常。
     *
     * <p>原始异常的消息、被抑制异常或堆栈跟踪中可能包含凭证，因此这里有意不通过
     * {@link #getCause()} 暴露原始异常。</p>
     */
    public @Nullable String getTransportCauseType() {
        return transportCauseType;
    }

    private static String format(HttpFailureKind kind, @Nullable String method,
                                 @Nullable String rawUrl, String detail) {
        StringBuilder result = new StringBuilder("HTTP ").append(kind);
        if (method != null) {
            result.append(" during ").append(method);
        }
        if (rawUrl != null) {
            result.append(' ').append(HttpRedactionUtils.redactUrl(rawUrl));
        }
        if (!detail.isBlank()) {
            result.append(": ").append(detail);
        }
        return result.toString();
    }
}
