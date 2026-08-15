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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeLongModuleTest {

    private static final long UNSAFE = 9223372036854775807L;

    private static final long SAFE = 42L;

    private static final long MAX_SAFE = 9007199254740991L;

    private static ObjectMapper mapper() {
        return JsonUtil.builder()
                .setCustomizer(builder -> builder.addModule(new SafeLongModule()))
                .build();
    }

    record Boxed(Long v) {
    }

    record Primitive(long v) {
    }

    record LongArray(long[] v) {
    }

    record BoxedArray(Long[] v) {
    }

    record LongList(List<Long> v) {
    }

    record ShapeString(@JsonFormat(shape = JsonFormat.Shape.STRING) Long v) {
    }

    record ShapeNumber(@JsonFormat(shape = JsonFormat.Shape.NUMBER) Long v) {
    }

    record ShapeNumberArray(@JsonFormat(shape = JsonFormat.Shape.NUMBER) long[] v) {
    }

    // 基本规则

    @DisplayName("超出安全范围输出字符串, 范围内输出数字")
    @Test
    void testSafeRangeRule() {
        ObjectMapper mapper = mapper();

        assertEquals("{\"v\":\"9223372036854775807\"}", mapper.writeValueAsString(new Boxed(UNSAFE)));
        assertEquals("{\"v\":42}", mapper.writeValueAsString(new Boxed(SAFE)));
    }

    @DisplayName("边界值: 2^53-1 仍是数字, 2^53 转字符串, 负数对称")
    @Test
    void testBoundaries() {
        ObjectMapper mapper = mapper();

        assertEquals("{\"v\":9007199254740991}", mapper.writeValueAsString(new Boxed(MAX_SAFE)));
        assertEquals("{\"v\":\"9007199254740992\"}", mapper.writeValueAsString(new Boxed(MAX_SAFE + 1)));
        assertEquals("{\"v\":-9007199254740991}", mapper.writeValueAsString(new Boxed(-MAX_SAFE)));
        assertEquals("{\"v\":\"-9007199254740992\"}", mapper.writeValueAsString(new Boxed(-MAX_SAFE - 1)));
    }

    @DisplayName("null 仍输出 null")
    @Test
    void testNull() {
        assertEquals("{\"v\":null}", mapper().writeValueAsString(new Boxed(null)));
    }

    // 容器覆盖

    @DisplayName("primitive long 字段生效")
    @Test
    void testPrimitiveField() {
        ObjectMapper mapper = mapper();

        assertEquals("{\"v\":\"9223372036854775807\"}", mapper.writeValueAsString(new Primitive(UNSAFE)));
        assertEquals("{\"v\":42}", mapper.writeValueAsString(new Primitive(SAFE)));
    }

    @DisplayName("long[] 生效, Jackson 专用数组序列化器不会绕过规则")
    @Test
    void testPrimitiveArray() {
        ObjectMapper mapper = mapper();

        assertEquals("{\"v\":[\"9223372036854775807\",42]}",
                mapper.writeValueAsString(new LongArray(new long[]{UNSAFE, SAFE})));
        assertEquals("{\"v\":[]}", mapper.writeValueAsString(new LongArray(new long[0])));
    }

    @DisplayName("Long[] 和 List<Long> 生效")
    @Test
    void testBoxedContainers() {
        ObjectMapper mapper = mapper();

        assertEquals("{\"v\":[\"9223372036854775807\",42]}",
                mapper.writeValueAsString(new BoxedArray(new Long[]{UNSAFE, SAFE})));
        assertEquals("{\"v\":[\"9223372036854775807\",42]}",
                mapper.writeValueAsString(new LongList(List.of(UNSAFE, SAFE))));
    }

    @DisplayName("根级 Long 和 long[] 生效")
    @Test
    void testRootLevel() {
        ObjectMapper mapper = mapper();

        assertEquals("\"9223372036854775807\"", mapper.writeValueAsString(UNSAFE));
        assertEquals("[\"9223372036854775807\",42]", mapper.writeValueAsString(new long[]{UNSAFE, SAFE}));
    }

    // 注解优先级

    @DisplayName("@JsonFormat(shape=STRING) 强制字符串, 与裸 Jackson 一致")
    @Test
    void testShapeStringHonored() {
        ObjectMapper safeLong = mapper();
        ObjectMapper bare = JsonMapper.builder().build();

        // 安全范围内本会输出数字, 注解要求字符串
        assertEquals("{\"v\":\"42\"}", safeLong.writeValueAsString(new ShapeString(SAFE)));
        assertEquals(bare.writeValueAsString(new ShapeString(SAFE)),
                safeLong.writeValueAsString(new ShapeString(SAFE)));
    }

    @DisplayName("@JsonFormat(shape=NUMBER) 强制数字, 与裸 Jackson 一致")
    @Test
    void testShapeNumberHonored() {
        ObjectMapper safeLong = mapper();
        ObjectMapper bare = JsonMapper.builder().build();

        // 超出安全范围本会输出字符串, 注解要求数字
        assertEquals("{\"v\":9223372036854775807}", safeLong.writeValueAsString(new ShapeNumber(UNSAFE)));
        assertEquals(bare.writeValueAsString(new ShapeNumber(UNSAFE)),
                safeLong.writeValueAsString(new ShapeNumber(UNSAFE)));
    }

    @DisplayName("long[] 字段上的 @JsonFormat 同样透传到元素")
    @Test
    void testShapeOnPrimitiveArray() {
        assertEquals("{\"v\":[9223372036854775807,42]}",
                mapper().writeValueAsString(new ShapeNumberArray(new long[]{UNSAFE, SAFE})));
    }

    // 作用范围

    @DisplayName("不影响 Map 的 key, Integer, AtomicLong 和 BigInteger")
    @Test
    void testScope() {
        ObjectMapper mapper = mapper();

        // JSON 的 key 本身就是字符串, 不存在精度问题
        assertEquals("{\"9223372036854775807\":\"x\"}",
                mapper.writeValueAsString(Map.of(UNSAFE, "x")));
        assertEquals("42", mapper.writeValueAsString(42));
        assertEquals("9223372036854775807", mapper.writeValueAsString(new AtomicLong(UNSAFE)));
        assertEquals("9223372036854775807", mapper.writeValueAsString(BigInteger.valueOf(UNSAFE)));
    }

    @DisplayName("默认不启用, 未注册模块时输出数字")
    @Test
    void testNotEnabledByDefault() {
        assertEquals("{\"v\":9223372036854775807}",
                JsonUtil.builder().build().writeValueAsString(new Boxed(UNSAFE)));
    }

    // 往返

    @DisplayName("字符串和数字两种输入都能读回")
    @Test
    void testRoundTrip() {
        ObjectMapper mapper = mapper();

        assertEquals(UNSAFE, mapper.readValue("{\"v\":\"9223372036854775807\"}", Boxed.class).v());
        assertEquals(UNSAFE, mapper.readValue("{\"v\":9223372036854775807}", Boxed.class).v());
        assertEquals(UNSAFE,
                mapper.readValue(mapper.writeValueAsString(new Boxed(UNSAFE)), Boxed.class).v());
        assertEquals(UNSAFE,
                mapper.readValue(mapper.writeValueAsString(new LongArray(new long[]{UNSAFE})), LongArray.class)
                        .v()[0]);
    }
}
