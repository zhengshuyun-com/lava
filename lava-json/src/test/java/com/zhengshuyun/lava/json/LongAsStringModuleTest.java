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
package com.zhengshuyun.lava.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongAsStringModuleTest {

    private static final JsonCodec STRING_LONGS = new JsonCodec(JsonMapperFactory.builder()
            .addModule(new LongAsStringModule())
            .build());

    record Values(
            long primitive,
            Long boxed,
            long[] primitiveArray,
            Long[] boxedArray,
            List<Long> list,
            Map<String, Object> nested,
            Optional<Long> optional,
            OptionalLong optionalLong) {
    }

    record NumericOverrides(
            @JsonFormat(shape = JsonFormat.Shape.NUMBER) long primitive,
            @JsonFormat(shape = JsonFormat.Shape.NUMBER) Long boxed,
            @JsonFormat(shape = JsonFormat.Shape.NUMBER) long[] array,
            @JsonFormat(shape = JsonFormat.Shape.NUMBER) OptionalLong optionalLong) {
    }

    @Test
    void optInModuleUsesOneStableStringShapeEverywhere() {
        Values values = new Values(
                1,
                Long.MAX_VALUE,
                new long[]{2, Long.MIN_VALUE},
                new Long[]{3L, Long.MAX_VALUE},
                List.of(4L, Long.MAX_VALUE),
                Map.of("inner", List.of(5L, Long.MIN_VALUE)),
                Optional.of(6L),
                OptionalLong.of(Long.MAX_VALUE));

        JsonNode root = STRING_LONGS.readTree(STRING_LONGS.write(values));
        assertTrue(root.get("primitive").isString());
        assertTrue(root.get("boxed").isString());
        assertAllText(root.get("primitiveArray"));
        assertAllText(root.get("boxedArray"));
        assertAllText(root.get("list"));
        assertTrue(root.get("nested").get("inner").get(0).isString());
        assertTrue(root.get("optional").isString());
        assertTrue(root.get("optionalLong").isString());
        assertEquals(Long.toString(Long.MAX_VALUE), root.get("boxed").stringValue());
    }

    @Test
    void staticJsonFormatCanKeepSelectedFieldsNumeric() {
        JsonNode root = STRING_LONGS.readTree(STRING_LONGS.write(new NumericOverrides(
                1, 2L, new long[]{3, 4}, OptionalLong.of(5))));

        assertTrue(root.get("primitive").isIntegralNumber());
        assertTrue(root.get("boxed").isIntegralNumber());
        assertTrue(root.get("array").get(0).isIntegralNumber());
        assertTrue(root.get("optionalLong").isIntegralNumber());
    }

    @Test
    void defaultCodecNeverChangesNumberShapeBasedOnMagnitude() {
        JsonNode small = JsonCodec.defaultCodec().readTree(JsonCodec.defaultCodec().write(1L));
        JsonNode large = JsonCodec.defaultCodec().readTree(JsonCodec.defaultCodec().write(Long.MAX_VALUE));
        assertTrue(small.isIntegralNumber());
        assertTrue(large.isIntegralNumber());
    }

    @Test
    void handlesRootLongAndEmptyOptionalLong() {
        assertTrue(STRING_LONGS.readTree(STRING_LONGS.write(9L)).isString());
        JsonNode empty = STRING_LONGS.readTree(STRING_LONGS.write(OptionalLong.empty()));
        assertTrue(empty.isNull());
    }

    private static void assertAllText(JsonNode array) {
        for (JsonNode element : array) {
            assertTrue(element.isString(), () -> "expected string but got " + element);
        }
    }
}
