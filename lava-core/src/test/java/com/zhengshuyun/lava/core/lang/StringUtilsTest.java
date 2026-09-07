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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证字符串判断与默认值转换的空值边界和原值保留语义。
 */
class StringUtilsTest {

    /** 验证空字符串判断不会把纯空白误判为空字符串。 */
    @Test
    void identifiesEmptyValues() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" "));
        assertFalse(StringUtils.isEmpty("lava"));

        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
        assertTrue(StringUtils.isNotEmpty(" "));
        assertTrue(StringUtils.isNotEmpty("lava"));
    }

    /** 验证空白判断沿用 JDK 的 Unicode 空白字符语义。 */
    @Test
    void identifiesBlankValues() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank(" \t\n"));
        assertTrue(StringUtils.isBlank("\u3000"));
        assertFalse(StringUtils.isBlank(" lava "));

        assertFalse(StringUtils.isNotBlank(null));
        assertFalse(StringUtils.isNotBlank("\u3000"));
        assertTrue(StringUtils.isNotBlank("lava"));
    }

    /** 验证 null 与空字符串转换不会修改非空原值。 */
    @Test
    void convertsNullAndEmptyValues() {
        String value = new String("lava");

        assertEquals("", StringUtils.nullToEmpty(null));
        assertSame(value, StringUtils.nullToEmpty(value));
        assertNull(StringUtils.emptyToNull(null));
        assertNull(StringUtils.emptyToNull(""));
        assertEquals(" ", StringUtils.emptyToNull(" "));
        assertSame(value, StringUtils.emptyToNull(value));
    }

    /** 验证默认值方法分别采用空字符串和空白字符串边界。 */
    @Test
    void suppliesDefaultsForEmptyAndBlankValues() {
        String value = new String("lava");

        assertEquals("default", StringUtils.defaultIfEmpty(null, "default"));
        assertEquals("default", StringUtils.defaultIfEmpty("", "default"));
        assertEquals(" ", StringUtils.defaultIfEmpty(" ", "default"));
        assertSame(value, StringUtils.defaultIfEmpty(value, "default"));

        assertEquals("default", StringUtils.defaultIfBlank(null, "default"));
        assertEquals("default", StringUtils.defaultIfBlank(" \t", "default"));
        assertSame(value, StringUtils.defaultIfBlank(value, "default"));
    }

    /** 默认值即使当前分支未使用也必须满足非空契约。 */
    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullDefaultsConsistently() {
        assertThrows(IllegalArgumentException.class,
                () -> StringUtils.defaultIfEmpty("lava", null));
        assertThrows(IllegalArgumentException.class,
                () -> StringUtils.defaultIfBlank("lava", null));
    }
}
