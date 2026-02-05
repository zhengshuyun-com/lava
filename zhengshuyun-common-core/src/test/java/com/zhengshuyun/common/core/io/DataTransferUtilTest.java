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

package com.zhengshuyun.common.core.io;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataTransferUtil 单元测试
 * 测试数据传输工具类的字节格式化、百分比计算、速率计算等功能
 *
 * @author Toint
 * @since 2026/01/18
 */
class DataTransferUtilTest {

    // formatBytes 测试

    /**
     * 测试格式化字节 - 小于 1KB
     */
    @Test
    void testFormatBytesLessThan1KB() {
        assertEquals("0 B", DataTransferUtil.formatBytes(0));
        assertEquals("1 B", DataTransferUtil.formatBytes(1));
        assertEquals("512 B", DataTransferUtil.formatBytes(512));
        assertEquals("1023 B", DataTransferUtil.formatBytes(1023));
    }

    /**
     * 测试格式化字节 - KB 级别
     */
    @Test
    void testFormatBytesKB() {
        assertEquals("1.00 KB", DataTransferUtil.formatBytes(1024));
        assertEquals("1.50 KB", DataTransferUtil.formatBytes(1536));
        assertEquals("10.0 KB", DataTransferUtil.formatBytes(10240));
        assertEquals("100 KB", DataTransferUtil.formatBytes(102400));
    }

    /**
     * 测试格式化字节 - MB 级别
     */
    @Test
    void testFormatBytesMB() {
        assertEquals("1.00 MB", DataTransferUtil.formatBytes(1024 * 1024));
        assertEquals("1.50 MB", DataTransferUtil.formatBytes(1024 * 1024 + 512 * 1024));
        assertEquals("10.0 MB", DataTransferUtil.formatBytes(10 * 1024 * 1024));
        assertEquals("100 MB", DataTransferUtil.formatBytes(100 * 1024 * 1024));
    }

    /**
     * 测试格式化字节 - GB 级别
     */
    @Test
    void testFormatBytesGB() {
        assertEquals("1.00 GB", DataTransferUtil.formatBytes(1024L * 1024 * 1024));
        assertEquals("5.50 GB", DataTransferUtil.formatBytes(5L * 1024 * 1024 * 1024 + 512L * 1024 * 1024));
        assertEquals("100 GB", DataTransferUtil.formatBytes(100L * 1024 * 1024 * 1024));
    }

    /**
     * 测试格式化字节 - TB 级别
     */
    @Test
    void testFormatBytesTB() {
        assertEquals("1.00 TB", DataTransferUtil.formatBytes(1024L * 1024 * 1024 * 1024));
        assertEquals("10.0 TB", DataTransferUtil.formatBytes(10L * 1024 * 1024 * 1024 * 1024));
    }

    /**
     * 测试格式化字节 - PB 级别
     */
    @Test
    void testFormatBytesPB() {
        assertEquals("1.00 PB", DataTransferUtil.formatBytes(1024L * 1024 * 1024 * 1024 * 1024));
    }

    /**
     * 测试格式化字节 - EB 级别
     */
    @Test
    void testFormatBytesEB() {
        assertEquals("1.00 EB", DataTransferUtil.formatBytes(1024L * 1024 * 1024 * 1024 * 1024 * 1024));
    }

    /**
     * 测试格式化字节 - Long.MAX_VALUE
     */
    @Test
    void testFormatBytesMaxValue() {
        // Long.MAX_VALUE = 9,223,372,036,854,775,807 bytes ≈ 8 EB
        String result = DataTransferUtil.formatBytes(Long.MAX_VALUE);
        assertTrue(result.endsWith("EB"));
        assertTrue(result.startsWith("8"));
    }

    /**
     * 测试格式化字节 - 负数应抛异常
     */
    @Test
    void testFormatBytesNegative() {
        assertThrows(IllegalArgumentException.class, () -> DataTransferUtil.formatBytes(-1));
        assertThrows(IllegalArgumentException.class, () -> DataTransferUtil.formatBytes(-1024));
    }

