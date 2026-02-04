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

package com.zhengshuyun.common.core.lang;

import com.google.common.base.Strings;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 校验工具, 未通过校验统一抛出{@link IllegalArgumentException}
 *
 * @author Toint
 * @since 2026/1/1
 */
public final class Validate {

    private static final String DEFAULT_IS_FALSE_MESSAGE = "The validated value must be false";
    private static final String DEFAULT_IS_TRUE_MESSAGE = "The validated value must be true";
    private static final String DEFAULT_NOT_BLANK_MESSAGE = "The validated value must not be blank";
    private static final String DEFAULT_NOT_NULL_MESSAGE = "The validated value must not be null";
    private static final String DEFAULT_IS_NULL_MESSAGE = "The validated value must be null";
    private static final String DEFAULT_NOT_EMPTY_MESSAGE = "The validated value must not be empty";
    private static final String DEFAULT_INVALID_EMAIL_MESSAGE = "The validated value must be a valid email";
    private static final String DEFAULT_INVALID_MOBILE_MESSAGE = "The validated value must be a valid mobile number";

    /** 邮箱正则 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /** 中国手机号正则, 1开头, 第二位3-9, 共11位 */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /*
     * isFalse*/
    /**
     * 校验布尔值为 false
     *
     * @param b 待校验值
     * @return false
     * @throws IllegalArgumentException 校验失败
     */
    public static boolean isFalse(boolean b) {
        if (b) {
            throw new IllegalArgumentException(DEFAULT_IS_FALSE_MESSAGE);
        }
        return false;
    }

    /**
     * 校验布尔值为 false
     *
     * @param b 待校验值
     * @param errMsg 错误消息
     * @return false
     * @throws IllegalArgumentException 校验失败
     */
    public static boolean isFalse(boolean b, @Nullable Object errMsg) {
        if (b) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return false;
    }

    /**
     * 校验布尔值为 false
     *
     * @param b 待校验值
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @return false
     * @throws IllegalArgumentException 校验失败
     */
    public static boolean isFalse(
            boolean b,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (b) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return false;
    }

    /*
     * isTrue*/
    /**
     * 校验布尔值为 true
     *
     * @param b 待校验值
     * @return true
     * @throws IllegalArgumentException 校验失败
     */
    public static boolean isTrue(boolean b) {
        if (!b) {
            throw new IllegalArgumentException(DEFAULT_IS_TRUE_MESSAGE);
        }
        return true;
    }

    /**
     * 校验布尔值为 true
     *
     * @param b 待校验值
     * @param errMsg 错误消息
     * @return true
     * @throws IllegalArgumentException 校验失败
     */
    public static boolean isTrue(boolean b, @Nullable Object errMsg) {
        if (!b) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return true;
    }

    /**
     * 校验布尔值为 true
     *
     * @param b 待校验值
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @return true
     * @throws IllegalArgumentException 校验失败
     */
    public static boolean isTrue(
            boolean b,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (!b) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return true;
    }

    /*
     * notBlank*/
    /**
     * 校验字符串非空且非空白
     *
     * @param str 待校验字符串
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String notBlank(@Nullable String str) {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException(DEFAULT_NOT_BLANK_MESSAGE);
        }
        return str;
    }

    /**
     * 校验字符串非空且非空白
     *
     * @param str 待校验字符串
     * @param errMsg 错误消息
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String notBlank(@Nullable String str, @Nullable Object errMsg) {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return str;
    }

    /**
     * 校验字符串非空且非空白
     *
     * @param str 待校验字符串
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String notBlank(
            @Nullable String str,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return str;
    }

    /*
     * notNull*/
    /**
     * 校验对象非空
     *
     * @param obj 待校验对象
     * @param <T> 对象类型
     * @return 原对象
     * @throws IllegalArgumentException 校验失败
     */
    public static <T> T notNull(@Nullable T obj) {
        if (obj == null) {
            throw new IllegalArgumentException(DEFAULT_NOT_NULL_MESSAGE);
        }
        return obj;
    }

    /**
     * 校验对象非空
     *
     * @param obj 待校验对象
     * @param errMsg 错误消息
     * @param <T> 对象类型
     * @return 原对象
     * @throws IllegalArgumentException 校验失败
     */
    public static <T> T notNull(@Nullable T obj, @Nullable Object errMsg) {
        if (obj == null) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return obj;
    }

    /**
     * 校验对象非空
     *
     * @param obj 待校验对象
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @param <T> 对象类型
     * @return 原对象
     * @throws IllegalArgumentException 校验失败
     */
    public static <T> T notNull(
            @Nullable T obj,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (obj == null) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return obj;
    }

    /*
     * isNull*/
    /**
     * 校验对象为空
     *
     * @param obj 待校验对象
     * @throws IllegalArgumentException 校验失败
     */
    public static void isNull(@Nullable Object obj) {
        if (obj != null) {
            throw new IllegalArgumentException(DEFAULT_IS_NULL_MESSAGE);
        }
    }

