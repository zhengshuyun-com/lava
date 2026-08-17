/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.Charset;

/** 简洁命名的流式响应句柄；关闭后释放底层网络调用。 */
public final class HttpStream implements AutoCloseable {
    /** 实际持有网络响应和关闭责任的底层句柄。 */
    private final HttpStreamingResponse delegate;

    HttpStream(HttpStreamingResponse delegate) {
        this.delegate = delegate;
    }

    /**
     * 返回 HTTP 响应状态码。
     *
     * @return 状态码
     */
    public int statusCode() {
        return delegate.getCode();
    }

    /**
     * 返回 HTTP 响应状态文本。
     *
     * @return 状态文本
     */
    public String statusMessage() {
        return delegate.getMessage();
    }

    /**
     * 判断响应是否为 2xx。
     *
     * @return 2xx 时返回 true
     */
    public boolean isSuccessful() {
        return delegate.isSuccessful();
    }

    /**
     * 判断响应是否为重定向状态。
     *
     * @return 3xx 时返回 true
     */
    public boolean isRedirect() {
        return delegate.isRedirect();
    }

    /**
     * 返回全部响应头。
     *
     * @return 响应头集合
     */
    public HttpHeaders headers() {
        return delegate.getHeaders();
    }

    /**
     * 按名称返回第一个响应头值。
     *
     * @param name 响应头名称
     * @return 响应头值；不存在时为 null
     */
    public @Nullable String header(String name) {
        return delegate.getHeader(name);
    }

    /**
     * 返回响应声明的正文长度。
     *
     * @return 字节长度；未知时为 -1
     */
    public long contentLength() {
        return delegate.getContentLength();
    }

    /**
     * 返回从 Content-Type 推断出的正文字符集。
     *
     * @return 字符集
     */
    public Charset charset() {
        return delegate.getCharset();
    }

    /**
     * 返回协商后的 HTTP 协议。
     *
     * @return 协议名称
     */
    public String protocol() {
        return delegate.getProtocol();
    }

    /**
     * 获取只能读取一次的正文流。
     *
     * @return 正文输入流
     */
    public InputStream body() {
        return delegate.getBodyAsStream();
    }

    /**
     * 返回本次调用的已脱敏元数据。
     *
     * @return 调用元数据
     */
    public HttpCallMetadata metadata() {
        return delegate.getMetadata();
    }

    /** 关闭底层响应并释放网络调用。 */
    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
