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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DurationFormatter 测试类
 * <p>
 * 测试时长格式化器的各种功能, 包括:
 * <ul>
 *   <li>基本格式化: 英文/中文格式</li>
 *   <li>自定义配置: 分隔符、零值显示、单位范围</li>
 *   <li>精度控制: 毫秒、微秒、纳秒级别的显示</li>
 *   <li>异常处理: 负数、null、无效范围</li>
 *   <li>Formatter 可复用: 同一实例格式化不同 Duration</li>
 * </ul>
 *
 * @author Toint
 * @since 2026/1/11
 */
class DurationFormatterTest {

    /**
     * 默认 Formatter (小时到秒, 英文)
     */
    private final DurationFormatter defaultFormatter = DurationFormatter.builder().build();

    /**
     * 测试基本格式化功能
     * <p>
     * 输入: 3665 秒
     * 预期输出: 1h 1min 5s (1小时 1分钟 5秒)
     * 验证默认情况下时长的正确格式化
     */
    @Test
    void testBasicFormat() {
        String result = defaultFormatter.format(Duration.ofSeconds(3665));
        assertEquals("1h 1min 5s", result);
    }

    /**
     * 测试中文格式化
     * <p>
     * 输入: 3665 秒
     * 预期输出: 1时 1分 5秒
     * 验证 setChinese() 方法能正确切换到中文格式
     */
    @Test
    void testSetChineseFormat() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setChinese()
                .build();
        String result = formatter.format(Duration.ofSeconds(3665));
        assertEquals("1小时 1分钟 5秒", result);
    }

    /**
     * 测试自定义分隔符
     * <p>
     * 输入: 3665 秒, 分隔符设为 ", "
     * 预期输出: 1h, 1min, 5s
     * 验证 setSeparator() 方法能自定义单位之间的分隔符
     */
    @Test
    void testCustomSeparator() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setSeparator(", ")
                .build();
        String result = formatter.format(Duration.ofSeconds(3665));
        assertEquals("1h, 1min, 5s", result);
    }

    /**
     * 测试显示零值单位
     * <p>
     * 输入: 3605 秒 (1小时 0分钟 5秒)
     * 预期输出: 1h 0min 5s
     * 验证 setShowZeroValues(true) 会显示数值为0的单位
     */
    @Test
    void testShowZeroValues() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setShowZeroValues(true)
                .build();
        String result = formatter.format(Duration.ofSeconds(3605));
        assertEquals("1h 0min 5s", result);
    }

    /**
     * 测试隐藏零值单位
     * <p>
     * 输入: 3605 秒 (1小时 0分钟 5秒)
     * 预期输出: 1h 5s
     * 验证默认会自动省略数值为0的单位
     */
    @Test
    void testHideZeroValues() {
        String result = defaultFormatter.format(Duration.ofSeconds(3605));
        assertEquals("1h 5s", result);
    }

    /**
     * 测试只显示毫秒
     * <p>
     * 输入: 5500 毫秒
     * 范围: 只显示毫秒
     * 预期输出: 5500ms
     */
    @Test
    void testOnlyMillis() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.MILLIS, ChronoUnit.MILLIS)
                .build();
        String result = formatter.format(Duration.ofMillis(5500));
        assertEquals("5500ms", result);
    }

    /**
     * 测试只显示微秒
     * <p>
     * 输入: 5500000 纳秒 = 5500 微秒
     * 范围: 只显示微秒
     * 预期输出: 5500μs
     */
    @Test
    void testOnlyMicros() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.MICROS, ChronoUnit.MICROS)
                .build();
        String result = formatter.format(Duration.ofNanos(5500000));
        assertEquals("5500μs", result);
    }

    /**
     * 测试只显示纳秒
     * <p>
     * 输入: 12345 纳秒
     * 范围: 只显示纳秒
     * 预期输出: 12345ns
     */
    @Test
    void testOnlyNanos() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.NANOS, ChronoUnit.NANOS)
                .build();
        String result = formatter.format(Duration.ofNanos(12345));
        assertEquals("12345ns", result);
    }

    /**
     * 测试毫秒与秒的组合显示
     * <p>
     * 输入: 5500 毫秒 = 5秒 500毫秒
     * 范围: 秒到毫秒
     * 预期输出: 5s 500ms
     */
    @Test
    void testMillisWithSeconds() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.SECONDS, ChronoUnit.MILLIS)
                .build();
        String result = formatter.format(Duration.ofMillis(5500));
        assertEquals("5s 500ms", result);
    }

    /**
     * 测试完整时间范围格式化 (英文)
     * <p>
     * 输入: 100亿毫秒 ≈ 115.74天
     * 范围: 月到毫秒
     * 预期输出: 3mo 25d 17h 46min 40s
     */
    @Test
    void testFullRange() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.MONTHS, ChronoUnit.MILLIS)
                .setEnglish()
                .build();
        String result = formatter.format(Duration.ofMillis(10_000_000_000L));
        assertEquals("3mo 25d 17h 46min 40s", result);
    }

    /**
     * 测试完整时间范围格式化 (中文)
     * <p>
     * 输入: 100亿毫秒
     * 范围: 年到秒
     * 格式: 中文, 无分隔符
     * 验证中文格式下包含"天"和"小时"等单位
     */
    @Test
    void testFullRangeSetChinese() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.YEARS, ChronoUnit.SECONDS)
                .setChinese()
                .setSeparator("")
                .build();
        String result = formatter.format(Duration.ofMillis(10_000_000_000L));
        assertTrue(result.contains("天"));
        assertTrue(result.contains("小时"));
    }

    /**
     * 测试年份计算
     * <p>
     * 输入: 400天 = 365天 + 30天 + 5天
     * 范围: 年到天
     * 预期输出: 1y 1mo 5d
     * 验证能正确计算年、月、天 (注: 1年=365天, 1月=30天)
     */
    @Test
    void testYearsCalculation() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.YEARS, ChronoUnit.DAYS)
                .build();
        long seconds = 400L * 24 * 3600;
        String result = formatter.format(Duration.ofSeconds(seconds));
        assertEquals("1y 1mo 5d", result);
    }

    /**
     * 测试月份计算
     * <p>
     * 输入: 90天 = 3个月
     * 范围: 月到天
     * 预期输出: 3mo
     */
    @Test
    void testMonthsCalculation() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.MONTHS, ChronoUnit.DAYS)
                .build();
        long seconds = 90L * 24 * 3600;
        String result = formatter.format(Duration.ofSeconds(seconds));
        assertEquals("3mo", result);
    }

    /**
     * 测试零时长格式化
     * <p>
     * 输入: 0秒
     * 预期输出: 0s
     */
    @Test
    void testZeroDuration() {
        String result = defaultFormatter.format(Duration.ofSeconds(0));
        assertEquals("0s", result);
    }

    /**
     * 测试零时长使用不同单位
     * <p>
     * 输入: 0秒, 最小单位设为分钟
     * 预期输出: 0min
     */
    @Test
    void testZeroDurationWithDifferentUnit() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setSmallestUnit(ChronoUnit.MINUTES)
                .build();
        String result = formatter.format(Duration.ofSeconds(0));
        assertEquals("0min", result);
    }

    /**
     * 测试无效范围参数
     * <p>
     * 尝试设置最大单位小于最小单位 (秒 < 小时)
     * 预期: 抛出 IllegalArgumentException
     */
    @Test
    void testInvalidRange() {
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.builder()
                        .setRange(ChronoUnit.SECONDS, ChronoUnit.HOURS)
        );
    }

    /**
     * 测试单独设置单位时 build() 兜底校验区间方向
     * <p>
     * setRange 会立即校验, 但单独调用 setLargestUnit/setSmallestUnit 时无法在设值点判断.
     * 若不在 build() 兜底, 反向区间会让所有单位都被排除, 时长被静默丢弃
     * (如只设 smallestUnit=DAYS 时 10 天会格式化成 "0d")
     */
    @Test
    void testInvalidRangeViaIndividualSettersRejectedAtBuild() {
        // 只设 smallestUnit, largestUnit 保持默认 HOURS, 形成 HOURS < DAYS 的反向区间
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.builder().setSmallestUnit(ChronoUnit.DAYS).build());

        // 只设 largestUnit 到 smallestUnit 默认值 SECONDS 之下
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.builder().setLargestUnit(ChronoUnit.MILLIS).build());

        // 先设合法区间, 再用单个 setter 破坏它
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.builder()
                        .setRange(ChronoUnit.HOURS, ChronoUnit.MINUTES)
                        .setSmallestUnit(ChronoUnit.DAYS)
                        .build());
    }

    /**
     * 测试不受支持的单位被拒绝
     * <p>
     * getUnitOrder 对未知单位返回 0, 会让区间判断失去意义:
     * largestUnit 非法时结果恒为 "0s", smallestUnit 非法时整点时长会渲染出 "1h 5ns"
     */
    @Test
    void testUnsupportedUnitRejected() {
        for (ChronoUnit unit : new ChronoUnit[]{
                ChronoUnit.WEEKS, ChronoUnit.DECADES, ChronoUnit.CENTURIES,
                ChronoUnit.HALF_DAYS, ChronoUnit.FOREVER, ChronoUnit.ERAS, ChronoUnit.MILLENNIA}) {
            assertThrows(IllegalArgumentException.class,
                    () -> DurationFormatter.builder().setLargestUnit(unit).build(),
                    () -> "largestUnit=" + unit + " should be rejected");
            assertThrows(IllegalArgumentException.class,
                    () -> DurationFormatter.builder().setSmallestUnit(unit).build(),
                    () -> "smallestUnit=" + unit + " should be rejected");
        }
    }

    /**
     * 测试纳秒换算溢出时抛异常而非静默返回错误结果
     * <p>
     * largestUnit 为 MILLIS 及以下时, totalSeconds 保留完整秒数,
     * 乘 1e9 在约 292 年后溢出. 溢出必须抛出, 与 NANOS 单一单位特例路径
     * (duration.toNanos() 同样抛 ArithmeticException) 保持一致
     */
    @Test
    void testNanosOverflowThrows() {
        Duration threeHundredYears = Duration.ofDays(365L * 300);

        assertThrows(ArithmeticException.class, () ->
                DurationFormatter.builder()
                        .setRange(ChronoUnit.MILLIS, ChronoUnit.NANOS)
                        .build()
                        .format(threeHundredYears));

        // 对照: NANOS 单一单位走特例路径, 同样抛出
        assertThrows(ArithmeticException.class, () ->
                DurationFormatter.builder()
                        .setRange(ChronoUnit.NANOS, ChronoUnit.NANOS)
                        .build()
                        .format(threeHundredYears));

        // 正常范围内不受影响
        assertEquals("1500ms", DurationFormatter.builder()
                .setRange(ChronoUnit.MILLIS, ChronoUnit.NANOS)
                .build()
                .format(Duration.ofMillis(1500)));
    }

    /**
     * 测试负时长异常
     * <p>
     * 尝试格式化负数时长: -100秒
     * 预期: 抛出 IllegalArgumentException
     */
    @Test
    void testNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                defaultFormatter.format(Duration.ofSeconds(-100))
        );
    }

    /**
     * 测试 null 时长异常
     * <p>
     * 尝试传入 null
     * 预期: 抛出 IllegalArgumentException
     */
    @Test
    void testNullDuration() {
        assertThrows(IllegalArgumentException.class, () ->
                defaultFormatter.format(null)
        );
    }

    /**
     * 测试复杂时长格式化
     * <p>
     * 输入: 1天2小时3分钟4秒500毫秒
     * 范围: 天到毫秒
     * 预期输出: 1d 2h 3min 4s 500ms
     */
    @Test
    void testComplexFormat() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.DAYS, ChronoUnit.MILLIS)
                .build();
        long millis = (1L * 24 * 3600 + 2 * 3600 + 3 * 60 + 4) * 1000 + 500;
        String result = formatter.format(Duration.ofMillis(millis));
        assertEquals("1d 2h 3min 4s 500ms", result);
    }

    /**
     * 测试秒到纳秒的完整精度格式化 (英文)
     * <p>
     * 输入: 1_123_456_789 纳秒 = 1秒 + 123毫秒 + 456微秒 + 789纳秒
     * 范围: 秒到纳秒
     * 预期输出: 1s 123ms 456μs 789ns
     */
    @Test
    void testSecondsToNanosRange() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.SECONDS, ChronoUnit.NANOS)
                .build();
        long nanos = 1_123_456_789L;
        String result = formatter.format(Duration.ofNanos(nanos));
        assertEquals("1s 123ms 456μs 789ns", result);
    }

    /**
     * 测试秒到纳秒的完整精度格式化 (中文)
     * <p>
     * 输入: 1_123_456_789 纳秒
     * 范围: 秒到纳秒
     * 预期输出: 1秒 123毫秒 456微秒 789纳秒
     */
    @Test
    void testSecondsToNanosRangeChinese() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.SECONDS, ChronoUnit.NANOS)
                .setChinese()
                .build();
        long nanos = 1_123_456_789L;
        String result = formatter.format(Duration.ofNanos(nanos));
        assertEquals("1秒 123毫秒 456微秒 789纳秒", result);
    }

    /**
     * 测试小时到分钟的范围限制
     * <p>
     * 输入: 3665 秒
     * 范围: 小时到分钟
     * 预期输出: 1h 1min (不显示秒)
     */
    @Test
    void testRangeHoursToMinutes() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.HOURS, ChronoUnit.MINUTES)
                .build();
        String result = formatter.format(Duration.ofSeconds(3665));
        assertEquals("1h 1min", result);
    }

    /**
     * 测试秒到秒的范围限制
     * <p>
     * 输入: 3665 秒
     * 范围: 秒到秒
     * 预期输出: 3665s
     */
    @Test
    void testRangeSecondsToSeconds() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.SECONDS, ChronoUnit.SECONDS)
                .build();
        String result = formatter.format(Duration.ofSeconds(3665));
        assertEquals("3665s", result);
    }

    /**
     * 测试大数值格式化
     * <p>
     * 输入: 10年的秒数
     * 范围: 年到天
     * 预期输出: 10y
     */
    @Test
    void testLargeValue() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setRange(ChronoUnit.YEARS, ChronoUnit.DAYS)
                .build();
        long seconds = 10L * 365 * 24 * 3600;
        String result = formatter.format(Duration.ofSeconds(seconds));
        assertEquals("10y", result);
    }

    /**
     * 测试 Formatter 可复用
     * <p>
     * 验证同一个 DurationFormatter 实例可以格式化不同的 Duration
     */
    @Test
    void testFormatterReuse() {
        DurationFormatter formatter = DurationFormatter.builder()
                .setChinese()
                .build();
        assertEquals("1小时 1分钟 1秒", formatter.format(Duration.ofSeconds(3661)));
        assertEquals("1分钟 30秒", formatter.format(Duration.ofSeconds(90)));
        assertEquals("0秒", formatter.format(Duration.ZERO));
    }

    /**
     * 测试默认 Formatter 的多种时长
     * <p>
     * 验证默认配置 (小时到秒, 英文) 对多种输入的正确性
     */
    @Test
    void testDefaultFormatterVariousDurations() {
        assertEquals("5min", defaultFormatter.format(Duration.ofMinutes(5)));
        assertEquals("5h", defaultFormatter.format(Duration.ofHours(5)));
        assertEquals("120h", defaultFormatter.format(Duration.ofDays(5)));
    }
}
