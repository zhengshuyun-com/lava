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
package com.zhengshuyun.lava.core.lang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void requiresBooleanConditions() {
        assertDoesNotThrow(() -> ValidationUtils.requireTrue(true));
        assertDoesNotThrow(() -> ValidationUtils.requireFalse(false));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireTrue(false));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireFalse(true));
    }

    @Test
    void usesCustomMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireTrue(false, "自定义消息"));

        assertEquals("自定义消息", exception.getMessage());
    }

    @Test
    void requiresNonNullValue() {
        Object value = new Object();

        assertSame(value, ValidationUtils.requireNonNull(value));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonNull(null, "不能为空"));
    }

    @Test
    void requiresNonBlankString() {
        assertEquals("lava", ValidationUtils.requireNotBlank("lava"));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNotBlank(null));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNotBlank("   ", "不能为空白"));
    }

    @Test
    void requiresNonEmptyCollection() {
        List<String> values = List.of("lava");

        assertSame(values, ValidationUtils.requireNotEmpty(values));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNotEmpty(List.of(), "集合不能为空"));
    }

    @Test
    void requiresNonEmptyMap() {
        Map<String, String> values = Map.of("name", "lava");

        assertSame(values, ValidationUtils.requireNotEmpty(values));
        assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNotEmpty(Map.of(), "Map 不能为空"));
    }
}
