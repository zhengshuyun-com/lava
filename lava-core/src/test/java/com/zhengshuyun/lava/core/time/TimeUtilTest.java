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

package com.zhengshuyun.lava.core.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 1, 12, 30, 0);
    private static final LocalDateTime DATE_ONLY = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

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

    /**
     * 校验解析结果的具体取值, 而不只是非 null
     */
    @Test
    void testParseValues() {
        assertEquals(DATE_TIME, TimeUtil.parse("2026-01-01 12:30:00"));
        assertEquals(DATE_TIME, TimeUtil.parse("2026/01/01 12:30:00"));
        assertEquals(DATE_TIME, TimeUtil.parse("2026-01-01T12:30:00"));
        assertEquals(DATE_TIME, TimeUtil.parse("20260101123000"));
        assertEquals(DATE_TIME, TimeUtil.parse("2026年01月01日 12时30分00秒"));

        // 只有日期时, 时间补齐为 00:00:00
        assertEquals(DATE_ONLY, TimeUtil.parse("2026-01-01"));
        assertEquals(DATE_ONLY, TimeUtil.parse("2026/01/01"));
        assertEquals(DATE_ONLY, TimeUtil.parse("20260101"));
        assertEquals(DATE_ONLY, TimeUtil.parse("2026年01月01日"));

        // 小数秒
        assertEquals(DATE_TIME.withNano(123_000_000), TimeUtil.parse("2026-01-01 12:30:00.123"));
        assertEquals(DATE_TIME.withNano(123_000_000), TimeUtil.parse("2026/01/01 12:30:00.123"));
        assertEquals(DATE_TIME.withNano(100_000_000), TimeUtil.parse("2026-01-01 12:30:00.1"));
        assertEquals(DATE_TIME.withNano(123_456_789), TimeUtil.parse("2026-01-01 12:30:00.123456789"));

        // 前后空白会被裁剪
        assertEquals(DATE_TIME, TimeUtil.parse("  2026-01-01 12:30:00  "));
    }

    /**
     * 非法输入必须返回 null, 而不是抛异常或给出错误结果
     */
    @Test
    void testParseInvalid() {
        assertNull(TimeUtil.parse(""));
        assertNull(TimeUtil.parse("2026-13-01"));          // 月份越界
        assertNull(TimeUtil.parse("2026-01-32"));          // 日越界
        assertNull(TimeUtil.parse("2026-02-30"));          // 不存在的日期
        assertNull(TimeUtil.parse("2026-01-01 25:00:00")); // 小时越界
        assertNull(TimeUtil.parse("2026-1-1"));            // 非补零
        assertNull(TimeUtil.parse("20260101 123000"));     // 紧凑格式不接受空格
        assertNull(TimeUtil.parse("2026-01-01 12:30"));    // 缺少秒
        assertNull(TimeUtil.parse("not a date"));
        assertNull(TimeUtil.parse("123"));
    }

    /**
     * 日期与时间之间必须且只能有一个分隔符 (空格或 T)
     * <p>
     * 若把两个分隔符各自放进独立的可选段, 两者都可跳过, 会连带接受
     * "无分隔符" 和 "两个分隔符" 两种畸形输入
     */
    @Test
    void testSeparatorMustBeExactlyOne() {
        // 合法: 恰好一个分隔符
        assertEquals(DATE_TIME, TimeUtil.parse("2026-01-01 12:30:00"));
        assertEquals(DATE_TIME, TimeUtil.parse("2026-01-01T12:30:00"));
        assertEquals(DATE_TIME, TimeUtil.parse("2026/01/01 12:30:00"));
        assertEquals(DATE_TIME, TimeUtil.parse("2026/01/01T12:30:00"));

        // 非法: 无分隔符
        assertNull(TimeUtil.parse("2026-01-0112:30:00"));
        assertNull(TimeUtil.parse("2026/01/0112:30:00"));

        // 非法: 两个分隔符
        assertNull(TimeUtil.parse("2026-01-01 T12:30:00"));
        assertNull(TimeUtil.parse("2026-01-01T 12:30:00"));
        assertNull(TimeUtil.parse("2026/01/01 T12:30:00"));

        // 中文格式同理: 分隔符与时间部分绑定
        assertEquals(DATE_TIME, TimeUtil.parse("2026年01月01日 12时30分00秒"));
        assertNull(TimeUtil.parse("2026年01月01日12时30分00秒"));
    }
}
