/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

/**
 * 不暴露原始异常消息的 SSE 失败快照。
 *
 * @param kind         稳定的失败分类
 * @param causeType    原始异常的类型名
 * @param statusCode   服务端已响应时的 HTTP 状态码
 * @param headers      服务端已响应时的已脱敏响应头
 * @param responseBody 有界失败响应正文
 */
public record SseFailure(HttpFailureKind kind, @Nullable String causeType,
                         @Nullable Integer statusCode, @Nullable HttpHeaders headers,
                         @Nullable String responseBody) {
    public SseFailure {
        ValidationUtils.requireNonNull(kind, "kind must not be null");
    }

    /**
     * 从兼容 API 的失败对象创建不携带 Throwable 的安全快照。
     *
     * @param failure 兼容 API 失败对象；null 表示协议级失败
     * @return 通用失败快照
     */
    static SseFailure from(@Nullable HttpSseFailure failure) {
        if (failure == null) {
            return new SseFailure(HttpFailureKind.PROTOCOL, null, null, null, null);
        }
        Throwable throwable = failure.throwable();
        return new SseFailure(failure.kind(), throwable == null ? null : throwable.getClass().getName(),
                failure.statusCode(), failure.headers() == null ? null : failure.headers().redacted(),
                failure.responseBody());
    }

    /**
     * 返回不泄露响应正文的调试表示。
     *
     * @return 已脱敏的失败摘要
     */
    @Override
    public String toString() {
        return "SseFailure[kind=" + kind + ", causeType=" + causeType
                + ", statusCode=" + statusCode + ", headers=" + headers
                + ", responseBody=[REDACTED]]";
    }
}
