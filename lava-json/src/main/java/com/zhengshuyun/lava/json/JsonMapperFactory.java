/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import java.util.TimeZone;
import java.util.function.Consumer;

/** 使用确定性的 Lava 默认配置创建不可变 Jackson 3 mapper。 */
public final class JsonMapperFactory {

    private JsonMapperFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 使用 Jackson 原生 JSON 形态和 {@link Locale#ROOT} 创建 mapper。不注册全局自定义序列化器，
     * 因而 long 值保持为 JSON number。
     */
    public static ObjectMapper defaultMapper() {
        return builder().build();
    }

    /**
     * 创建 mapper 配置构建器。
     *
     * @return 新的构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** 一次性 builder；{@link #build()} 返回的 mapper 不可变且线程安全。 */
    public static final class Builder {

        /** {@link LocalDateTime} 的可选格式模式。 */
        private @Nullable String dateTimePattern;

        /** {@link LocalDate} 的可选格式模式。 */
        private @Nullable String datePattern;

        /** {@link LocalTime} 的可选格式模式。 */
        private @Nullable String timePattern;

        /** 绝对时间类型使用的可选默认时区。 */
        private @Nullable ZoneId zone;

        /** 日期时间格式化使用的区域设置，默认为根区域。 */
        private Locale locale = Locale.ROOT;

        /** 按注册顺序加入 mapper 的 Jackson 模块。 */
        private final List<JacksonModule> modules = new ArrayList<>();

        /** 对底层 Jackson Builder 的可选自定义配置回调。 */
        private @Nullable Consumer<JsonMapper.Builder> customizer;

        private Builder() {
        }

        /**
         * 设置 {@link LocalDateTime} 的序列化和反序列化格式。
         *
         * @param pattern 非空白的日期时间格式
         * @return 当前构建器
         */
        public Builder localDateTimePattern(String pattern) {
            dateTimePattern = requirePattern(pattern, "pattern");
            return this;
        }

        /**
         * 设置 {@link LocalDate} 的序列化和反序列化格式。
         *
         * @param pattern 非空白的日期格式
         * @return 当前构建器
         */
        public Builder localDatePattern(String pattern) {
            datePattern = requirePattern(pattern, "pattern");
            return this;
        }

        /**
         * 设置 {@link LocalTime} 的序列化和反序列化格式。
         *
         * @param pattern 非空白的时间格式
         * @return 当前构建器
         */
        public Builder localTimePattern(String pattern) {
            timePattern = requirePattern(pattern, "pattern");
            return this;
        }

        /**
         * 设置绝对时间类型的默认时区；本地日期时间值不带时区。
         *
         * @param zone 默认时区
         * @return 当前构建器
         */
        public Builder zone(ZoneId zone) {
            this.zone = ValidationUtils.requireNonNull(zone, "zone");
            return this;
        }

        /**
         * 设置格式化和解析使用的区域设置。
         *
         * @param locale 区域设置
         * @return 当前构建器
         */
        public Builder locale(Locale locale) {
            this.locale = ValidationUtils.requireNonNull(locale, "locale");
            return this;
        }

        /**
         * 注册一个 Jackson 模块。
         *
         * @param module 待注册的模块
         * @return 当前构建器
         */
        public Builder addModule(JacksonModule module) {
            modules.add(ValidationUtils.requireNonNull(module, "module"));
            return this;
        }

        /**
         * 配置此工厂未建模的 Jackson 能力。
         *
         * @param customizer 对底层 Jackson 构建器的配置回调
         * @return 当前构建器
         */
        public Builder customize(Consumer<JsonMapper.Builder> customizer) {
            this.customizer = ValidationUtils.requireNonNull(customizer, "customizer");
            return this;
        }

        /**
         * 根据当前配置创建不可变且线程安全的 mapper。
         *
         * @return 新的 mapper
         */
        public ObjectMapper build() {
            JsonMapper.Builder mapper = JsonMapper.builder().defaultLocale(locale);
            applyPattern(mapper, LocalDateTime.class, dateTimePattern);
            applyPattern(mapper, LocalDate.class, datePattern);
            applyPattern(mapper, LocalTime.class, timePattern);
            if (zone != null) {
                mapper.defaultTimeZone(TimeZone.getTimeZone(zone));
            }
            mapper.addModules(List.copyOf(modules));
            if (customizer != null) {
                customizer.accept(mapper);
            }
            return mapper.build();
        }

        private static String requirePattern(String pattern, String name) {
            return ValidationUtils.requireNotBlank(pattern, name + " must not be blank");
        }

        private static void applyPattern(
                JsonMapper.Builder mapper, Class<?> type, @Nullable String pattern) {
            if (pattern != null) {
                mapper.withConfigOverride(
                        type,
                        override -> override.setFormat(JsonFormat.Value.forPattern(pattern)));
            }
        }
    }
}
