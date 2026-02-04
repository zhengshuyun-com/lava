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

import com.google.common.base.Strings;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * 时间工具类
 *
 * @author Toint
 * @since 2026/01/17
 */
public class TimeUtil {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_TIME),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_TIME_MILLIS),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_TIME_SLASH),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_TIME_SLASH_MILLIS),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_TIME_COMPACT),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_TIME_CHINESE),
            DateTimeFormatter.ofPattern(DateTimePatterns.ISO_LOCAL_DATE_TIME),
            DateTimeFormatter.ofPattern(DateTimePatterns.ISO_LOCAL_DATE_TIME_MILLIS)
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_SLASH),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_COMPACT),
            DateTimeFormatter.ofPattern(DateTimePatterns.DATE_CHINESE)
    );

    /**
     * 解析字符串为 LocalDateTime, 支持多种常见格式
     * <p>
     * 支持的格式 (按优先级顺序):
     * <ul>
      *   <li>{@code yyyy-MM-dd HH:mm:ss} - 标准日期时间, 如: 2026-01-01 12:30:00</li>
      *   <li>{@code yyyy-MM-dd HH:mm:ss.SSS} - 带毫秒的标准日期时间, 如: 2026-01-01 12:30:00.123</li>
      *   <li>{@code yyyy/MM/dd HH:mm:ss} - 斜杠分隔日期时间, 如: 2026/01/01 12:30:00</li>
      *   <li>{@code yyyy/MM/dd HH:mm:ss.SSS} - 带毫秒的斜杠分隔日期时间, 如: 2026/01/01 12:30:00.123</li>
      *   <li>{@code yyyyMMddHHmmss} - 紧凑日期时间, 如: 20260101123000</li>
      *   <li>{@code yyyy年MM月dd日 HH时mm分ss秒} - 中文日期时间, 如: 2026年01月01日 12时30分00秒</li>
      *   <li>{@code yyyy-MM-dd'T'HH:mm:ss} - ISO 8601 标准本地时间, 如: 2026-01-01T12:30:00</li>
      *   <li>{@code yyyy-MM-dd'T'HH:mm:ss.SSS} - 带毫秒的 ISO 8601 标准本地时间, 如: 2026-01-01T12:30:00.123</li>
      *   <li>{@code yyyy-MM-dd} - 标准日期, 如: 2026-01-01 (时间部分默认为 00:00:00)</li>
      *   <li>{@code yyyy/MM/dd} - 斜杠分隔日期, 如: 2026/01/01 (时间部分默认为 00:00:00)</li>
      *   <li>{@code yyyyMMdd} - 紧凑日期, 如: 20260101 (时间部分默认为 00:00:00)</li>
      *   <li>{@code yyyy年MM月dd日} - 中文日期, 如: 2026年01月01日 (时间部分默认为 00:00:00)</li>
     * </ul>
     *
     * @param dateTime 日期时间字符串
     * @return LocalDateTime 对象, 解析失败或输入为空时返回 null
     */
    public static @Nullable LocalDateTime parse(@Nullable String dateTime) {
        dateTime = Strings.nullToEmpty(dateTime).trim();

        if (dateTime.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTime, formatter);
            } catch (DateTimeParseException ignore) {
            }
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateTime, formatter).atStartOfDay();
            } catch (DateTimeParseException ignore) {
            }
        }

        return null;
    }
}
