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

package com.zhengshuyun.lava.core.time;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * 可复用、不可变且严格的日期时间格式化器。
 */
public final class DateTimeFormatterUtils {

    /**
     * 严格解析和格式化 ISO 风格日期，格式为 {@code uuuu-MM-dd}。
     */
    public static final DateTimeFormatter DATE = strict("uuuu-MM-dd");

    /**
     * 严格解析和格式化 24 小时时间，格式为 {@code HH:mm:ss}。
     */
    public static final DateTimeFormatter TIME = strict("HH:mm:ss");

    /**
     * 严格解析和格式化空格分隔日期时间，格式为 {@code uuuu-MM-dd HH:mm:ss}。
     */
    public static final DateTimeFormatter DATE_TIME = strict("uuuu-MM-dd HH:mm:ss");

    /**
     * 严格解析和格式化带毫秒的日期时间，格式为 {@code uuuu-MM-dd HH:mm:ss.SSS}。
     */
    public static final DateTimeFormatter DATE_TIME_MILLIS = strict("uuuu-MM-dd HH:mm:ss.SSS");

    /**
     * 严格解析和格式化紧凑日期，格式为 {@code uuuuMMdd}。
     */
    public static final DateTimeFormatter COMPACT_DATE = strict("uuuuMMdd");

    /**
     * 严格解析和格式化紧凑日期时间，格式为 {@code uuuuMMddHHmmss}。
     */
    public static final DateTimeFormatter COMPACT_DATE_TIME = strict("uuuuMMddHHmmss");

    /**
     * 严格解析和格式化斜杠分隔日期，格式为 {@code uuuu/MM/dd}。
     */
    public static final DateTimeFormatter SLASH_DATE = strict("uuuu/MM/dd");

    /**
     * 严格解析和格式化斜杠分隔日期时间，格式为 {@code uuuu/MM/dd HH:mm:ss}。
     */
    public static final DateTimeFormatter SLASH_DATE_TIME = strict("uuuu/MM/dd HH:mm:ss");

    private DateTimeFormatterUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }
}