    /**
     * 校验对象为空
     *
     * @param obj 待校验对象
     * @param errMsg 错误消息
     * @throws IllegalArgumentException 校验失败
     */
    public static void isNull(@Nullable Object obj, @Nullable Object errMsg) {
        if (obj != null) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
    }

    /**
     * 校验对象为空
     *
     * @param obj 待校验对象
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @throws IllegalArgumentException 校验失败
     */
    public static void isNull(
            @Nullable Object obj,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (obj != null) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
    }

    /*
     * notEmpty-collection*/
    /**
     * 校验集合非空
     *
     * @param collection 待校验集合
     * @param <T> 集合类型
     * @return 原集合
     * @throws IllegalArgumentException 校验失败
     */
    public static <T extends Collection<?>> T notEmpty(@Nullable T collection) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(DEFAULT_NOT_EMPTY_MESSAGE);
        }
        return collection;
    }

    /**
     * 校验集合非空
     *
     * @param collection 待校验集合
     * @param errMsg 错误消息
     * @param <T> 集合类型
     * @return 原集合
     * @throws IllegalArgumentException 校验失败
     */
    public static <T extends Collection<?>> T notEmpty(@Nullable T collection, @Nullable Object errMsg) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return collection;
    }

    /**
     * 校验集合非空
     *
     * @param collection 待校验集合
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @param <T> 集合类型
     * @return 原集合
     * @throws IllegalArgumentException 校验失败
     */
    public static <T extends Collection<?>> T notEmpty(
            @Nullable T collection,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return collection;
    }

    /*
     * notEmpty-map*/
    /**
     * 校验 Map 非空
     *
     * @param map 待校验 Map
     * @param <T> Map 类型
     * @return 原 Map
     * @throws IllegalArgumentException 校验失败
     */
    public static <T extends Map<?, ?>> T notEmpty(@Nullable T map) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(DEFAULT_NOT_EMPTY_MESSAGE);
        }
        return map;
    }

    /**
     * 校验 Map 非空
     *
     * @param map 待校验 Map
     * @param errMsg 错误消息
     * @param <T> Map 类型
     * @return 原 Map
     * @throws IllegalArgumentException 校验失败
     */
    public static <T extends Map<?, ?>> T notEmpty(@Nullable T map, @Nullable Object errMsg) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return map;
    }

    /**
     * 校验 Map 非空
     *
     * @param map 待校验 Map
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @param <T> Map 类型
     * @return 原 Map
     * @throws IllegalArgumentException 校验失败
     */
    public static <T extends Map<?, ?>> T notEmpty(
            @Nullable T map,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return map;
    }

    /*
     * isEmail*/
    /**
     * 校验邮箱格式
     *
     * @param str 待校验字符串
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String isEmail(@Nullable String str) {
        if (str == null || !EMAIL_PATTERN.matcher(str).matches()) {
            throw new IllegalArgumentException(DEFAULT_INVALID_EMAIL_MESSAGE);
        }
        return str;
    }

    /**
     * 校验邮箱格式
     *
     * @param str 待校验字符串
     * @param errMsg 错误消息
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String isEmail(@Nullable String str, @Nullable Object errMsg) {
        if (str == null || !EMAIL_PATTERN.matcher(str).matches()) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return str;
    }

    /**
     * 校验邮箱格式
     *
     * @param str 待校验字符串
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String isEmail(
            @Nullable String str,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (str == null || !EMAIL_PATTERN.matcher(str).matches()) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return str;
    }

    /*
     * isMobile*/
    /**
     * 校验中国手机号格式
     *
     * @param str 待校验字符串
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String isMobile(@Nullable String str) {
        if (str == null || !MOBILE_PATTERN.matcher(str).matches()) {
            throw new IllegalArgumentException(DEFAULT_INVALID_MOBILE_MESSAGE);
        }
        return str;
    }

    /**
     * 校验中国手机号格式
     *
     * @param str 待校验字符串
     * @param errMsg 错误消息
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String isMobile(@Nullable String str, @Nullable Object errMsg) {
        if (str == null || !MOBILE_PATTERN.matcher(str).matches()) {
            throw new IllegalArgumentException(String.valueOf(errMsg));
        }
        return str;
    }

    /**
     * 校验中国手机号格式
     *
     * @param str 待校验字符串
     * @param errMsgTemplate 错误消息模板
     * @param errMsgArgs 模板参数
     * @return 原字符串
     * @throws IllegalArgumentException 校验失败
     */
    public static String isMobile(
            @Nullable String str,
            @Nullable String errMsgTemplate,
            @Nullable Object... errMsgArgs) {
        if (str == null || !MOBILE_PATTERN.matcher(str).matches()) {
            throw new IllegalArgumentException(Strings.lenientFormat(errMsgTemplate, errMsgArgs));
        }
        return str;
    }
}
