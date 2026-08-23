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

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * 单次 HTTP 交互的凭证安全元数据。
 */
public final class HttpCallMetadata {
    /**
     * 客户端为本次调用生成的唯一标识。
     */
    private final String requestId;
    /**
     * 已脱敏的最终请求 URL。
     */
    private final String url;
    /**
     * HTTP 请求方法名称。
     */
    private final String method;
    /**
     * 请求开始执行的时间点。
     */
    private final Instant requestTime;
    /**
     * 收到响应头的时间点。
     */
    private final Instant responseTime;
    /**
     * 已脱敏的有效请求头。
     */
    private final HttpHeaders requestHeaders;
    /**
     * 已脱敏的响应头。
     */
    private final HttpHeaders responseHeaders;
    /**
     * 从请求开始到收到响应头的耗时。
     */
    private final Duration duration;
    /**
     * 协商后的 HTTP 协议；不可用时为 null。
     */
    private final @Nullable String protocol;
    /**
     * 服务端返回的 HTTP 状态码。
     */
    private final int statusCode;
    /**
     * 服务端返回的 HTTP 状态文本；不可用时为 null。
     */
    private final @Nullable String statusMessage;

    private HttpCallMetadata(Builder builder) {
        requestId = ValidationUtils.requireNonNull(builder.requestId, "requestId");
        url = HttpRedactionUtils.redactUrl(ValidationUtils.requireNonNull(builder.url, "url"));
        method = ValidationUtils.requireNonNull(builder.method, "method");
        requestTime = ValidationUtils.requireNonNull(builder.requestTime, "requestTime");
        responseTime = ValidationUtils.requireNonNull(builder.responseTime, "responseTime");
        requestHeaders = ValidationUtils.requireNonNull(builder.requestHeaders, "requestHeaders").redacted();
        responseHeaders = ValidationUtils.requireNonNull(builder.responseHeaders, "responseHeaders").redacted();
        duration = ValidationUtils.requireNonNull(builder.duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        protocol = builder.protocol;
        statusCode = builder.statusCode;
        statusMessage = builder.statusMessage;
    }

    /**
     * 创建调用元数据构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回调用唯一标识。
     *
     * @return 请求 ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 返回已脱敏的 URL。
     */
    public String getUrl() {
        return url;
    }

    /**
     * 返回 HTTP 请求方法名称。
     *
     * @return 方法名称
     */
    public String getMethod() {
        return method;
    }

    /**
     * 返回请求开始时间。
     *
     * @return 请求开始时间
     */
    public Instant getRequestTime() {
        return requestTime;
    }

    /**
     * 返回收到响应头的时间。
     *
     * @return 响应时间
     */
    public Instant getResponseTime() {
        return responseTime;
    }

    /**
     * 返回本次调用耗时。
     *
     * @return 非负耗时
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * 以毫秒返回本次调用耗时。
     *
     * @return 耗时毫秒数
     */
    public long getDurationMillis() {
        return duration.toMillis();
    }

    /**
     * 返回协商后的 HTTP 协议。
     *
     * @return 协议名称；不可用时为 null
     */
    public @Nullable String getProtocol() {
        return protocol;
    }

    /**
     * 返回服务端状态码。
     *
     * @return HTTP 状态码
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * 返回服务端状态文本。
     *
     * @return 状态文本；不可用时为 null
     */
    public @Nullable String getStatusMessage() {
        return statusMessage;
    }

    /**
     * 返回将凭证替换为 {@code [REDACTED]} 的请求头。
     */
    public HttpHeaders getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * 返回将凭证替换为 {@code [REDACTED]} 的响应头。
     */
    public HttpHeaders getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * 判断状态码是否为 2xx。
     *
     * @return 2xx 时返回 true
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    @Override
    public String toString() {
        return "HttpCallMetadata[requestId=" + requestId
                + ", url=" + url
                + ", method=" + method
                + ", requestTime=" + requestTime
                + ", responseTime=" + responseTime
                + ", requestHeaders=" + requestHeaders
                + ", responseHeaders=" + responseHeaders
                + ", duration=" + duration
                + ", protocol=" + protocol
                + ", statusCode=" + statusCode
                + ']';
    }

    public static final class Builder {
        /**
         * 待构建的请求 ID。
         */
        private @Nullable String requestId;
        /**
         * 待构建的请求 URL。
         */
        private @Nullable String url;
        /**
         * 待构建的 HTTP 方法名称。
         */
        private @Nullable String method;
        /**
         * 待构建的请求开始时间。
         */
        private @Nullable Instant requestTime;
        /**
         * 待构建的响应时间。
         */
        private @Nullable Instant responseTime;
        /**
         * 待构建的请求头。
         */
        private @Nullable HttpHeaders requestHeaders;
        /**
         * 待构建的响应头。
         */
        private @Nullable HttpHeaders responseHeaders;
        /**
         * 待构建的调用耗时。
         */
        private @Nullable Duration duration;
        /**
         * 待构建的 HTTP 协议。
         */
        private @Nullable String protocol;
        /**
         * 待构建的 HTTP 状态码。
         */
        private int statusCode;
        /**
         * 待构建的 HTTP 状态文本。
         */
        private @Nullable String statusMessage;

        private Builder() {
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder requestTime(Instant requestTime) {
            this.requestTime = requestTime;
            return this;
        }

        public Builder responseTime(Instant responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder protocol(@Nullable String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder requestHeaders(HttpHeaders requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }

        public Builder responseHeaders(HttpHeaders responseHeaders) {
            this.responseHeaders = responseHeaders;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder statusMessage(@Nullable String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        public HttpCallMetadata build() {
            return new HttpCallMetadata(this);
        }
    }
}
