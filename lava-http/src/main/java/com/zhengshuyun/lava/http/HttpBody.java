/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

/** 不依赖具体传输实现的请求体。 */
@FunctionalInterface
public interface HttpBody {
    /**
     * 将请求体写入传输层提供的输出流。
     *
     * @param output 由传输层关闭的输出流
     * @throws IOException 写入失败时抛出
     */
    void writeTo(OutputStream output) throws IOException;

    /**
     * 返回请求体的媒体类型。
     *
     * @return 媒体类型；未知时返回 null
     */
    default @Nullable String contentType() {
        return null;
    }

    /**
     * 返回请求体长度。
     *
     * @return 字节长度；未知时返回 -1
     */
    default long contentLength() {
        return -1L;
    }
}
