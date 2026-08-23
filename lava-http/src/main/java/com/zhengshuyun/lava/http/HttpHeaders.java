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

import java.util.*;

/**
 * 不可变且保持插入顺序的 HTTP 请求头。
 *
 * <p>请求头名称按大小写不敏感规则查找；名称和值都会在到达传输层前校验。
 * 请求头值有意只接受可见 ASCII 字符和水平制表符；这可拒绝 CR/LF 注入、控制字符
 * 以及含义不明确的非 ASCII 线缆编码。</p>
 */
public final class HttpHeaders {

    private static final HttpHeaders EMPTY = new HttpHeaders(List.of());

    /**
     * 交替存放的名称和值条目。
     */
    private final List<String> namesAndValues;

    private HttpHeaders(List<String> namesAndValues) {
        this.namesAndValues = List.copyOf(namesAndValues);
    }

    public static HttpHeaders of() {
        return EMPTY;
    }

    public static HttpHeaders of(String... namesAndValues) {
        ValidationUtils.requireNonNull(namesAndValues, "namesAndValues must not be null");
        if ((namesAndValues.length & 1) != 0) {
            throw new IllegalArgumentException("namesAndValues must contain name/value pairs");
        }
        Builder builder = builder();
        for (int index = 0; index < namesAndValues.length; index += 2) {
            builder.add(namesAndValues[index], namesAndValues[index + 1]);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public @Nullable String get(String name) {
        requireName(name);
        for (int index = namesAndValues.size() - 2; index >= 0; index -= 2) {
            if (namesAndValues.get(index).equalsIgnoreCase(name)) {
                return namesAndValues.get(index + 1);
            }
        }
        return null;
    }

    public List<String> values(String name) {
        requireName(name);
        List<String> result = new ArrayList<>();
        for (int index = 0; index < namesAndValues.size(); index += 2) {
            if (namesAndValues.get(index).equalsIgnoreCase(name)) {
                result.add(namesAndValues.get(index + 1));
            }
        }
        return List.copyOf(result);
    }

    public boolean contains(String name) {
        return get(name) != null;
    }

    public Set<String> names() {
        Set<String> canonical = new LinkedHashSet<>();
        Set<String> result = new LinkedHashSet<>();
        for (int index = 0; index < namesAndValues.size(); index += 2) {
            String name = namesAndValues.get(index);
            if (canonical.add(name.toLowerCase(Locale.ROOT))) {
                result.add(name);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public int size() {
        return namesAndValues.size() / 2;
    }

    public boolean isEmpty() {
        return namesAndValues.isEmpty();
    }

    public String name(int index) {
        checkIndex(index);
        return namesAndValues.get(index * 2);
    }

    public String value(int index) {
        checkIndex(index);
        return namesAndValues.get(index * 2 + 1);
    }

    /**
     * 返回适用于元数据和诊断的安全快照。
     */
    public HttpHeaders redacted() {
        if (isEmpty()) {
            return this;
        }
        Builder builder = builder();
        for (int index = 0; index < size(); index++) {
            String name = name(index);
            builder.add(name, HttpRedactionUtils.redactHeaderValue(name, value(index)));
        }
        return builder.build();
    }

    static HttpHeaders fromOkHttp(okhttp3.Headers headers) {
        Builder builder = builder();
        for (int index = 0; index < headers.size(); index++) {
            builder.add(headers.name(index), headers.value(index));
        }
        return builder.build();
    }

    okhttp3.Headers toOkHttp() {
        okhttp3.Headers.Builder builder = new okhttp3.Headers.Builder();
        for (int index = 0; index < size(); index++) {
            // 值在插入时已校验，因此使用常规的安全 OkHttp API 即可。
            builder.add(name(index), value(index));
        }
        return builder.build();
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    private static void requireName(@Nullable String name) {
        ValidationUtils.requireNonNull(name, "header name must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("header name must not be empty");
        }
        for (int index = 0; index < name.length(); index++) {
            char c = name.charAt(index);
            if (!isTokenCharacter(c)) {
                throw new IllegalArgumentException("invalid HTTP header name");
            }
        }
    }

    private static void requireValue(@Nullable String value) {
        ValidationUtils.requireNonNull(value, "header value must not be null");
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (c != '\t' && (c < 0x20 || c > 0x7e)) {
                throw new IllegalArgumentException("invalid HTTP header value");
            }
        }
    }

    private static boolean isTokenCharacter(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return object instanceof HttpHeaders other && namesAndValues.equals(other.namesAndValues);
    }

    @Override
    public int hashCode() {
        return namesAndValues.hashCode();
    }

    /**
     * 敏感值始终会被脱敏。
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < size(); index++) {
            String name = name(index);
            result.append(name).append(": ")
                    .append(HttpRedactionUtils.redactHeaderValue(name, value(index)))
                    .append('\n');
        }
        return result.toString();
    }

    public static final class Builder {
        private final List<String> namesAndValues = new ArrayList<>();

        private Builder() {
        }

        public Builder add(String name, String value) {
            requireName(name);
            requireValue(value);
            namesAndValues.add(name);
            namesAndValues.add(value);
            return this;
        }

        public Builder set(String name, String value) {
            requireName(name);
            requireValue(value);
            remove(name);
            return add(name, value);
        }

        public Builder remove(String name) {
            requireName(name);
            for (int index = namesAndValues.size() - 2; index >= 0; index -= 2) {
                if (namesAndValues.get(index).equalsIgnoreCase(name)) {
                    namesAndValues.remove(index + 1);
                    namesAndValues.remove(index);
                }
            }
            return this;
        }

        public Builder addAll(Map<String, String> headers) {
            ValidationUtils.requireNonNull(headers, "headers must not be null");
            headers.forEach(this::add);
            return this;
        }

        public Builder addAll(HttpHeaders headers) {
            ValidationUtils.requireNonNull(headers, "headers must not be null");
            for (int index = 0; index < headers.size(); index++) {
                add(headers.name(index), headers.value(index));
            }
            return this;
        }

        public HttpHeaders build() {
            return namesAndValues.isEmpty() ? EMPTY : new HttpHeaders(namesAndValues);
        }
    }
}