    /**
     * 测试格式化字节 - 精度格式
     */
    @Test
    void testFormatBytesPrecision() {
        // < 10: 两位小数
        assertEquals("1.23 KB", DataTransferUtil.formatBytes(1260));
        // >= 10 且 < 100: 一位小数
        assertEquals("12.3 KB", DataTransferUtil.formatBytes(12595));
        // >= 100: 无小数
        assertEquals("123 KB", DataTransferUtil.formatBytes(125952));
    }

    // calculatePercentage 测试

    /**
     * 测试计算百分比 - 正常情况
     */
    @Test
    void testCalculatePercentageNormal() {
        assertEquals(0, DataTransferUtil.calculatePercentage(0, 100));
        assertEquals(25, DataTransferUtil.calculatePercentage(25, 100));
        assertEquals(50, DataTransferUtil.calculatePercentage(50, 100));
        assertEquals(75, DataTransferUtil.calculatePercentage(75, 100));
        assertEquals(100, DataTransferUtil.calculatePercentage(100, 100));
    }

    /**
     * 测试计算百分比 - 超过 100%
     */
    @Test
    void testCalculatePercentageOver100() {
        assertEquals(100, DataTransferUtil.calculatePercentage(150, 100));
        assertEquals(100, DataTransferUtil.calculatePercentage(200, 100));
    }

    /**
     * 测试计算百分比 - 未知总大小
     */
    @Test
    void testCalculatePercentageUnknownTotal() {
        assertNull(DataTransferUtil.calculatePercentage(50, -1));
        assertNull(DataTransferUtil.calculatePercentage(50, 0));
    }

    /**
     * 测试计算百分比 - 大数值
     */
    @Test
    void testCalculatePercentageLargeNumbers() {
        long total = 1024L * 1024 * 1024 * 10; // 10 GB
        long current = 1024L * 1024 * 1024 * 5; // 5 GB
        assertEquals(50, DataTransferUtil.calculatePercentage(current, total));
    }

    /**
     * 测试计算百分比 - 极大数值 (潜在溢出测试)
     */
    @Test
    void testCalculatePercentageVeryLargeNumbers() {
        // 测试接近 Long.MAX_VALUE 的情况
        long total = Long.MAX_VALUE;
        long current = Long.MAX_VALUE / 2;
        Integer percentage = DataTransferUtil.calculatePercentage(current, total);
        assertNotNull(percentage);
        // 由于整数除法,可能不完全准确,但应该在合理范围内
        assertTrue(percentage >= 0 && percentage <= 100, "Percentage should be between 0 and 100, got: " + percentage);
    }

    /**
     * 测试计算百分比 - 负数应抛异常
     */
    @Test
    void testCalculatePercentageNegativeCurrent() {
        assertThrows(IllegalArgumentException.class, () -> 
            DataTransferUtil.calculatePercentage(-1, 100));
    }

    /**
     * 测试计算百分比 - 小数精度
     */
    @Test
    void testCalculatePercentagePrecision() {
        assertEquals(33, DataTransferUtil.calculatePercentage(1, 3)); // 33.33% -> 33
        assertEquals(66, DataTransferUtil.calculatePercentage(2, 3)); // 66.66% -> 66
        assertEquals(99, DataTransferUtil.calculatePercentage(99, 100)); // 99%
        assertEquals(1, DataTransferUtil.calculatePercentage(1, 100)); // 1%
    }

    // formatDuration 测试

    /**
     * 测试格式化时长 - 毫秒
     */
    @Test
    void testFormatDurationMillis() {
        assertEquals("1s", DataTransferUtil.formatDuration(1000L));
        assertEquals("30s", DataTransferUtil.formatDuration(30000L));
        assertEquals("59s", DataTransferUtil.formatDuration(59000L));
    }

