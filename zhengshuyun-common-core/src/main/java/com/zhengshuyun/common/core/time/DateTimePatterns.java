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

package com.zhengshuyun.common.core.time;

import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式模式常量
 *
 * <p>提供常用的日期时间格式化模式字符串
 *
 * @author Toint
 * @see DateTimeFormatter
 * @since 2025/12/29
 */
public final class DateTimePatterns {

    private DateTimePatterns() {
    }

    /**
     * 标准日期: {@code yyyy-MM-dd}
     * <p>示例: 2026-01-01
     */
    public static final String DATE = "yyyy-MM-dd";

    /**
     * 标准时间: {@code HH:mm:ss}
     * <p>示例: 12:30:00
     */
    public static final String TIME = "HH:mm:ss";

    /**
     * 标准日期时间: {@code yyyy-MM-dd HH:mm:ss}
     * <p>示例: 2026-01-01 12:30:00
     */
    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * 带毫秒的日期时间: {@code yyyy-MM-dd HH:mm:ss.SSS}
     * <p>示例: 2026-01-01 12:30:00.123
     */
    public static final String DATE_TIME_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 紧凑日期: {@code yyyyMMdd}
     * <p>示例: 20260101
     */
    public static final String DATE_COMPACT = "yyyyMMdd";

    /**
     * 紧凑日期时间: {@code yyyyMMddHHmmss}
     * <p>示例: 20260101123000
     */
    public static final String DATE_TIME_COMPACT = "yyyyMMddHHmmss";

    /**
     * 中文日期: {@code yyyy年MM月dd日}
     * <p>示例: 2026年01月01日
     */
    public static final String DATE_CHINESE = "yyyy年MM月dd日";

    /**
     * 中文日期时间: {@code yyyy年MM月dd日 HH时mm分ss秒}
     * <p>示例: 2026年01月01日 00时00分00秒
     */
    public static final String DATE_TIME_CHINESE = "yyyy年MM月dd日 HH时mm分ss秒";

    /**
     * ISO 8601 本地时间: {@code yyyy-MM-dd'T'HH:mm:ss}
     * <p>示例: 2026-01-01T12:30:00
     * <p>用于不带时区的 API 交互
     */
    public static final String ISO_LOCAL_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * ISO 8601 本地时间 (带毫秒): {@code yyyy-MM-dd'T'HH:mm:ss.SSS}
     * <p>示例: 2026-01-01T12:30:00.123
     */
    public static final String ISO_LOCAL_DATE_TIME_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * ISO 8601 带时区: {@code yyyy-MM-dd'T'HH:mm:ssXXX}
     * <p>示例: 2026-01-01T12:30:00+08:00 (中国时区)
     * <p>用于需要明确时区的场景
     */
    public static final String ISO_OFFSET_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * 斜杠分隔日期: {@code yyyy/MM/dd}
     * <p>示例: 2026/01/01
     */
    public static final String DATE_SLASH = "yyyy/MM/dd";

    /**
     * 斜杠分隔日期时间: {@code yyyy/MM/dd HH:mm:ss}
     * <p>示例: 2026/01/01 12:30:00
     */
    public static final String DATE_TIME_SLASH = "yyyy/MM/dd HH:mm:ss";

    /**
     * 斜杠分隔日期时间 (带毫秒): {@code yyyy/MM/dd HH:mm:ss.SSS}
     * <p>示例: 2026/01/01 12:30:00.123
     */
    public static final String DATE_TIME_SLASH_MILLIS = "yyyy/MM/dd HH:mm:ss.SSS";
}
