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

package com.zhengshuyun.lava.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.zhengshuyun.lava.core.lang.Validate;
import com.zhengshuyun.lava.core.time.DateTimePatterns;
import com.zhengshuyun.lava.core.time.ZoneIds;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * JSON 序列化配置构建器
 *
 * <p>默认配置 (适合通用库/框架场景):
 * <ul>
 *   <li>日期时间格式: ISO 8601 UTC 时间 ({@code yyyy-MM-dd'T'HH:mm:ss'Z'})</li>
 *   <li>日期格式: ISO 8601 ({@code yyyy-MM-dd})</li>
 *   <li>时间格式: ISO 8601 ({@code HH:mm:ss})</li>
 *   <li>时区: UTC</li>
 *   <li>地区: Locale.ROOT (无地区特定格式)</li>
 *   <li>Long 类型: JS 安全序列化 (超出范围转字符串)</li>
 * </ul>
 *
 * <p>日期时间类型序列化示例:
 * <ul>
 *   <li>{@code Date}: {@code "2026-01-01T12:30:00Z"} (UTC 时间, 带 Z 后缀)</li>
 *   <li>{@code Instant}: {@code "2026-01-01T12:30:00Z"} (UTC 时间, 带 Z 后缀)</li>
 *   <li>{@code LocalDateTime}: {@code "2026-01-01T12:30:00Z"} (作为 UTC 时间, 带 Z 后缀)</li>
 *   <li>{@code LocalDate}: {@code "2026-01-01"}</li>
 *   <li>{@code LocalTime}: {@code "12:30:00"}</li>
 * </ul>
 *
 * <p><strong>迁移指南 (保持旧格式)</strong>:
 * <pre>{@code
 * ObjectMapper mapper = new JsonBuilder()
 *     .setDateTimeFormat(DateTimePatterns.DATE_TIME)  // "yyyy-MM-dd HH:mm:ss"
 *     .setZone(ZoneIds.ASIA_SHANGHAI)
 *     .setLocale(Locale.CHINA)
 *     .build();
 * }</pre>
 *
 * @author Toint
 * @since 2025/12/29
 */
public final class JsonBuilder {
    /**
     * 日期时间格式, 默认{@link DateTimePatterns#ISO_INSTANT}
     */
    private String dateTimeFormat = DateTimePatterns.ISO_INSTANT;

    /**
     * 日期格式, 默认{@link DateTimePatterns#DATE}
     */
    private String dateFormat = DateTimePatterns.DATE;

    /**
     * 时间格式, 默认{@link DateTimePatterns#TIME}
     */
    private String timeFormat = DateTimePatterns.TIME;

    /**
     * 时区, 默认{@link ZoneIds#UTC}
     */
    private ZoneId zone = ZoneIds.UTC;

    /**
     * 地区, 默认{@link Locale#ROOT}
     */
    private Locale locale = Locale.ROOT;

    /**
     * Long值JS安全序列化
     */
    @Nullable
    private SafeLongModule safeLongModule = new SafeLongModule();

    /**
     * 自定义
     */
    @Nullable
    private Consumer<JsonMapper.Builder> customizer;

    public JsonBuilder setDateTimeFormat(String val) {
        dateTimeFormat = val;
        return this;
    }

    public JsonBuilder setDateFormat(String val) {
        dateFormat = val;
        return this;
    }

    public JsonBuilder setTimeFormat(String val) {
        timeFormat = val;
        return this;
    }

    public JsonBuilder setZone(ZoneId val) {
        zone = val;
        return this;
    }

    public JsonBuilder setLocale(Locale val) {
        locale = val;
        return this;
    }

    public JsonBuilder setSafeLongModule(@Nullable SafeLongModule val) {
        safeLongModule = val;
        return this;
    }

    public JsonBuilder setCustomizer(@Nullable Consumer<JsonMapper.Builder> val) {
        customizer = val;
        return this;
    }

    public ObjectMapper build() {
        Validate.notNull(locale, "Locale must not be null");
        Validate.notNull(zone, "ZoneId must not be null");
        Validate.notBlank(dateTimeFormat, "dateTimeFormat must not be blank");

        TimeZone timeZone = TimeZone.getTimeZone(zone);

        // 使用 Builder 模式创建 ObjectMapper
        JsonMapper.Builder builder = JsonMapper.builder()
                // 禁用将日期类型序列化为数字时间戳
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 反序列化时忽略未知字段
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 序列化空对象时不抛异常
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 设置时区和地区
                .defaultTimeZone(timeZone)
                .defaultLocale(locale)
                // 添加 Date ISO 8601 序列化支持
                .addModule(new IsoDateModule(zone))
                // 添加Java8时间模块支持
                .addModule(createJavaTimeModule());

        // 安全Long, 解决JS精度丢失问题
        if (safeLongModule != null) {
            builder.addModule(safeLongModule);
        }

        // 自定义
        if (customizer != null) {
            customizer.accept(builder);
        }

        return builder.build();
    }

    /**
     * 创建Java8(Jsr310)时间模块支持
     *
     * <p>包含{@link LocalDateTime}, {@link LocalDate}, {@link LocalTime}</p>
     */
    private JavaTimeModule createJavaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();

        // LocalDateTime
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat, locale);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        // LocalDate
        Validate.notBlank(dateFormat, "dateFormat must not be blank");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat, locale);
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        // LocalTime
        Validate.notBlank(timeFormat, "timeFormat must not be blank");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat, locale);
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        return module;
    }
}
