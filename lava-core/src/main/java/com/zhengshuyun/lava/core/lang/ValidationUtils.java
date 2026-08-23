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

package com.zhengshuyun.lava.core.lang;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 参数校验工具，校验失败时抛出 {@link IllegalArgumentException}。
 */
public final class ValidationUtils {

    private static final String MUST_BE_TRUE = "The validated condition must be true";
    private static final String MUST_BE_FALSE = "The validated condition must be false";
    private static final String MUST_NOT_BE_NULL = "The validated value must not be null";
    private static final String MUST_NOT_BE_BLANK = "The validated value must not be blank";
    private static final String MUST_NOT_BE_EMPTY = "The validated value must not be empty";

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 要求条件为 true，否则使用默认消息抛出异常。
     *
     * @param condition 待校验的条件
     */
    public static void requireTrue(boolean condition) {
        requireTrue(condition, MUST_BE_TRUE);
    }

    /**
     * 要求条件为 true，否则抛出指定消息的异常。
     *
     * @param condition 待校验的条件
     * @param message   校验失败时的异常消息
     */
    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 要求条件为 false，否则使用默认消息抛出异常。
     *
     * @param condition 待校验的条件
     */
    public static void requireFalse(boolean condition) {
        requireFalse(condition, MUST_BE_FALSE);
    }

    /**
     * 要求条件为 false，否则抛出指定消息的异常。
     *
     * @param condition 待校验的条件
     * @param message   校验失败时的异常消息
     */
    public static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 要求值不为 null，并使用默认消息返回原值。
     *
     * @param value 待校验的值
     * @param <T>   值类型
     * @return 非 null 的原值
     */
    public static <T> T requireNonNull(@Nullable T value) {
        return requireNonNull(value, MUST_NOT_BE_NULL);
    }

    /**
     * 要求值不为 null，并使用指定消息返回原值。
     *
     * @param value   待校验的值
     * @param message 校验失败时的异常消息
     * @param <T>     值类型
     * @return 非 null 的原值
     */
    public static <T> T requireNonNull(@Nullable T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 要求字符串不为 null、空字符串或纯空白，并使用默认消息返回原字符串。
     *
     * @param value 待校验的字符串
     * @return 非空白的原字符串
     */
    public static String requireNotBlank(@Nullable String value) {
        return requireNotBlank(value, MUST_NOT_BE_BLANK);
    }

    /**
     * 要求字符串不为 null、空字符串或纯空白，并使用指定消息返回原字符串。
     *
     * @param value   待校验的字符串
     * @param message 校验失败时的异常消息
     * @return 非空白的原字符串
     */
    public static String requireNotBlank(@Nullable String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 要求集合不为 null 且不为空，并使用默认消息返回原集合。
     *
     * @param value 待校验的集合
     * @param <T>   集合类型
     * @return 非空的原集合
     */
    public static <T extends Collection<?>> T requireNotEmpty(@Nullable T value) {
        return requireNotEmpty(value, MUST_NOT_BE_EMPTY);
    }

    /**
     * 要求集合不为 null 且不为空，并使用指定消息返回原集合。
     *
     * @param value   待校验的集合
     * @param message 校验失败时的异常消息
     * @param <T>     集合类型
     * @return 非空的原集合
     */
    public static <T extends Collection<?>> T requireNotEmpty(@Nullable T value, String message) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * 要求 Map 不为 null 且不为空，并使用默认消息返回原 Map。
     *
     * @param value 待校验的 Map
     * @param <T>   Map 类型
     * @return 非空的原 Map
     */
    public static <T extends Map<?, ?>> T requireNotEmpty(@Nullable T value) {
        return requireNotEmpty(value, MUST_NOT_BE_EMPTY);
    }

    /**
     * 要求 Map 不为 null 且不为空，并使用指定消息返回原 Map。
     *
     * @param value   待校验的 Map
     * @param message 校验失败时的异常消息
     * @param <T>     Map 类型
     * @return 非空的原 Map
     */
    public static <T extends Map<?, ?>> T requireNotEmpty(@Nullable T value, String message) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
