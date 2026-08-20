/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

/**
 * 调用方显式要求成功但服务端返回非 2xx 时抛出的异常。
 */
public final class HttpStatusException extends RuntimeException {
    private static final int MAX_ERROR_BODY_BYTES = 64 * 1024;
    private final int statusCode;
    private final HttpHeaders headers;
    private final String responseBody;

    HttpStatusException(HttpResponse response) {
        super("HTTP status " + response.getCode() + " from " + response.getMetadata().getUrl());
        statusCode = response.getCode();
        headers = response.getHeaders().redacted();
        byte[] body = response.getBodyAsBytes();
        int length = Math.min(body.length, MAX_ERROR_BODY_BYTES);
        responseBody = new String(body, 0, length, response.getCharset());
    }

    public int statusCode() {
        return statusCode;
    }

    public HttpHeaders headers() {
        return headers;
    }

    /**
     * 返回有界响应正文；异常的 toString 不会包含正文。
     */
    public String responseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        return "HttpStatusException[statusCode=" + statusCode
                + ", headers=" + headers + ", responseBody=[REDACTED]]";
    }
}