    /**
     * 测试格式化时长 - 分钟
     */
    @Test
    void testFormatDurationMinutes() {
        assertTrue(DataTransferUtil.formatDuration(60000L).matches("1m(in)?"));
        assertTrue(DataTransferUtil.formatDuration(90000L).matches("1m(in)? 30s"));
        assertTrue(DataTransferUtil.formatDuration(300000L).matches("5m(in)?"));
    }

    /**
     * 测试格式化时长 - 小时
     */
    @Test
    void testFormatDurationHours() {
        assertTrue(DataTransferUtil.formatDuration(3600000L).matches("1h"));
        assertTrue(DataTransferUtil.formatDuration(5400000L).matches("1h 30m(in)?"));
        assertTrue(DataTransferUtil.formatDuration(8130000L).matches("2h 15m(in)? 30s"));
    }

    /**
     * 测试格式化时长 - Duration 对象
     */
    @Test
    void testFormatDurationObject() {
        assertEquals("1s", DataTransferUtil.formatDuration(Duration.ofSeconds(1)));
        assertTrue(DataTransferUtil.formatDuration(Duration.ofMinutes(1)).matches("1m(in)?"));
        assertTrue(DataTransferUtil.formatDuration(Duration.ofHours(1)).matches("1h"));
    }

    /**
     * 测试格式化时长 - 零时长
     */
    @Test
    void testFormatDurationZero() {
        String result = DataTransferUtil.formatDuration(0L);
        assertNotNull(result);
    }

    // formatSpeed 测试

    /**
     * 测试格式化速率 - 正常情况
     */
    @Test
    void testFormatSpeedNormal() {
        // 1 MB in 1 second = 1 MB/s
        assertEquals("1.00 MB/s", DataTransferUtil.formatSpeed(1024 * 1024, 1000));
        
        // 10 MB in 2 seconds = 5 MB/s
        assertEquals("5.00 MB/s", DataTransferUtil.formatSpeed(10 * 1024 * 1024, 2000));
    }

    /**
     * 测试格式化速率 - 高速
     */
    @Test
    void testFormatSpeedHigh() {
        // 100 MB in 1 second = 100 MB/s
        assertEquals("100 MB/s", DataTransferUtil.formatSpeed(100L * 1024 * 1024, 1000));
    }

    /**
     * 测试格式化速率 - 低速
     */
    @Test
    void testFormatSpeedLow() {
        // 1 KB in 1 second = 1 KB/s
        assertEquals("1.00 KB/s", DataTransferUtil.formatSpeed(1024, 1000));
    }

    /**
     * 测试格式化速率 - 零时间
     */
    @Test
    void testFormatSpeedZeroTime() {
        assertEquals("unknown/s", DataTransferUtil.formatSpeed(1024, 0));
        assertEquals("unknown/s", DataTransferUtil.formatSpeed(1024, -1));
    }

    /**
     * 测试格式化速率 - 零字节
     */
    @Test
    void testFormatSpeedZeroBytes() {
        assertEquals("0 B/s", DataTransferUtil.formatSpeed(0, 1000));
    }

    // Tracker 测试

    /**
     * 测试创建追踪器
     */
    @Test
    void testTrackerCreation() {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        assertNotNull(tracker);
        assertEquals(1024 * 1024, tracker.getTotalBytes());
    }

    /**
     * 测试追踪器 - 未知总大小
     */
    @Test
    void testTrackerUnknownSize() {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(-1);
        assertEquals(-1, tracker.getTotalBytes());
    }

    /**
     * 测试追踪器格式化进度 - 已知大小
     */
    @Test
    void testTrackerFormatWithKnownSize() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        Thread.sleep(10); // 确保有经过时间
        
        String progress = tracker.format(512 * 1024);
        
