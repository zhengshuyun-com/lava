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

import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bearer Token 提取工具。
 *
 * <p>按 Spring Security {@code DefaultBearerTokenResolver} 使用的严格格式解析
 * {@code Authorization} 请求头：认证方案名称不区分大小写，方案名与凭据之间使用一个
 * ASCII 空格，凭据使用 RFC 6750 的 {@code b64token} 字符集。</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc6750.html#section-2.1">RFC 6750
 * Section 2.1: Authorization Request Header Field</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110.html#section-11.1">RFC 9110
 * Section 11.1: Authentication Scheme</a>
 */
public final class BearerTokenUtils {

    /**
     * Spring Security 兼容的 Bearer Token 格式。
     */
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile(
            "^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * 禁止实例化工具类。
     */
    private BearerTokenUtils() {
    }

    /**
     * 从 Authorization 请求头值中提取 Bearer Token。
     *
     * @param authorization Authorization 请求头值
     * @return Bearer Token；请求头缺失、认证方案不匹配或格式不合法时返回 {@code null}
     */
    public static @Nullable String extract(@Nullable String authorization) {
        if (authorization == null) {
            return null;
        }
        Matcher matcher = AUTHORIZATION_PATTERN.matcher(authorization);
        return matcher.matches() ? matcher.group("token") : null;
    }
}
