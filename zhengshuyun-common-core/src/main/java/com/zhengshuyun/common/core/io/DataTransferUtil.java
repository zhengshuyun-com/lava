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

import com.google.common.base.Stopwatch;
import com.zhengshuyun.common.core.lang.Validate;
import com.zhengshuyun.common.core.time.DurationFormatter;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 数据传输工具类 - 字节大小格式化、百分比计算、时长格式化等
 *
 * @author Toint
 * @since 2026/1/11
 */
public final class DataTransferUtil {

    private DataTransferUtil() {
    }

    /**
     * 格式化字节大小为人类可读格式
     *
     * @param bytes 字节数 (必须>=0)
     * @return 格式化字符串 (如 "1.50 MB", "128 KB")
     */
    public static String formatBytes(long bytes) {
        Validate.isTrue(bytes >= 0, "bytes cannot be negative");

        if (bytes < 1024) {
            return bytes + " B";
        }

        final String[] units = {"KB", "MB", "GB", "TB", "PB", "EB"};
        int unitIndex = (int) (Math.log(bytes) / Math.log(1024)) - 1;
        double value = bytes / Math.pow(1024, unitIndex + 1);

        String format = value >= 100 ? "%.0f %s" : (value >= 10 ? "%.1f %s" : "%.2f %s");
        return String.format(format, value, units[unitIndex]);
    }

    /**
     * 计算百分比
     *
     * @param current 当前值 (必须>=0)
     * @param total   总值 (-1 或 <=0 表示未知)
     * @return 百分比 (0-100), 未知时返回 null
     */
    public static @Nullable Integer calculatePercentage(long current, long total) {
        Validate.isTrue(current >= 0, "current cannot be negative");
        if (total <= 0) {
            return null;
        }
        // 使用分解计算避免 current * 100 溢出
        long q = current / total;
        long r = current % total;
        return (int) Math.min(100, q * 100 + r * 100 / total);
    }

    /**
     * 格式化时长
     *
     * @param millis 毫秒数
     * @return 时长字符串 (如 "1h 23m", "45s", "1m 30s")
     * @see DurationFormatter
     */
    public static String formatDuration(long millis) {
        return formatDuration(Duration.ofMillis(millis));
    }

    /**
     * 格式化时长
     *
     * @param duration 时长
     * @return 时长字符串 (如 "1h 23m", "45s", "1m 30s")
     * @see DurationFormatter
     */
    public static String formatDuration(Duration duration) {
        return DurationFormatter.of(duration)
                .setRange(ChronoUnit.HOURS, ChronoUnit.SECONDS)
                .setEnglish()
                .format();
    }

    /**
     * 创建进度追踪器
     *
     * @param totalBytes 总大小 (-1 表示未知)
     */
    public static Tracker tracker(long totalBytes) {
        return new Tracker(totalBytes);
    }

    /**
     * 格式化当前速率
     *
     * @param currentBytes  当前已读取字节数
     * @param elapsedMillis 已用时 (毫秒)
     * @return 速率字符串 (如 "2.30 MB/s")
     */
    public static String formatSpeed(long currentBytes, long elapsedMillis) {
        if (elapsedMillis <= 0) {
            return "unknown/s";
        }
        double bytesPerSecond = (double) currentBytes / elapsedMillis * 1000;
        return DataTransferUtil.formatBytes((long) bytesPerSecond) + "/s";
    }

    /**
     * 进度追踪器
     */
    public static final class Tracker {
        /**
         * 计时器
         */
        private final Stopwatch stopwatch;

        /**
         * 总字节大小
         */
        private final long totalBytes;

        private Tracker(long totalBytes) {
            this.stopwatch = Stopwatch.createStarted();
            this.totalBytes = totalBytes;
        }

        /**
         * 格式化当前进度 (包含大小、百分比、速率、剩余时间) 
         *
         * @param currentBytes 当前已读取字节数
         * @return 进度字符串 (如 "5.50 MB / 10.00 MB (55%) - 2.30 MB/s - 剩余: 2m 15s")
         */
        public String format(long currentBytes) {
            long elapsedMillis = getElapsedMillis();
            return formatProgress(currentBytes, elapsedMillis);
        }

        /**
         * 获取当前速率
         *
         * @param currentBytes 当前已读取字节数
         * @return 速率字符串 (如 "2.30 MB/s")
         */
        public String getSpeed(long currentBytes) {
            long elapsedMillis = getElapsedMillis();
            return formatSpeed(currentBytes, elapsedMillis);
        }

        /**
         * 获取剩余时间
         *
         * @param currentBytes 当前已读取字节数
         * @return 剩余时间字符串 (如 "2m 15s"), 无法计算时返回 null
         */
        public @Nullable String getRemainingTime(long currentBytes) {
            long elapsedMillis = getElapsedMillis();
            Long remaining = calculateRemainingTime(currentBytes, elapsedMillis);
            return remaining != null ? DataTransferUtil.formatDuration(remaining) : null;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        private String formatProgress(long currentBytes, long elapsedMillis) {
            StringBuilder sb = new StringBuilder();

            // 基础进度: "5.50 MB / 10.00 MB (55%)"
            sb.append(DataTransferUtil.formatBytes(currentBytes));
            if (totalBytes > 0) {
                sb.append(" / ").append(DataTransferUtil.formatBytes(totalBytes));
                Integer percentage = DataTransferUtil.calculatePercentage(currentBytes, totalBytes);
                if (percentage != null) {
                    sb.append(String.format(" (%d%%)", percentage));
                }
            } else {
                sb.append(" / unknown");
            }

            // 速率: "- 2.30 MB/s"
            sb.append(" - ").append(formatSpeed(currentBytes, elapsedMillis));

            // 剩余时间: "- 剩余: 2m 15s"
            if (totalBytes > 0) {
                Long remaining = calculateRemainingTime(currentBytes, elapsedMillis);
                if (remaining != null && remaining > 0) {
                    sb.append(" - 剩余: ").append(DataTransferUtil.formatDuration(remaining));
                }
            }

            return sb.toString();
        }

        private @Nullable Long calculateRemainingTime(long currentBytes, long elapsedMillis) {
            if (totalBytes <= 0 || currentBytes <= 0 || elapsedMillis <= 0) {
                return null;
            }
            if (currentBytes >= totalBytes) {
                return 0L;
            }

            long remaining = totalBytes - currentBytes;
            return (long) ((double) remaining / currentBytes * elapsedMillis);
        }

        private long getElapsedMillis() {
            return Math.max(1, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}