/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.common.http;

import com.google.common.base.MoreObjects;
import com.zhengshuyun.common.core.lang.Validate;
import okhttp3.Headers;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * HTTP 单次调用元数据
 * <p>
 * 记录请求的追踪信息, 用于日志记录、性能监控、问题排查
 *
 * @author Toint
 * @since 2026/1/8
 */
public final class HttpCallMetadata {

    /**
     * 请求唯一标识 (UUID) 
     */
    private final String requestId;

    /**
     * 请求 URL
     */
    private final String url;

    /**
     * 请求方法 (GET、POST 等) 
     */
    private final String method;

    /**
     * 请求发送时间
     */
    private final Instant requestTime;

    /**
     * 响应接收时间
     */
    private final Instant responseTime;

    /**
     * 请求头
     */
    private final Headers requestHeaders;

    /**
     * 响应头
     */
    private final Headers responseHeaders;

    /**
     * 请求耗时
     */
    private final Duration duration;

    /**
     * HTTP 协议版本 (如 HTTP/1.1、HTTP/2) 
     */
    private final @Nullable String protocol;

    /**
     * HTTP 响应状态码
     */
    private final int statusCode;

    /**
     * HTTP 响应状态消息 (如 "OK"、"Not Found") 
     */
    private final @Nullable String statusMessage;

    private HttpCallMetadata(Builder builder) {
        // 验证必填字段并赋值
        this.requestId = Validate.notNull(builder.requestId, "requestId must not be null");
        this.url = Validate.notNull(builder.url, "url must not be null");
        this.method = Validate.notNull(builder.method, "method must not be null");
        this.requestTime = Validate.notNull(builder.requestTime, "requestTime must not be null");
        this.responseTime = Validate.notNull(builder.responseTime, "responseTime must not be null");
        this.requestHeaders = Validate.notNull(builder.requestHeaders, "requestHeaders must not be null");
        this.responseHeaders = Validate.notNull(builder.responseHeaders, "responseHeaders must not be null");
        this.protocol = builder.protocol;
        this.statusCode = builder.statusCode;
        this.statusMessage = builder.statusMessage;

        // 验证时间关系并计算请求耗时
        Validate.isTrue(!requestTime.isAfter(responseTime), "requestTime must not be after responseTime");
        this.duration = Duration.between(requestTime, responseTime);
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Instant getRequestTime() {
        return requestTime;
    }

    public Instant getResponseTime() {
        return responseTime;
    }

    public Duration getDuration() {
        return duration;
    }

    /**
     * 获取请求耗时 (毫秒) 
     */
    public long getDurationMillis() {
        return duration.toMillis();
    }

    public @Nullable String getProtocol() {
        return protocol;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public @Nullable String getStatusMessage() {
        return statusMessage;
    }

    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * 判断响应是否成功 (2xx) 
     *
     * @return 状态码在 200-299 范围内返回 true
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public static final class Builder {
        private String requestId;
        private String url;
        private String method;
        private Instant requestTime;
        private Instant responseTime;
        private Headers requestHeaders;
        private Headers responseHeaders;
        private @Nullable String protocol;
        private int statusCode;
        private @Nullable String statusMessage;

        private Builder() {
        }

        public HttpCallMetadata build() {
            return new HttpCallMetadata(this);
        }

        public Builder setRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setRequestTime(Instant requestTime) {
            this.requestTime = requestTime;
            return this;
        }

        public Builder setResponseTime(Instant responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        public Builder setProtocol(@Nullable String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setRequestHeaders(Headers requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }

        public Builder setResponseHeaders(Headers responseHeaders) {
            this.responseHeaders = responseHeaders;
            return this;
        }

        public Builder setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder setStatusMessage(@Nullable String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("requestId", requestId)
                .add("url", url)
                .add("method", method)
                .add("requestTime", requestTime)
                .add("responseTime", responseTime)
                .add("requestHeaders", requestHeaders)
                .add("responseHeaders", responseHeaders)
                .add("duration", duration)
                .add("protocol", protocol)
                .add("statusCode", statusCode)
                .add("statusMessage", statusMessage)
                .toString();
    }
}