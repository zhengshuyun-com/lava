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

/**
 * 结构化且凭证安全的 SSE 失败上下文。
 *
 * @param kind         稳定的失败分类
 * @param throwable    原始异常，仅供内部分类使用
 * @param statusCode   服务端已响应时的 HTTP 状态码
 * @param headers      服务端已响应时的响应头
 * @param responseBody 有界失败响应正文
 */
record HttpSseFailure(
        HttpFailureKind kind,
        @Nullable Throwable throwable,
        @Nullable Integer statusCode,
        @Nullable HttpHeaders headers,
        @Nullable String responseBody) {

    public HttpSseFailure {
        ValidationUtils.requireNonNull(kind, "kind must not be null");
    }

    /**
     * 返回不泄露原始异常消息和响应正文的调试表示。
     *
     * @return 已脱敏的失败摘要
     */
    @Override
    public String toString() {
        return "HttpSseFailure[kind=" + kind
                + ", throwable=" + (throwable == null ? null : throwable.getClass().getName())
                + ", statusCode=" + statusCode
                + ", headers=" + (headers == null ? null : headers.redacted())
                + ", responseBody=" + (responseBody == null ? null : "[REDACTED]") + ']';
    }
}
