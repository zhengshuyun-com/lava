/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.exception;

import org.jspecify.annotations.Nullable;

/**
 * 支付宝网关返回的已验签 API 失败。
 */
public final class AlipayPayApiException extends AlipayPayException {
    /** 支付宝网关返回码。 */
    private final String code;
    /** 支付宝网关返回描述。 */
    private final String apiMessage;
    /** 支付宝业务返回码。 */
    private final @Nullable String subCode;
    /** 支付宝业务返回描述。 */
    private final @Nullable String subMessage;
    /** 支付宝链路标识。 */
    private final @Nullable String traceId;

    /**
     * 创建结构化 API 异常。异常文本不包含原始响应或签名内容。
     *
     * @param code       网关返回码
     * @param apiMessage 网关返回描述
     * @param subCode    可选业务返回码
     * @param subMessage 可选业务返回描述
     * @param traceId    可选链路标识
     */
    public AlipayPayApiException(String code, String apiMessage,
                                 @Nullable String subCode,
                                 @Nullable String subMessage,
                                 @Nullable String traceId) {
        super("支付宝 API 调用失败，code=" + code
                + (subCode == null ? "" : ", subCode=" + subCode));
        this.code = code;
        this.apiMessage = apiMessage;
        this.subCode = subCode;
        this.subMessage = subMessage;
        this.traceId = traceId;
    }

    /**
     * 获取支付宝网关返回码。
     *
     * @return 网关返回码
     */
    public String code() {
        return code;
    }

    /**
     * 获取支付宝网关返回描述。
     *
     * @return 网关返回描述
     */
    public String apiMessage() {
        return apiMessage;
    }

    /**
     * 获取支付宝业务返回码。
     *
     * @return 业务返回码；没有时为 {@code null}
     */
    public @Nullable String subCode() {
        return subCode;
    }

    /**
     * 获取支付宝业务返回描述。
     *
     * @return 业务返回描述；没有时为 {@code null}
     */
    public @Nullable String subMessage() {
        return subMessage;
    }

    /**
     * 获取支付宝链路标识。
     *
     * @return 支付宝链路标识；没有时为 {@code null}
     */
    public @Nullable String traceId() {
        return traceId;
    }
}
