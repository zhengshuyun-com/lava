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

package com.zhengshuyun.lava.ai;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.*;
import com.zhengshuyun.lava.json.JsonCodec;
import tools.jackson.core.type.TypeReference;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * 协议中立的 AI 请求便利层，不包含任何供应商字段模型。
 */
public final class AiClient implements AutoCloseable {
    private final HttpClient http;
    private final boolean ownsHttp;
    private final JsonCodec jsonCodec;
    private final Duration sseIdleTimeout;

    private AiClient(HttpClient http, boolean ownsHttp, JsonCodec jsonCodec,
                     Duration sseIdleTimeout) {
        this.http = http;
        this.ownsHttp = ownsHttp;
        this.jsonCodec = jsonCodec;
        this.sseIdleTimeout = sseIdleTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 包装外部 HTTP 客户端；关闭 AI 客户端不会关闭传入的客户端。
     */
    public static AiClient using(HttpClient client) {
        ValidationUtils.requireNonNull(client, "client must not be null");
        return new AiClient(client, false, JsonCodec.defaultCodec(),
                SseOptions.DEFAULT_IDLE_TIMEOUT);
    }

    public HttpClient httpClient() {
        return http;
    }

    /**
     * 发送已经构造好的普通请求。
     */
    public HttpResponse send(HttpRequest request) {
        return http.send(request);
    }

    /**
     * 将对象编码为 JSON 后发送，状态码仍由返回响应表达。
     */
    public HttpResponse sendJson(HttpRequest request, Object body) {
        ValidationUtils.requireNonNull(request, "request must not be null");
        return http.send(request.withBody(com.zhengshuyun.lava.http.HttpBodyUtils.json(body, jsonCodec)));
    }

    /**
     * 发送 JSON 并在成功时直接解码目标类型。
     */
    public <T> T sendJson(HttpRequest request, Object body, Class<T> responseType) {
        ValidationUtils.requireNonNull(responseType, "responseType must not be null");
        return jsonCodec.read(sendJson(request, body).requireSuccess().getBodyAsBytes(), responseType);
    }

    public <T> T sendJson(HttpRequest request, Object body, TypeReference<T> responseType) {
        ValidationUtils.requireNonNull(responseType, "responseType must not be null");
        return jsonCodec.read(sendJson(request, body).requireSuccess().getBodyAsBytes(), responseType);
    }

    /**
     * 打开一个 JSON 请求对应的 SSE 增量流。
     * decoder 可以按供应商协议忽略心跳、解析 data 或识别结束标记。
     */
    public <T> SseSession openJsonStream(HttpRequest request, Object body,
                                         AiChunkDecoder<T> decoder,
                                         AiStreamListener<T> listener) {
        return openJsonStream(request, body,
                SseOptions.builder().idleTimeout(sseIdleTimeout).build(), decoder, listener);
    }

    public <T> SseSession openJsonStream(HttpRequest request, Object body, SseOptions options,
                                         AiChunkDecoder<T> decoder,
                                         AiStreamListener<T> listener) {
        ValidationUtils.requireNonNull(request, "request must not be null");
        ValidationUtils.requireNonNull(options, "options must not be null");
        ValidationUtils.requireNonNull(decoder, "decoder must not be null");
        ValidationUtils.requireNonNull(listener, "listener must not be null");
        HttpRequest effective = request.withBody(
                com.zhengshuyun.lava.http.HttpBodyUtils.json(body, jsonCodec));
        if (!effective.getHeaders().contains(HttpHeaderNames.ACCEPT)) {
            effective = effective.withHeader(HttpHeaderNames.ACCEPT, "text/event-stream");
        }
        return http.openSse(effective, options, new SseListener() {
            @Override
            public void onOpen(SseSession session, int statusCode, HttpHeaders headers) {
                // AI 层不预设供应商握手模型。
            }

            @Override
            public void onEvent(SseSession session, SseEvent event) {
                Optional<T> decoded;
                try {
                    decoded = decoder.decode(event);
                } catch (Exception exception) {
                    throw new IllegalStateException("AI SSE decoder failed", exception);
                }
                decoded.ifPresent(value -> listener.onChunk(session, value));
            }

            @Override
            public void onTerminal(SseSession session, SseTerminal terminal) {
                listener.onTerminal(session, terminal);
            }
        });
    }

    @Override
    public void close() {
        if (ownsHttp) {
            http.close();
        }
    }

    public static final class Builder {
        private HttpClient httpClient;
        private URI baseUrl;
        private HttpHeaders.Builder defaultHeaders = HttpHeaders.builder();
        private JsonCodec jsonCodec = JsonCodec.defaultCodec();
        private Duration connectTimeout = HttpClient.Builder.DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = HttpClient.Builder.DEFAULT_READ_TIMEOUT;
        private Duration writeTimeout = HttpClient.Builder.DEFAULT_WRITE_TIMEOUT;
        private Duration callTimeout = HttpClient.Builder.DEFAULT_CALL_TIMEOUT;
        private Duration sseIdleTimeout = SseOptions.DEFAULT_IDLE_TIMEOUT;

        public Builder httpClient(HttpClient value) {
            httpClient = ValidationUtils.requireNonNull(value, "httpClient must not be null");
            return this;
        }

        public Builder baseUrl(String value) {
            ValidationUtils.requireNotBlank(value, "baseUrl must not be blank");
            try {
                return baseUrl(new URI(value));
            } catch (java.net.URISyntaxException exception) {
                throw new IllegalArgumentException("baseUrl must be a valid URI", exception);
            }
        }

        public Builder baseUrl(URI value) {
            ValidationUtils.requireNonNull(value, "baseUrl must not be null");
            if (!value.isAbsolute()
                    || !("http".equalsIgnoreCase(value.getScheme())
                    || "https".equalsIgnoreCase(value.getScheme()))) {
                throw new IllegalArgumentException("baseUrl must be an absolute HTTP or HTTPS URI");
            }
            if (value.getQuery() != null || value.getFragment() != null) {
                throw new IllegalArgumentException("baseUrl must not contain query or fragment");
            }
            String text = value.toString();
            baseUrl = URI.create(text.endsWith("/") ? text : text + "/");
            return this;
        }

        public Builder defaultHeader(String name, String value) {
            defaultHeaders.set(name, value);
            return this;
        }

        public Builder defaultHeaders(HttpHeaders headers) {
            ValidationUtils.requireNonNull(headers, "headers must not be null");
            defaultHeaders = HttpHeaders.builder();
            defaultHeaders.addAll(headers);
            return this;
        }

        public Builder bearerToken(String value) {
            return defaultHeader(HttpHeaderNames.AUTHORIZATION,
                    "Bearer " + ValidationUtils.requireNonNull(value, "token must not be null"));
        }

        public Builder jsonCodec(JsonCodec value) {
            jsonCodec = ValidationUtils.requireNonNull(value, "jsonCodec must not be null");
            return this;
        }

        public Builder connectTimeout(Duration value) {
            connectTimeout = requireTimeout(value, "connectTimeout");
            return this;
        }

        public Builder readTimeout(Duration value) {
            readTimeout = requireTimeout(value, "readTimeout");
            return this;
        }

        public Builder writeTimeout(Duration value) {
            writeTimeout = requireTimeout(value, "writeTimeout");
            return this;
        }

        public Builder callTimeout(Duration value) {
            callTimeout = requireTimeout(value, "callTimeout");
            return this;
        }

        public Builder sseIdleTimeout(Duration value) {
            sseIdleTimeout = requireTimeout(value, "sseIdleTimeout");
            return this;
        }

        public AiClient build() {
            if (httpClient != null) {
                return new AiClient(httpClient, false, jsonCodec, sseIdleTimeout);
            }
            HttpClient.Builder builder = HttpClient.builder()
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .writeTimeout(writeTimeout)
                    .callTimeout(callTimeout)
                    .sseIdleTimeout(sseIdleTimeout)
                    .jsonCodec(jsonCodec);
            if (baseUrl != null) {
                builder.baseUrl(baseUrl);
            }
            HttpHeaders headers = defaultHeaders.build();
            for (int index = 0; index < headers.size(); index++) {
                builder.defaultHeader(headers.name(index), headers.value(index));
            }
            return new AiClient(builder.build(), true, jsonCodec, sseIdleTimeout);
        }

        private static Duration requireTimeout(Duration value, String name) {
            ValidationUtils.requireNonNull(value, name + " must be non-negative");
            if (value.isNegative()) {
                throw new IllegalArgumentException(name + " must be non-negative");
            }
            return value;
        }
    }
}
