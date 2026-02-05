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

package com.zhengshuyun.common.core.lang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateTest {
    @Test
    void testNotEmpty_collection() {
        assertThrows(IllegalArgumentException.class, () -> Validate.notEmpty(List.of()));
        assertThrows(IllegalArgumentException.class, () -> Validate.notEmpty(List.of(), "must not be empty"));

        try {
            Validate.notEmpty(List.of(), "%s must not be empty", "Collection");
        } catch (Exception e) {
            assertEquals("Collection must not be empty", e.getMessage());
        }
    }

    @Test
    void testNotEmpty_map() {
        assertThrows(IllegalArgumentException.class, () -> Validate.notEmpty(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> Validate.notEmpty(Map.of(), "must not be empty"));

        try {
            Validate.notEmpty(Map.of(), "%s must not be empty", "Map");
        } catch (Exception e) {
            assertEquals("Map must not be empty", e.getMessage());
        }
    }

    // isFalse

    @Test
    void isFalse_shouldPass() {
        assertFalse(Validate.isFalse(false));
        assertFalse(Validate.isFalse(false, "错误"));
        assertFalse(Validate.isFalse(false, "错误: %s", "test"));
    }

    @Test
    void isFalse_shouldThrow_noMessage() {
        assertThrows(IllegalArgumentException.class, () -> Validate.isFalse(true));
    }

    @Test
    void isFalse_shouldThrow_withMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isFalse(true, "值必须为 false")
        );
        assertEquals("值必须为 false", ex.getMessage());
    }

    @Test
    void isFalse_shouldThrow_withTemplate() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isFalse(true, "期望 %s", false)
        );
        assertEquals("期望 false", ex.getMessage());
    }

    // isTrue

    @Test
    void isTrue_shouldPass() {
        assertTrue(Validate.isTrue(true));
        assertTrue(Validate.isTrue(true, "错误"));
        assertTrue(Validate.isTrue(true, "错误: %s", "test"));
    }

    @Test
    void isTrue_shouldThrow_noMessage() {
        assertThrows(IllegalArgumentException.class, () -> Validate.isTrue(false));
    }

    @Test
    void isTrue_shouldThrow_withMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isTrue(false, "值必须为 true")
        );
        assertEquals("值必须为 true", ex.getMessage());
    }

    @Test
    void isTrue_shouldThrow_withTemplate() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isTrue(false, "期望 %s 但得到 %s", true, false)
        );
        assertEquals("期望 true 但得到 false", ex.getMessage());
    }

    // notBlank

    @Test
    void notBlank_shouldPass() {
        assertEquals("hello", Validate.notBlank("hello"));
        assertEquals("hello", Validate.notBlank("hello", "错误"));
        assertEquals("hello", Validate.notBlank("hello", "错误: %s", "test"));
    }

    @Test
    void notBlank_shouldThrow_noMessage() {
        assertThrows(IllegalArgumentException.class, () -> Validate.notBlank(null));
        assertThrows(IllegalArgumentException.class, () -> Validate.notBlank(""));
        assertThrows(IllegalArgumentException.class, () -> Validate.notBlank("   "));
    }

    @Test
    void notBlank_shouldThrow_withMessage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Validate.notBlank(null, "字符串不能为空")
        );
    }

    @Test
    void notBlank_shouldThrow_withTemplate() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.notBlank("   ", "字段 '%s' 不能为空白", "username")
        );
        assertEquals("字段 'username' 不能为空白", ex.getMessage());
    }

    // notNull

    @Test
    void notNull_shouldPass() {
        assertEquals("test", Validate.notNull("test"));
        assertEquals("test", Validate.notNull("test", "错误"));
        assertEquals("test", Validate.notNull("test", "错误: %s", "test"));
    }

    @Test
    void notNull_shouldThrow_noMessage() {
        assertThrows(IllegalArgumentException.class, () -> Validate.notNull(null));
    }

    @Test
    void notNull_shouldThrow_withMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.notNull(null, "对象不能为 null")
        );
        assertEquals("对象不能为 null", ex.getMessage());
    }

    @Test
    void notNull_shouldThrow_withTemplate() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.notNull(null, "参数 '%s' 不能为 null", "config")
        );
        assertEquals("参数 'config' 不能为 null", ex.getMessage());
    }

    // isNull

    @Test
    void isNull_shouldPass() {
        assertDoesNotThrow(() -> Validate.isNull(null));
        assertDoesNotThrow(() -> Validate.isNull(null, "错误"));
        assertDoesNotThrow(() -> Validate.isNull(null, "错误: %s", "test"));
    }

    @Test
    void isNull_shouldThrow_noMessage() {
        assertThrows(IllegalArgumentException.class, () -> Validate.isNull("not null"));
    }

    @Test
    void isNull_shouldThrow_withMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isNull("not null", "对象必须为 null")
        );
        assertEquals("对象必须为 null", ex.getMessage());
    }

    @Test
    void isNull_shouldThrow_withTemplate() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isNull(123, "字段 '%s' 必须为 null", "optional")
        );
        assertEquals("字段 'optional' 必须为 null", ex.getMessage());
    }

    // isEmail

    @Test
    void isEmail_shouldPass() {
        assertEquals("test@example.com", Validate.isEmail("test@example.com"));
        assertEquals("user+tag@domain.co", Validate.isEmail("user+tag@domain.co"));
    }

    @Test
    void isEmail_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> Validate.isEmail(null));
        assertThrows(IllegalArgumentException.class, () -> Validate.isEmail(""));
        assertThrows(IllegalArgumentException.class, () -> Validate.isEmail("invalid"));
    }

    @Test
    void isEmail_shouldThrow_withMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isEmail("invalid", "邮箱格式错误")
        );
        assertEquals("邮箱格式错误", ex.getMessage());
    }

    // isMobile

    @Test
    void isMobile_shouldPass() {
        assertEquals("13800138000", Validate.isMobile("13800138000"));
        assertEquals("19912345678", Validate.isMobile("19912345678"));
    }

    @Test
    void isMobile_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> Validate.isMobile(null));
        assertThrows(IllegalArgumentException.class, () -> Validate.isMobile(""));
        assertThrows(IllegalArgumentException.class, () -> Validate.isMobile("12345678901"));
        assertThrows(IllegalArgumentException.class, () -> Validate.isMobile("1380013800"));
        assertThrows(IllegalArgumentException.class, () -> Validate.isMobile("23800138000"));
    }

    @Test
    void isMobile_shouldThrow_withMessage() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Validate.isMobile("12345678901", "手机号格式错误")
        );
        assertEquals("手机号格式错误", ex.getMessage());
    }
}
