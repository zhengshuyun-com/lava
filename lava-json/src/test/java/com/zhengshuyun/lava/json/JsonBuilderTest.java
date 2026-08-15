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
import com.zhengshuyun.lava.core.time.DateTimePatterns;
import com.zhengshuyun.lava.core.time.ZoneIds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonBuilderTest {

    private static final Instant INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    // 默认行为与裸 Jackson 一致

    @DisplayName("默认配置下时间类型输出与裸 Jackson 3 完全一致")
    @Test
    void testDefaultsMatchBareJackson() {
        ObjectMapper lava = JsonUtil.builder().build();
        ObjectMapper bare = JsonMapper.builder().build();

        Object[] values = {
                INSTANT,
                Date.from(INSTANT),
                new java.sql.Timestamp(INSTANT.toEpochMilli()),
                java.sql.Date.valueOf("2026-01-01"),
                java.sql.Time.valueOf("12:30:00"),
                LocalDateTime.of(2026, 1, 1, 12, 30),
                LocalDate.of(2026, 1, 1),
                LocalTime.of(12, 30),
                INSTANT.atZone(ZoneIds.ASIA_SHANGHAI).toOffsetDateTime(),
                INSTANT.atZone(ZoneIds.ASIA_SHANGHAI),
        };

        for (Object value : values) {
            assertEquals(bare.writeValueAsString(value), lava.writeValueAsString(value),
                    "偏离裸 Jackson 默认值: " + value.getClass().getName());
        }
    }

    @DisplayName("默认配置下绝对时刻输出 ISO-8601 UTC")
    @Test
    void testDefaultAbsoluteOutput() {
        ObjectMapper mapper = JsonUtil.builder().build();

        assertEquals("{\"t\":\"2026-01-01T00:00:00Z\"}", mapper.writeValueAsString(Map.of("t", INSTANT)));
        assertEquals("{\"t\":\"2026-01-01T00:00:00.000Z\"}",
                mapper.writeValueAsString(Map.of("t", Date.from(INSTANT))));
    }

    @DisplayName("默认配置下本地时间输出 ISO-8601, 不带时区后缀")
    @Test
    void testDefaultLocalOutput() {
        ObjectMapper mapper = JsonUtil.builder().build();

        assertEquals("{\"t\":\"2026-01-01T12:30:00\"}",
                mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 12, 30))));
        assertEquals("{\"t\":\"2026-01-01\"}",
                mapper.writeValueAsString(Map.of("t", LocalDate.of(2026, 1, 1))));
        assertEquals("{\"t\":\"12:30:00\"}",
                mapper.writeValueAsString(Map.of("t", LocalTime.of(12, 30))));
    }

    @DisplayName("默认配置下 OffsetDateTime 保留原有偏移量")
    @Test
    void testDefaultKeepsOffset() {
        ObjectMapper mapper = JsonUtil.builder().build();

        assertEquals("{\"t\":\"2026-01-01T08:00:00+08:00\"}",
                mapper.writeValueAsString(Map.of("t", INSTANT.atZone(ZoneIds.ASIA_SHANGHAI).toOffsetDateTime())));
    }

    @DisplayName("默认配置下亚秒精度按实际值输出")
    @Test
    void testDefaultSubSecondPrecision() {
        ObjectMapper mapper = JsonUtil.builder().build();

        assertEquals("{\"t\":\"2026-01-01T00:00:00.123Z\"}",
                mapper.writeValueAsString(Map.of("t", INSTANT.plusMillis(123))));
        assertEquals("{\"t\":\"2026-01-01T12:30:00.123\"}",
                mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 12, 30, 0, 123_000_000))));
    }

    // 注解优先级: 框架不能抢走使用者的控制权

    record AnnotatedInstant(@JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss", timezone = "Asia/Shanghai") Instant t) {
    }

    record AnnotatedLocalDateTime(@JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss") LocalDateTime t) {
    }

    @DisplayName("@JsonFormat 在绝对时刻上生效")
    @Test
    void testJsonFormatOnAbsolute() {
        ObjectMapper mapper = JsonUtil.builder().build();

        assertEquals("{\"t\":\"2026/01/01 08:00:00\"}",
                mapper.writeValueAsString(new AnnotatedInstant(INSTANT)));
    }

    @DisplayName("@JsonFormat 优先于全局格式配置")
    @Test
    void testJsonFormatOverridesGlobalConfig() {
        ObjectMapper mapper = JsonUtil.builder()
                .setDateTimeFormat(DateTimePatterns.DATE_TIME)
                .build();

        // 字段注解优先, 不受全局 yyyy-MM-dd HH:mm:ss 影响
        assertEquals("{\"t\":\"2026/01/01 12:30:00\"}",
                mapper.writeValueAsString(new AnnotatedLocalDateTime(LocalDateTime.of(2026, 1, 1, 12, 30))));
    }

    @DisplayName("全局格式等价于在类型上标注 @JsonFormat")
    @Test
    void testGlobalFormatEquivalentToTypeAnnotation() {
        ObjectMapper lava = JsonUtil.builder().setDateTimeFormat(DateTimePatterns.DATE_TIME).build();
        ObjectMapper viaOverride = JsonMapper.builder()
                .defaultLocale(Locale.ROOT)
                .withConfigOverride(LocalDateTime.class,
                        c -> c.setFormat(JsonFormat.Value.forPattern(DateTimePatterns.DATE_TIME)))
                .build();

        LocalDateTime value = LocalDateTime.of(2026, 1, 1, 12, 30);
        assertEquals(viaOverride.writeValueAsString(value), lava.writeValueAsString(value));
    }

    // 显式配置才覆盖

    @DisplayName("显式配置本地时间格式后生效")
    @Test
    void testExplicitLocalFormats() {
        ObjectMapper mapper = JsonUtil.builder()
                .setDateTimeFormat(DateTimePatterns.DATE_TIME)
                .setDateFormat(DateTimePatterns.DATE_SLASH)
                .setTimeFormat("HH-mm-ss")
                .build();

        assertEquals("{\"t\":\"2026-01-01 12:30:00\"}",
                mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 12, 30))));
        assertEquals("{\"t\":\"2026/01/01\"}",
                mapper.writeValueAsString(Map.of("t", LocalDate.of(2026, 1, 1))));
        // 用与默认值不同的格式, 确认配置确实生效而不是碰巧等于默认输出
        assertEquals("{\"t\":\"12-30-00\"}",
                mapper.writeValueAsString(Map.of("t", LocalTime.of(12, 30))));
    }

    @DisplayName("本地时间格式不影响绝对时刻")
    @Test
    void testLocalFormatDoesNotAffectAbsolute() {
        ObjectMapper mapper = JsonUtil.builder()
                .setDateTimeFormat(DateTimePatterns.DATE_TIME_CHINESE)
                .build();

        assertEquals("{\"t\":\"2026-01-01T00:00:00Z\"}", mapper.writeValueAsString(Map.of("t", INSTANT)));
    }

    @DisplayName("显式配置时区后作用于 Date")
    @Test
    void testExplicitZone() {
        ObjectMapper mapper = JsonUtil.builder().setZone(ZoneIds.ASIA_SHANGHAI).build();

        assertEquals("{\"t\":\"2026-01-01T08:00:00.000+08:00\"}",
                mapper.writeValueAsString(Map.of("t", Date.from(INSTANT))));
    }

    @DisplayName("显式配置时区不影响本地时间")
    @Test
    void testExplicitZoneDoesNotAffectLocal() {
        ObjectMapper mapper = JsonUtil.builder().setZone(ZoneIds.ASIA_SHANGHAI).build();

        assertEquals("{\"t\":\"2026-01-01T12:30:00\"}",
                mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 12, 30))));
    }

    // Locale 默认收敛到 ROOT

    @DisplayName("默认 Locale 为 ROOT, 不随 JVM 默认地区变化")
    @Test
    void testLocaleDefaultsToRoot() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.CHINA);
            ObjectMapper mapper = JsonUtil.builder().setDateTimeFormat("yyyy MMMM dd").build();
            String withChinaDefault = mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 0, 0)));

            Locale.setDefault(Locale.US);
            mapper = JsonUtil.builder().setDateTimeFormat("yyyy MMMM dd").build();
            String withUsDefault = mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 0, 0)));

            assertEquals(withChinaDefault, withUsDefault);
            // 不只是两者相等, 还要确认取的是 ROOT 的结果
            assertEquals("{\"t\":\"2026 Jan 01\"}", withChinaDefault);
        } finally {
            Locale.setDefault(original);
        }
    }

    @DisplayName("显式配置 Locale 后生效")
    @Test
    void testExplicitLocale() {
        ObjectMapper mapper = JsonUtil.builder()
                .setDateTimeFormat("yyyy MMMM dd")
                .setLocale(Locale.US)
                .build();

        assertEquals("{\"t\":\"2026 January 01\"}",
                mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 0, 0))));
    }

    // java.sql 类型完全交给 Jackson

    record AnnotatedSqlDate(@JsonFormat(pattern = "yyyy/MM/dd", timezone = "UTC") java.sql.Date t) {
    }

    @DisplayName("java.sql.Date 不做干预, @JsonFormat 保持生效")
    @Test
    void testSqlDateLeftToJackson() {
        ObjectMapper lava = JsonUtil.builder().build();
        ObjectMapper bare = JsonMapper.builder().build();
        java.sql.Date value = java.sql.Date.valueOf("2026-01-01");

        assertEquals(bare.writeValueAsString(value), lava.writeValueAsString(value));
        assertEquals(bare.writeValueAsString(new AnnotatedSqlDate(value)),
                lava.writeValueAsString(new AnnotatedSqlDate(value)));
    }

    @DisplayName("本地时间格式配置不影响 java.sql 类型")
    @Test
    void testSqlTypesUnaffectedByLocalFormats() {
        ObjectMapper lava = JsonUtil.builder()
                .setDateFormat(DateTimePatterns.DATE_COMPACT)
                .setTimeFormat("HH-mm-ss")
                .build();
        ObjectMapper bare = JsonMapper.builder().build();

        assertEquals(bare.writeValueAsString(java.sql.Date.valueOf("2026-01-01")),
                lava.writeValueAsString(java.sql.Date.valueOf("2026-01-01")));
        assertEquals(bare.writeValueAsString(java.sql.Time.valueOf("12:30:00")),
                lava.writeValueAsString(java.sql.Time.valueOf("12:30:00")));
    }

    // 参数校验与失败行为

    @DisplayName("空白或 null 格式在 setter 处立即报错")
    @Test
    void testBlankPatternRejected() {
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.builder().setDateTimeFormat("  "));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.builder().setDateFormat(""));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.builder().setTimeFormat(null));
    }

    @DisplayName("带偏移量的格式用在 LocalDateTime 上会抛异常")
    @Test
    void testOffsetPatternOnLocalDateTimeFails() {
        ObjectMapper mapper = JsonUtil.builder()
                .setDateTimeFormat(DateTimePatterns.ISO_OFFSET_DATE_TIME)
                .build();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(Map.of("t", LocalDateTime.of(2026, 1, 1, 12, 30))));
        assertInstanceOf(UnsupportedTemporalTypeException.class, e.getCause());
    }

    // 往返

    @DisplayName("绝对时刻可往返, 且接受带偏移量的输入")
    @Test
    void testAbsoluteRoundTrip() {
        ObjectMapper mapper = JsonUtil.builder().build();

        assertEquals(INSTANT, mapper.readValue(mapper.writeValueAsString(INSTANT), Instant.class));
        assertEquals(INSTANT,
                mapper.readValue(mapper.writeValueAsString(Date.from(INSTANT)), Date.class).toInstant());
        assertEquals(INSTANT, mapper.readValue("\"2026-01-01T08:00:00+08:00\"", Instant.class));
        assertEquals(INSTANT,
                mapper.readValue("\"2026-01-01T08:00:00+08:00\"", OffsetDateTime.class).toInstant());
    }

    @DisplayName("本地时间可往返, 显式格式下同样可往返")
    @Test
    void testLocalRoundTrip() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 1, 1, 12, 30);

        ObjectMapper mapper = JsonUtil.builder().build();
        assertEquals(dateTime, mapper.readValue(mapper.writeValueAsString(dateTime), LocalDateTime.class));

        ObjectMapper custom = JsonUtil.builder().setDateTimeFormat(DateTimePatterns.DATE_TIME).build();
        assertEquals(dateTime, custom.readValue(custom.writeValueAsString(dateTime), LocalDateTime.class));
    }
}