        // 应包含: 当前大小, 总大小, 百分比, 速率
        assertTrue(progress.contains("KB"));
        assertTrue(progress.contains("MB"));
        assertTrue(progress.contains("%"));
        assertTrue(progress.contains("/s"));
    }

    /**
     * 测试追踪器格式化进度 - 未知大小
     */
    @Test
    void testTrackerFormatWithUnknownSize() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(-1);
        Thread.sleep(10);
        
        String progress = tracker.format(512 * 1024);
        
        assertTrue(progress.contains("KB"));
        assertTrue(progress.contains("unknown"));
        assertTrue(progress.contains("/s"));
        assertFalse(progress.contains("%")); // 未知大小不显示百分比
    }

    /**
     * 测试追踪器获取速率
     */
    @Test
    void testTrackerGetSpeed() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        Thread.sleep(10);
        
        String speed = tracker.getSpeed(512 * 1024);
        assertTrue(speed.endsWith("/s"));
        assertTrue(speed.contains("B/s") || speed.contains("KB/s") || speed.contains("MB/s"));
    }

    /**
     * 测试追踪器获取剩余时间 - 已知大小
     */
    @Test
    void testTrackerGetRemainingTimeKnownSize() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        Thread.sleep(10);
        
        String remaining = tracker.getRemainingTime(512 * 1024); // 50% 完成
        // 应该返回时间字符串
        if (remaining != null) {
            assertTrue(remaining.matches(".*\\d+.*")); // 包含数字
        }
    }

    /**
     * 测试追踪器获取剩余时间 - 未知大小
     */
    @Test
    void testTrackerGetRemainingTimeUnknownSize() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(-1);
        Thread.sleep(10);
        
        String remaining = tracker.getRemainingTime(512 * 1024);
        assertNull(remaining); // 未知大小无法计算剩余时间
    }

    /**
     * 测试追踪器获取剩余时间 - 已完成
     */
    @Test
    void testTrackerGetRemainingTimeCompleted() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        Thread.sleep(10);
        
        String remaining = tracker.getRemainingTime(1024 * 1024); // 100% 完成
        // 剩余时间应为 0 或 null
        if (remaining != null) {
            assertTrue(remaining.contains("0") || remaining.isEmpty());
        }
    }

    /**
     * 测试追踪器获取剩余时间 - 超过总大小
     */
    @Test
    void testTrackerGetRemainingTimeOverflow() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        Thread.sleep(10);
        
        String remaining = tracker.getRemainingTime(2 * 1024 * 1024); // 超过 100%
        // 应该返回 0 或合理值
        assertNotNull(remaining);
    }

    /**
     * 测试追踪器格式化进度 - 0 字节
     */
    @Test
    void testTrackerFormatZeroBytes() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        Thread.sleep(10);
        
        String progress = tracker.format(0);
        assertTrue(progress.contains("0 B"));
    }

    /**
     * 测试追踪器格式化进度 - 完整进度
     */
    @Test
    void testTrackerFormatFullProgress() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(1024 * 1024);
        Thread.sleep(10);
        
        String progress = tracker.format(1024 * 1024); // 100%
        assertTrue(progress.contains("1.00 MB"));
        assertTrue(progress.contains("100%"));
    }

    /**
     * 测试追踪器 - 实时进度模拟
     */
    @Test
    void testTrackerRealtimeProgress() throws InterruptedException {
        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(10 * 1024 * 1024); // 10 MB
        
        // 模拟分步传输
        Thread.sleep(10);
        String progress1 = tracker.format(2 * 1024 * 1024); // 20%
        assertTrue(progress1.contains("2.00 MB"));
        assertTrue(progress1.contains("10.0 MB"));
        
        Thread.sleep(10);
        String progress2 = tracker.format(5 * 1024 * 1024); // 50%
        assertTrue(progress2.contains("5.00 MB"));
        
        Thread.sleep(10);
        String progress3 = tracker.format(10 * 1024 * 1024); // 100%
        assertTrue(progress3.contains("10.0 MB"));
        assertTrue(progress3.contains("100%"));
    }
}
