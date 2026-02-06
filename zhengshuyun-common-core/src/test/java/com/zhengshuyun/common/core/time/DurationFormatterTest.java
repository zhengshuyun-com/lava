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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DurationFormatter 测试类
 * <p>
 * 测试时长格式化器的各种功能, 包括：
 * <ul>
 *   <li>基本格式化：英文/中文格式</li>
 *   <li>自定义配置：分隔符、零值显示、单位范围</li>
 *   <li>工厂方法：ofSeconds、ofMillis、ofMinutes 等</li>
 *   <li>精度控制：毫秒、微秒、纳秒级别的显示</li>
 *   <li>异常处理：负数、null、无效范围</li>
 * </ul>
 *
 * @author Toint
 * @since 2026/1/11
 */
class DurationFormatterTest {

    /**
     * 测试基本格式化功能
     * <p>
     * 输入：3665 秒
     * 预期输出：1h 1min 5s (1小时 1分钟 5秒)
     * 验证默认情况下时长的正确格式化
     */
    @Test
    void testBasicFormat() {
        String result = DurationFormatter.ofSeconds(3665).format();
        assertEquals("1h 1min 5s", result);
    }

    /**
     * 测试中文格式化
     * <p>
     * 输入：3665 秒
     * 预期输出：1时 1分 5秒
     * 验证 setChinese() 方法能正确切换到中文格式
     */
    @Test
    void testSetChineseFormat() {
        String result = DurationFormatter.ofSeconds(3665)
                .setChinese()
                .format();
        assertEquals("1时 1分 5秒", result);
    }

    /**
     * 测试自定义分隔符
     * <p>
     * 输入：3665 秒, 分隔符设为 ", "
     * 预期输出：1h, 1min, 5s
     * 验证 setSeparator() 方法能自定义单位之间的分隔符
     */
    @Test
    void testCustomSeparator() {
        String result = DurationFormatter.ofSeconds(3665)
                .setSeparator(", ")
                .format();
        assertEquals("1h, 1min, 5s", result);
    }

    /**
     * 测试显示零值单位
     * <p>
     * 输入：3605 秒 (1小时 0分钟 5秒)
     * 预期输出：1h 0min 5s
     * 验证 setShowZeroValues(true) 会显示数值为0的单位
     */
    @Test
    void testShowZeroValues() {
        String result = DurationFormatter.ofSeconds(3605)
                .setShowZeroValues(true)
                .format();
        assertEquals("1h 0min 5s", result);
    }

    /**
     * 测试隐藏零值单位
     * <p>
     * 输入：3605 秒 (1小时 0分钟 5秒)
     * 预期输出：1h 5s
     * 验证 setShowZeroValues(false) 会自动省略数值为0的单位
     */
    @Test
    void testHideZeroValues() {
        String result = DurationFormatter.ofSeconds(3605)
                .setShowZeroValues(false)
                .format();
        assertEquals("1h 5s", result);
    }

    /**
     * 测试只显示毫秒
     * <p>
     * 输入：5500 毫秒
     * 范围：只显示毫秒
     * 预期输出：5500ms
     * 验证设置范围为 MILLIS 到 MILLIS 时只显示毫秒单位
     */
    @Test
    void testOnlyMillis() {
        String result = DurationFormatter.ofMillis(5500)
                .setRange(ChronoUnit.MILLIS, ChronoUnit.MILLIS)
                .format();
        assertEquals("5500ms", result);
    }

    /**
     * 测试只显示微秒
     * <p>
     * 输入：5500000 纳秒 = 5500 微秒
     * 范围：只显示微秒
     * 预期输出：5500μs
     * 验证设置范围为 MICROS 到 MICROS 时只显示微秒单位
     */
    @Test
    void testOnlyMicros() {
        String result = DurationFormatter.ofNanos(5500000)
                .setRange(ChronoUnit.MICROS, ChronoUnit.MICROS)
                .format();
        assertEquals("5500μs", result);
    }

    /**
     * 测试只显示纳秒
     * <p>
     * 输入：12345 纳秒
     * 范围：只显示纳秒
     * 预期输出：12345ns
     * 验证设置范围为 NANOS 到 NANOS 时只显示纳秒单位
     */
    @Test
    void testOnlyNanos() {
        String result = DurationFormatter.ofNanos(12345)
                .setRange(ChronoUnit.NANOS, ChronoUnit.NANOS)
                .format();
        assertEquals("12345ns", result);
    }

    /**
     * 测试毫秒与秒的组合显示
     * <p>
     * 输入：5500 毫秒 = 5秒 500毫秒
     * 范围：秒到毫秒
     * 预期输出：5s 500ms
     * 验证毫秒能正确转换为秒和毫秒的组合
     */
    @Test
    void testMillisWithSeconds() {
        String result = DurationFormatter.ofMillis(5500)
                .setRange(ChronoUnit.SECONDS, ChronoUnit.MILLIS)
                .format();
        assertEquals("5s 500ms", result);
    }

