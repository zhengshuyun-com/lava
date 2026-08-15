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

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.zhengshuyun.lava.core.lang.Validate;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

/**
 * JSON 序列化配置构建器
 *
 * <p>设计原则: <b>不改 Jackson 的核心行为, 只提供简化配置的入口</b>.
 * 不配置任何参数时, 行为与裸 Jackson 3 一致, 也与 Spring Boot 的默认值一致,
 * 因此 {@code @JsonFormat}, {@code @JsonSerialize} 等注解全部照常生效.
 *
 * <p>Jackson 3 的时间类型默认输出 (无需本类干预):
 * <ul>
 *   <li>{@link Instant}: {@code "2026-01-01T12:30:00Z"} (ISO-8601 UTC)</li>
 *   <li>{@link Date}, {@link java.sql.Timestamp}, {@link java.util.Calendar}: {@code "2026-01-01T12:30:00.000Z"}</li>
 *   <li>{@link java.time.OffsetDateTime}, {@link java.time.ZonedDateTime}: {@code "2026-01-01T20:30:00+08:00"} (保留原偏移量)</li>
 *   <li>{@link LocalDateTime}: {@code "2026-01-01T12:30:00"}</li>
 *   <li>{@link LocalDate}: {@code "2026-01-01"}</li>
 *   <li>{@link LocalTime}: {@code "12:30:00"}</li>
 * </ul>
 * 默认时区为 UTC. {@link Instant} 和本地时间类型按实际亚秒精度输出,
 * {@link Date} 及其子类固定输出三位毫秒并截断更高精度.
 *
 * <p>本类只在一处偏离 Jackson 默认值: {@link #setLocale(Locale)} 默认改为
 * {@link Locale#ROOT}. Jackson 取 JVM 默认地区, 会导致同一份代码在不同机器上输出不同结果.
 *
 * <p>其余配置项一律默认不生效, 只在显式调用对应 setter 后才覆盖 Jackson 行为.
 * 需要 Jackson 未暴露的能力时用 {@link #setCustomizer(Consumer)} 直接操作
 * {@link JsonMapper.Builder}.
 *
 * <p>类型选择建议: 表示"某个时刻"用 {@link Instant}, 表示"日历上的某天"用 {@link LocalDate},
 * 表示"一天中的某个钟点"用 {@link LocalTime}. {@link LocalDateTime} 不含时区,
 * 语义模糊, 不建议出现在对外接口上.
 *
 * <p>不要在对外接口上用 {@link java.sql.Date}. Jackson 按其父类 {@link Date} 当作绝对时刻
 * 处理, 会做时区换算, {@code 2026-01-01} 会输出成 {@code "2025-12-31T16:00:00.000Z"},
 * 跨时区读回还会变成前一天. 该类型请在持久层就转成 {@link LocalDate}.
 *
 * @author Toint
 * @since 2025/12/29
 */
public final class JsonBuilder {

    /**
     * {@link LocalDateTime} 格式, 默认 null 表示沿用 Jackson 的 ISO-8601 输出
     */
    @Nullable
    private String dateTimeFormat;

    /**
     * {@link LocalDate} 格式, 默认 null 表示沿用 Jackson 的 ISO-8601 输出
     */
    @Nullable
    private String dateFormat;

    /**
     * {@link LocalTime} 格式, 默认 null 表示沿用 Jackson 的 ISO-8601 输出
     */
    @Nullable
    private String timeFormat;

    /**
     * 时区, 默认 null 表示沿用 Jackson 默认值 UTC
     */
    @Nullable
    private ZoneId zone;

    /**
     * 地区, 默认 {@link Locale#ROOT}
     */
    private Locale locale = Locale.ROOT;

    /**
     * 自定义
     */
    @Nullable
    private Consumer<JsonMapper.Builder> customizer;

    /**
     * 设置 {@link LocalDateTime} 格式, 默认沿用 Jackson 的 ISO-8601 输出
     *
     * <p>只影响 {@link LocalDateTime}. {@link Instant} 和 {@link Date} 这类绝对时刻
     * 保持 Jackson 默认行为, 需要单独定制时用字段上的 {@code @JsonFormat}.
     *
     * <p>格式中不要写偏移量 ({@code XXX}, {@code Z}), {@link LocalDateTime} 不含时区信息,
     * 带偏移量的格式会在序列化时抛 {@code DatabindException},
     * cause 为 {@link java.time.temporal.UnsupportedTemporalTypeException}.
     */
    public JsonBuilder setDateTimeFormat(String val) {
        Validate.notBlank(val, "dateTimeFormat must not be blank");
        dateTimeFormat = val;
        return this;
    }

    /**
     * 设置 {@link LocalDate} 格式, 默认沿用 Jackson 的 ISO-8601 输出
     */
    public JsonBuilder setDateFormat(String val) {
        Validate.notBlank(val, "dateFormat must not be blank");
        dateFormat = val;
        return this;
    }

    /**
     * 设置 {@link LocalTime} 格式, 默认沿用 Jackson 的 ISO-8601 输出
     */
    public JsonBuilder setTimeFormat(String val) {
        Validate.notBlank(val, "timeFormat must not be blank");
        timeFormat = val;
        return this;
    }

    /**
     * 设置时区, 默认沿用 Jackson 默认值 UTC
     *
     * <p>作用于 {@link Date}, {@link java.util.Calendar} 这类绝对时刻的渲染时区.
     * 注意显式设置后, {@link java.time.OffsetDateTime} 和 {@link java.time.ZonedDateTime}
     * 会被归一到该时区而不再保留原有偏移量, 这是 Jackson 自身的行为.
     *
     * <p>本地时间类型不含时区, 不受本项影响.
     */
    public JsonBuilder setZone(ZoneId val) {
        Validate.notNull(val, "zone must not be null");
        zone = val;
        return this;
    }

    /**
     * 设置地区, 默认 {@link Locale#ROOT}
     *
     * <p>影响含文本的格式, 例如月份名和星期名
     */
    public JsonBuilder setLocale(Locale val) {
        Validate.notNull(val, "locale must not be null");
        locale = val;
        return this;
    }

    public JsonBuilder setCustomizer(@Nullable Consumer<JsonMapper.Builder> val) {
        customizer = val;
        return this;
    }

    public ObjectMapper build() {
        JsonMapper.Builder builder = JsonMapper.builder()
                // Jackson 取 JVM 默认地区, 会让同一份代码在不同机器上输出不同结果
                .defaultLocale(locale);

        // 用 Jackson 自己的 configOverride 给类型设默认格式, 等价于在该类型上标注
        // @JsonFormat. 不注册任何自定义序列化器, 字段上的 @JsonFormat 仍然优先.
        applyPattern(builder, LocalDateTime.class, dateTimeFormat);
        applyPattern(builder, LocalDate.class, dateFormat);
        applyPattern(builder, LocalTime.class, timeFormat);

        // 未显式设置时不调用, 保留 Jackson 对"时区未指定"的默认处理
        if (zone != null) {
            builder.defaultTimeZone(TimeZone.getTimeZone(zone));
        }

        // 自定义
        if (customizer != null) {
            customizer.accept(builder);
        }

        return builder.build();
    }

    private void applyPattern(JsonMapper.Builder builder, Class<?> type, @Nullable String pattern) {
        if (pattern == null) {
            return;
        }
        builder.withConfigOverride(type, config -> config.setFormat(JsonFormat.Value.forPattern(pattern)));
    }
}
