/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.http;

import okhttp3.Response;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 显式流式响应。关闭此对象会关闭响应体并释放调用。
 */
final class HttpStreamingResponse implements AutoCloseable {
    /**
     * 持有网络连接和只能消费一次响应体的底层响应。
     */
    private final Response response;
    /**
     * 创建时快照的响应头，避免后续依赖底层对象。
     */
    private final HttpHeaders headers;
    /**
     * 已脱敏的调用元数据。
     */
    private final HttpCallMetadata metadata;
    /**
     * 响应关闭后用于注销客户端活动调用的回调。
     */
    private final Runnable onClose;
    /**
     * 保证底层响应和注销回调只执行一次。
     */
    private final AtomicBoolean closed = new AtomicBoolean();
    /**
     * 保证同一响应体不会被重复获取为流。
     */
    private final AtomicBoolean bodyClaimed = new AtomicBoolean();

    HttpStreamingResponse(Response response, HttpCallMetadata metadata, Runnable onClose) {
        this.response = response;
        this.headers = HttpHeaders.fromOkHttp(response.headers());
        this.metadata = metadata;
        this.onClose = onClose;
    }

    /**
     * 返回 HTTP 响应状态码。
     *
     * @return 状态码
     */
    public int getCode() {
        return response.code();
    }

    /**
     * 返回 HTTP 响应状态文本。
     *
     * @return 状态文本
     */
    public String getMessage() {
        return response.message();
    }

    /**
     * 判断响应是否为 2xx。
     *
     * @return 2xx 时返回 true
     */
    public boolean isSuccessful() {
        return response.isSuccessful();
    }

    /**
     * 判断响应是否为重定向状态。
     *
     * @return 3xx 时返回 true
     */
    public boolean isRedirect() {
        return response.isRedirect();
    }

    /**
     * 返回全部响应头。
     *
     * @return 响应头集合
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * 返回指定名称的全部响应头值。
     *
     * @param name 响应头名称
     * @return 响应头值列表
     */
    public List<String> getHeaders(String name) {
        return headers.values(name);
    }

    /**
     * 返回指定名称的第一个响应头值。
     *
     * @param name 响应头名称
     * @return 响应头值；不存在时返回 null
     */
    public @Nullable String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * 返回服务端声明的正文长度。
     *
     * @return 字节长度；未知时返回 -1
     */
    public long getContentLength() {
        return response.body().contentLength();
    }

    /**
     * 返回由 Content-Type 推断出的正文字符集。
     *
     * @return 正文字符集
     */
    public Charset getCharset() {
        return HttpResponse.responseCharset(response);
    }

    /**
     * 返回只能消费一次的网络流。
     *
     * @return 正文输入流
     */
    public InputStream getBodyAsStream() {
        if (closed.get()) {
            throw new IllegalStateException("response is closed");
        }
        if (!bodyClaimed.compareAndSet(false, true)) {
            throw new IllegalStateException("response body stream has already been obtained");
        }
        // 流的关闭仍依赖响应句柄，调用方应以 try-with-resources 管理本对象。
        return response.body().byteStream();
    }

    /**
     * 返回本次调用的已脱敏元数据。
     *
     * @return 调用元数据
     */
    public HttpCallMetadata getMetadata() {
        return metadata;
    }

    /**
     * 返回协商后的 HTTP 协议。
     *
     * @return 协议名称
     */
    public String getProtocol() {
        return response.protocol().toString();
    }

    /**
     * 关闭响应体、释放连接并注销活动调用；可重复调用。
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                response.close();
            } finally {
                // 无论响应关闭是否抛错，都必须注销活动调用，避免 close 后仍被客户端追踪。
                onClose.run();
            }
        }
    }

    @Override
    public String toString() {
        return metadata.toString();
    }
}