    /**
     * 测试完整时间范围格式化 (英文)
     * <p>
     * 输入：100亿毫秒 ≈ 115.74天
     * 范围：月到毫秒
     * 预期输出：3mo 25d 17h 46min 40s
     * 验证能正确处理大时长的完整格式化 (包含月、天、小时、分钟、秒)
     */
    @Test
    void testFullRange() {
        // 100亿毫秒 ≈ 115天 (约3个月25天)
        String result = DurationFormatter.ofMillis(10_000_000_000L)
                .setRange(ChronoUnit.MONTHS, ChronoUnit.MILLIS)
                .setEnglish()
                .format();
        assertEquals("3mo 25d 17h 46min 40s", result);
    }

    /**
     * 测试完整时间范围格式化 (中文)
     * <p>
     * 输入：100亿毫秒
     * 范围：年到秒
     * 格式：中文, 无分隔符
     * 验证中文格式下包含"天"和"时"等单位
     */
    @Test
    void testFullRangeSetChinese() {
        String result = DurationFormatter.ofMillis(10_000_000_000L)
                .setRange(ChronoUnit.YEARS, ChronoUnit.SECONDS)
                .setChinese()
                .setSeparator("")
                .format();
        assertTrue(result.contains("天"));
        assertTrue(result.contains("时"));
    }

    /**
     * 测试年份计算
     * <p>
     * 输入：400天 = 365天 + 30天 + 5天
     * 范围：年到天
     * 预期输出：1y 1mo 5d
     * 验证能正确计算年、月、天 (注：1年=365天, 1月=30天)
     */
    @Test
    void testYearsCalculation() {
        // 400天 = 1年 + 1月 + 5天 (365 + 30 + 5)
        long seconds = 400L * 24 * 3600;
        String result = DurationFormatter.ofSeconds(seconds)
                .setRange(ChronoUnit.YEARS, ChronoUnit.DAYS)
                .format();
        assertEquals("1y 1mo 5d", result);
    }

    /**
     * 测试月份计算
     * <p>
     * 输入：90天 = 3个月
     * 范围：月到天
     * 预期输出：3mo
     * 验证能正确将天数转换为月份 (注：1月=30天)
     */
    @Test
    void testMonthsCalculation() {
        // 90天 = 3个月 (30 * 3)
        long seconds = 90L * 24 * 3600;
        String result = DurationFormatter.ofSeconds(seconds)
                .setRange(ChronoUnit.MONTHS, ChronoUnit.DAYS)
                .format();
        assertEquals("3mo", result);
    }

    /**
     * 测试零时长格式化
     * <p>
     * 输入：0秒
     * 预期输出：0s
     * 验证时长的默认格式化 (最小单位为秒)
     */
    @Test
    void testZeroDuration() {
        String result = DurationFormatter.ofSeconds(0).format();
        assertEquals("0s", result);
    }

    /**
     * 测试零时长使用不同单位
     * <p>
     * 输入：0秒, 最小单位设为分钟
     * 预期输出：0min
     * 验证 setSmallestUnit() 能改变零值的显示单位
     */
    @Test
    void testZeroDurationWithDifferentUnit() {
        String result = DurationFormatter.ofSeconds(0)
                .setSmallestUnit(ChronoUnit.MINUTES)
                .format();
        assertEquals("0min", result);
    }

