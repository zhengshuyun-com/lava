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

package com.zhengshuyun.lava.http;

import okhttp3.Headers;
import org.jspecify.annotations.Nullable;

/**
 * SSE 失败上下文.
 *
 * @param throwable    失败异常, 允许为空
 * @param statusCode   HTTP 状态码, 允许为空
 * @param headers      响应头, 允许为空
 * @param responseBody 错误响应体, 允许为空
 * @author Toint
 * @since 2026/4/18
 */
public record HttpSseFailure(
        @Nullable Throwable throwable,
        @Nullable Integer statusCode,
        @Nullable Headers headers,
        @Nullable String responseBody) {
}
