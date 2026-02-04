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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TimeUtil 单元测试
 * 测试 TimeUtil.parse 方法
 *
 * @author Toint
 * @since 2026/01/18
 */
class TimeUtilTest {

    /**
     * 测试 parse 方法
     * 验证各种日期时间格式能够正确解析为 LocalDateTime
     */
    @Test
    void testParse() {
        assertNotNull(TimeUtil.parse("2026-01-01 12:30:00"));
        assertNotNull(TimeUtil.parse("2026-01-01 12:30:00.123"));
        assertNotNull(TimeUtil.parse("2026/01/01 12:30:00"));
        assertNotNull(TimeUtil.parse("2026/01/01 12:30:00.123"));
        assertNotNull(TimeUtil.parse("20260101123000"));
        assertNotNull(TimeUtil.parse("2026年01月01日 12时30分00秒"));
        assertNotNull(TimeUtil.parse("2026-01-01T12:30:00"));
        assertNotNull(TimeUtil.parse("2026-01-01T12:30:00.123"));
        assertNotNull(TimeUtil.parse("2026-01-01"));
        assertNotNull(TimeUtil.parse("2026/01/01"));
        assertNotNull(TimeUtil.parse("20260101"));
        assertNotNull(TimeUtil.parse("2026年01月01日"));
        assertNull(TimeUtil.parse(null));
        assertNull(TimeUtil.parse("   "));
        assertNull(TimeUtil.parse("invalid"));
        assertNull(TimeUtil.parse("12:30:00"));
    }
}