    /**
     * 测试无效范围参数
     * <p>
     * 尝试设置最大单位小于最小单位 (秒 < 小时)
     * 预期：抛出 IllegalArgumentException
     * 验证 setRange() 会拒绝非法的范围参数
     */
    @Test
    void testInvalidRange() {
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.ofSeconds(100)
                        .setRange(ChronoUnit.SECONDS, ChronoUnit.HOURS)
        );
    }

    /**
     * 测试负时长异常
     * <p>
     * 尝试创建负数时长：-100秒
     * 预期：抛出 IllegalArgumentException
     * 验证 DurationFormatter 不接受负数时长
     */
    @Test
    void testNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.of(Duration.ofSeconds(-100))
        );
    }

    /**
     * 测试 null 时长异常
     * <p>
     * 尝试传入 null
     * 预期：抛出 IllegalArgumentException
     * 验证 DurationFormatter 不接受 null 值
     */
    @Test
    void testNullDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.of((Duration) null)
        );
    }

    /**
     * 测试 TimeUnit 工厂方法
     * <p>
     * 输入：5 分钟 (使用 TimeUnit.MINUTES)
     * 预期输出：5min
     * 验证 of(long, TimeUnit) 工厂方法能正确创建格式化器
     */
    @Test
    void testTimeUnitFactory() {
        String result = DurationFormatter.of(5, TimeUnit.MINUTES).format();
        assertEquals("5min", result);
    }

    /**
     * 测试复杂时长格式化
     * <p>
     * 输入：1天2小时3分钟4秒500毫秒
     * 范围：天到毫秒
     * 预期输出：1d 2h 3min 4s 500ms
     * 验证能正确处理包含多个单位的复杂时长
     */
    @Test
    void testComplexFormat() {
        // 1天 2小时 3分钟 4秒 500毫秒
        long millis = (1L * 24 * 3600 + 2 * 3600 + 3 * 60 + 4) * 1000 + 500;
        String result = DurationFormatter.ofMillis(millis)
                .setRange(ChronoUnit.DAYS, ChronoUnit.MILLIS)
                .format();
        assertEquals("1d 2h 3min 4s 500ms", result);
    }

    /**
     * 测试秒到纳秒的完整精度格式化 (英文)
     * <p>
     * 输入：1_123_456_789 纳秒 = 1秒 + 123毫秒 + 456微秒 + 789纳秒
     * 范围：秒到纳秒
     * 预期输出：1s 123ms 456μs 789ns
     * 验证能正确处理从秒到纳秒的完整精度 (包含秒、毫秒、微秒、纳秒)
     */
    @Test
    void testSecondsToNanosRange() {
        // 1秒 + 123毫秒 + 456微秒 + 789纳秒
        long nanos = 1_123_456_789L;
        String result = DurationFormatter.ofNanos(nanos)
                .setRange(ChronoUnit.SECONDS, ChronoUnit.NANOS)
                .format();
        assertEquals("1s 123ms 456μs 789ns", result);
    }

    /**
     * 测试秒到纳秒的完整精度格式化 (中文)
     * <p>
     * 输入：1_123_456_789 纳秒
     * 范围：秒到纳秒
     * 预期输出：1秒 123毫秒 456微秒 789纳秒
     * 验证中文格式下从秒到纳秒的完整精度单位显示
     */
    @Test
    void testSecondsToNanosRangeChinese() {
        long nanos = 1_123_456_789L;
        String result = DurationFormatter.ofNanos(nanos)
                .setRange(ChronoUnit.SECONDS, ChronoUnit.NANOS)
                .setChinese()
                .format();
        assertEquals("1秒 123毫秒 456微秒 789纳秒", result);
    }

    /**
     * 测试小时到分钟的范围限制
     * <p>
     * 输入：3665 秒
     * 范围：小时到分钟
     * 预期输出：1h 1min
     * 验证设置范围为 HOURS 到 MINUTES 后, 不显示超出范围的单位 (不显示秒)
     */
    @Test
    void testRangeHoursToMinutes() {
        String result = DurationFormatter.ofSeconds(3665)
                .setRange(ChronoUnit.HOURS, ChronoUnit.MINUTES)
                .format();
        assertEquals("1h 1min", result); // 不显示秒
    }

    /**
     * 测试秒到秒的范围限制
     * <p>
     * 输入：3665 秒
     * 范围：秒到秒
     * 预期输出：3665s
     * 验证设置范围为 SECONDS 到 SECONDS 时只显示秒单位
     */
    @Test
    void testRangeSecondsToSeconds() {
        String result = DurationFormatter.ofSeconds(3665)
                .setRange(ChronoUnit.SECONDS, ChronoUnit.SECONDS)
                .format();
        assertEquals("3665s", result);
    }

    /**
     * 测试大数值格式化
     * <p>
     * 输入：10年的秒数
     * 范围：年到天
     * 预期输出：10y
     * 验证能正确处理非常大的时长值
     */
    @Test
    void testLargeValue() {
        // 10年
        long seconds = 10L * 365 * 24 * 3600;
        String result = DurationFormatter.ofSeconds(seconds)
                .setRange(ChronoUnit.YEARS, ChronoUnit.DAYS)
                .format();
        assertEquals("10y", result);
    }

    /**
     * 测试所有 ofXxx 工厂方法
     * <p>
     * 验证以下 ofXxx 工厂方法的正确性：
     * <ul>
     *   <li>ofMinutes(5) → 5min (默认最大单位为小时) </li>
     *   <li>ofHours(5) → 5h (默认最大单位为小时) </li>
     *   <li>ofDays(5) → 120h (默认最大单位为小时, 5天=120小时) </li>
     *   <li>ofDays(5).setRange(DAYS) → 5d (设置范围后才显示天) </li>
     *   <li>ofMillis(5000).setRange(MILLIS) → 5000ms (设置范围后才显示毫秒) </li>
     * </ul>
     */
    @Test
    void testAllOfXxxFactoryMethods() {
        assertEquals("5min", DurationFormatter.ofMinutes(5).format());
        assertEquals("5h", DurationFormatter.ofHours(5).format());
        assertEquals("120h", DurationFormatter.ofDays(5).format());
        assertEquals("5d", DurationFormatter.ofDays(5)
                .setRange(ChronoUnit.DAYS, ChronoUnit.DAYS)
                .format());
        assertEquals("5000ms",
                DurationFormatter.ofMillis(5000)
                        .setRange(ChronoUnit.MILLIS, ChronoUnit.MILLIS)
                        .format());
    }
}
