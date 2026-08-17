/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.json.JsonCodec;
import tools.jackson.core.type.TypeReference;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 已完整缓冲的 HTTP 响应；HTTP 错误状态码会正常表示。 */
public final class HttpResponse {
    /** 服务端返回的 HTTP 状态码。 */
    private final int code;
    /** 服务端返回的 HTTP 状态文本。 */
    private final String message;
    /** 响应头快照。 */
    private final HttpHeaders headers;
    /** 已完整缓冲的响应正文。 */
    private final byte[] body;
    /** 从 Content-Type 推断出的正文字符集。 */
    private final Charset charset;
    /** 已脱敏的调用元数据。 */
    private final HttpCallMetadata metadata;
    /** 协商后的 HTTP 协议名称。 */
    private final String protocol;
    /** 用于解码 JSON 正文的客户端编解码器。 */
    private final JsonCodec jsonCodec;

    HttpResponse(int code, String message, HttpHeaders headers, byte[] body, Charset charset,
                 HttpCallMetadata metadata, String protocol) {
        this(code, message, headers, body, charset, metadata, protocol, JsonCodec.defaultCodec());
    }

    HttpResponse(int code, String message, HttpHeaders headers, byte[] body, Charset charset,
                 HttpCallMetadata metadata, String protocol, JsonCodec jsonCodec) {
        this.code = code;
        this.message = message;
        this.headers = headers;
        this.body = body.clone();
        this.charset = charset;
        this.metadata = metadata;
        this.protocol = protocol;
        this.jsonCodec = ValidationUtils.requireNonNull(jsonCodec, "jsonCodec");
    }

    /**
     * 返回 HTTP 状态码。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    public int statusCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String statusMessage() {
        return message;
    }

    /**
     * 判断响应状态码是否为 2xx。
     *
     * @return 2xx 时返回 true
     */
    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    public boolean isRedirect() {
        return code >= 300 && code < 400;
    }

    /**
     * 返回响应头快照。
     *
     * @return 响应头
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public List<String> getHeaders(String name) {
        return headers.values(name);
    }

    public @Nullable String getHeader(String name) {
        return headers.get(name);
    }

    public String getHeaderOrDefault(String name, String defaultValue) {
        String value = getHeader(name);
        return value == null ? defaultValue : value;
    }

    public @Nullable String getContentType() {
        return getHeader(HttpHeaderNames.CONTENT_TYPE);
    }

    public long getContentLength() {
        return body.length;
    }

    public @Nullable String getLocation() {
        return getHeader(HttpHeaderNames.LOCATION);
    }

    public Map<String, String> getCookies() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String header : headers.values(HttpHeaderNames.SET_COOKIE)) {
            int semicolon = header.indexOf(';');
            String pair = semicolon >= 0 ? header.substring(0, semicolon) : header;
            int equals = pair.indexOf('=');
            if (equals > 0) {
                String name = pair.substring(0, equals).strip();
                String value = pair.substring(equals + 1).strip();
                if (value.length() >= 2 && value.charAt(0) == '"'
                        && value.charAt(value.length() - 1) == '"') {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(name, value);
            }
        }
        return Map.copyOf(result);
    }

    public @Nullable String getCookie(String name) {
        return getCookies().get(
                ValidationUtils.requireNotBlank(name, "cookie name must not be blank"));
    }

    /**
     * 返回响应正文的防御性副本。
     *
     * @return 正文字节数组副本
     */
    public byte[] getBodyAsBytes() {
        return body.clone();
    }

    public byte[] bodyBytes() {
        return getBodyAsBytes();
    }

    /**
     * 使用响应字符集将正文解码为字符串。
     *
     * @return 正文文本
     */
    public String getBodyAsString() {
        return new String(body, charset);
    }

    public String bodyString() {
        return getBodyAsString();
    }

    /**
     * 使用指定字符集将正文解码为字符串。
     *
     * @param charset 解码使用的字符集
     * @return 正文文本
     */
    public String getBodyAsString(Charset charset) {
        return new String(body, ValidationUtils.requireNonNull(charset, "charset must not be null"));
    }

    public Charset getCharset() {
        return charset;
    }

    public HttpCallMetadata getMetadata() {
        return metadata;
    }

    public HttpCallMetadata metadata() {
        return metadata;
    }

    public String getProtocol() {
        return protocol;
    }

    /** 显式要求 2xx；失败时保留有界响应上下文。 */
    public HttpResponse requireSuccess() {
        if (!isSuccessful()) {
            throw new HttpStatusException(this);
        }
        return this;
    }

    /** 使用客户端配置的 JSON 编解码器读取响应。 */
    public <T> T bodyAs(Class<T> type) {
        return jsonCodec.read(body,
                ValidationUtils.requireNonNull(type, "type must not be null"));
    }

    /** 使用客户端配置的 JSON 编解码器读取带泛型信息的响应。 */
    public <T> T bodyAs(TypeReference<T> type) {
        return jsonCodec.read(body,
                ValidationUtils.requireNonNull(type, "type must not be null"));
    }

    @Override
    public String toString() {
        return metadata.toString();
    }

    static Charset responseCharset(okhttp3.Response response) {
        okhttp3.MediaType contentType = response.body().contentType();
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }
        Charset result = contentType.charset(StandardCharsets.UTF_8);
        return result == null ? StandardCharsets.UTF_8 : result;
    }
}
