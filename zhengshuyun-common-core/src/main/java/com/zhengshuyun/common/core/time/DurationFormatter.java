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

import com.zhengshuyun.common.core.lang.Validate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 时长格式化器
 * <p>
 * 不可变对象, 通过 {@link Builder} 构建, 可复用同一配置格式化不同 Duration.
 * <p>
 * 示例:
 * <pre>{@code
 * DurationFormatter formatter = DurationFormatter.builder()
 *     .setLargestUnit(ChronoUnit.HOURS)
 *     .setChinese()
 *     .build();
 *
 * formatter.format(Duration.ofSeconds(3661)); // "1时 1分 1秒"
 * formatter.format(Duration.ofSeconds(90));   // "1分 30秒"
 * }</pre>
 *
 * @author Toint
 * @since 2026/1/11
 */
public final class DurationFormatter {

    /**
     * 最大单位
     */
    private final ChronoUnit largestUnit;

    /**
     * 最小单位
     */
    private final ChronoUnit smallestUnit;

    /**
     * 语言环境
     */
    private final Locale locale;

    /**
     * 是否显示零值单位
     */
    private final boolean showZeroValues;

    /**
     * 单位之间的分隔符
     */
    private final String separator;

    private DurationFormatter(Builder builder) {
        this.largestUnit = builder.largestUnit;
        this.smallestUnit = builder.smallestUnit;
        this.locale = builder.locale;
        this.showZeroValues = builder.showZeroValues;
        this.separator = builder.separator;
    }

    /**
     * 创建 Builder 实例
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 格式化时长
     *
     * <p><b>注意</b>: 年和月的计算是近似值:
     * <ul>
     *   <li>1年 = 365天 (不考虑闰年) </li>
     *   <li>1月 = 30天 (不考虑实际天数) </li>
     * </ul>
     *
     * @param duration 时长 (不能为 null 或负数)
     * @return 格式化后的字符串
     */
    public String format(Duration duration) {
        Validate.notNull(duration, "duration cannot be null");
        Validate.isFalse(duration.isNegative(), "duration cannot be negative");

        // 特殊处理: 只显示纳秒
        if (largestUnit == ChronoUnit.NANOS && smallestUnit == ChronoUnit.NANOS) {
            return duration.toNanos() + getUnitSuffix(ChronoUnit.NANOS);
        }

        // 特殊处理: 只显示微秒
        if (largestUnit == ChronoUnit.MICROS && smallestUnit == ChronoUnit.MICROS) {
            return duration.toNanos() / 1000 + getUnitSuffix(ChronoUnit.MICROS);
        }

        // 特殊处理: 只显示毫秒
        if (largestUnit == ChronoUnit.MILLIS && smallestUnit == ChronoUnit.MILLIS) {
            return duration.toMillis() + getUnitSuffix(ChronoUnit.MILLIS);
        }

        List<String> parts = new ArrayList<>();

        // 计算各个单位的值
        long totalSeconds = duration.getSeconds();

        long years = 0, months = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
        long millis = 0, micros = 0, nanos = 0;

        // 从大到小依次计算
        if (shouldInclude(ChronoUnit.YEARS)) {
            years = totalSeconds / (365L * 24 * 3600);
            totalSeconds %= (365L * 24 * 3600);
        }

        if (shouldInclude(ChronoUnit.MONTHS)) {
            months = totalSeconds / (30L * 24 * 3600);
            totalSeconds %= (30L * 24 * 3600);
        }

        if (shouldInclude(ChronoUnit.DAYS)) {
            days = totalSeconds / (24 * 3600);
            totalSeconds %= (24 * 3600);
        }

        if (shouldInclude(ChronoUnit.HOURS)) {
            hours = totalSeconds / 3600;
            totalSeconds %= 3600;
        }

        if (shouldInclude(ChronoUnit.MINUTES)) {
            minutes = totalSeconds / 60;
            totalSeconds %= 60;
        }

        if (shouldInclude(ChronoUnit.SECONDS)) {
            seconds = totalSeconds;
            totalSeconds = 0;
        }

        // 计算毫秒/微秒/纳秒
        // 如果 SECONDS 不在范围内, 需要将未消费的秒数转换为纳秒
        long remainingNanos = totalSeconds * 1_000_000_000L + duration.toNanosPart();

        if (shouldInclude(ChronoUnit.MILLIS)) {
            millis = remainingNanos / 1_000_000;
            remainingNanos %= 1_000_000;
        }

        if (shouldInclude(ChronoUnit.MICROS)) {
            micros = remainingNanos / 1_000;
            remainingNanos %= 1_000;
        }

        if (shouldInclude(ChronoUnit.NANOS)) {
            nanos = remainingNanos;
        }

        // 添加各个单位到结果
        addPart(parts, years, ChronoUnit.YEARS);
        addPart(parts, months, ChronoUnit.MONTHS);
        addPart(parts, days, ChronoUnit.DAYS);
        addPart(parts, hours, ChronoUnit.HOURS);
        addPart(parts, minutes, ChronoUnit.MINUTES);
        addPart(parts, seconds, ChronoUnit.SECONDS);
        addPart(parts, millis, ChronoUnit.MILLIS);
        addPart(parts, micros, ChronoUnit.MICROS);
        addPart(parts, nanos, ChronoUnit.NANOS);

        return parts.isEmpty() ? "0" + getUnitSuffix(smallestUnit) : String.join(separator, parts);
    }

    private boolean shouldInclude(ChronoUnit unit) {
        int unitOrder = getUnitOrder(unit);
        int largestOrder = getUnitOrder(largestUnit);
        int smallestOrder = getUnitOrder(smallestUnit);
        return unitOrder >= smallestOrder && unitOrder <= largestOrder;
    }

    private void addPart(List<String> parts, long value, ChronoUnit unit) {
        if (!shouldInclude(unit)) {
            return;
        }
        if (value > 0 || showZeroValues) {
            parts.add(value + getUnitSuffix(unit));
        }
    }

    private String getUnitSuffix(ChronoUnit unit) {
        if (locale.equals(Locale.CHINESE) || locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            return switch (unit) {
                case YEARS -> "年";
                case MONTHS -> "月";
                case DAYS -> "天";
                case HOURS -> "小时";
                case MINUTES -> "分钟";
                case SECONDS -> "秒";
                case MILLIS -> "毫秒";
                case MICROS -> "微秒";
                case NANOS -> "纳秒";
                default -> "";
            };
        } else {
            return switch (unit) {
                case YEARS -> "y";
                case MONTHS -> "mo";
                case DAYS -> "d";
                case HOURS -> "h";
                case MINUTES -> "min";
                case SECONDS -> "s";
                case MILLIS -> "ms";
                case MICROS -> "μs";
                case NANOS -> "ns";
                default -> "";
            };
        }
    }

    private static int getUnitOrder(ChronoUnit unit) {
        return switch (unit) {
            case YEARS -> 9;
            case MONTHS -> 8;
            case DAYS -> 7;
            case HOURS -> 6;
            case MINUTES -> 5;
            case SECONDS -> 4;
            case MILLIS -> 3;
            case MICROS -> 2;
            case NANOS -> 1;
            default -> 0;
        };
    }

    /**
     * 时长格式化器构建器
     *
     * @author Toint
     * @since 2026/1/11
     */
    public static final class Builder {

        /**
         * 最大单位 (默认: 小时)
         */
        private ChronoUnit largestUnit = ChronoUnit.HOURS;

        /**
         * 最小单位 (默认: 秒)
         */
        private ChronoUnit smallestUnit = ChronoUnit.SECONDS;

        /**
         * 语言环境 (默认: 英文)
         */
        private Locale locale = Locale.ENGLISH;

        /**
         * 是否显示零值单位 (默认: false)
         */
        private boolean showZeroValues = false;

        /**
         * 单位之间的分隔符 (默认: 空格)
         */
        private String separator = " ";

        private Builder() {
        }

        /**
         * 设置最大单位
         *
         * @param largestUnit 最大单位 (YEARS/MONTHS/DAYS/HOURS/MINUTES/SECONDS/MILLIS/MICROS/NANOS)
         */
        public Builder setLargestUnit(ChronoUnit largestUnit) {
            this.largestUnit = Validate.notNull(largestUnit, "largestUnit cannot be null");
            return this;
        }

        /**
         * 设置最小单位
         *
         * @param smallestUnit 最小单位 (YEARS/MONTHS/DAYS/HOURS/MINUTES/SECONDS/MILLIS/MICROS/NANOS)
         */
        public Builder setSmallestUnit(ChronoUnit smallestUnit) {
            this.smallestUnit = Validate.notNull(smallestUnit, "smallestUnit cannot be null");
            return this;
        }

        /**
         * 设置单位范围
         *
         * @param largestUnit  最大单位
         * @param smallestUnit 最小单位
         * @throws IllegalArgumentException 如果 largestUnit < smallestUnit 或单位不支持
         */
        public Builder setRange(ChronoUnit largestUnit, ChronoUnit smallestUnit) {
            Validate.notNull(largestUnit, "largestUnit cannot be null");
            Validate.notNull(smallestUnit, "smallestUnit cannot be null");

            int largestOrder = getUnitOrder(largestUnit);
            int smallestOrder = getUnitOrder(smallestUnit);

            Validate.isTrue(largestOrder > 0 && smallestOrder > 0,
                    "Unsupported unit: only YEARS/MONTHS/DAYS/HOURS/MINUTES/SECONDS/MILLIS/MICROS/NANOS are supported");
            Validate.isTrue(largestOrder >= smallestOrder,
                    "largestUnit must be >= smallestUnit");

            return setLargestUnit(largestUnit).setSmallestUnit(smallestUnit);
        }

        /**
         * 设置语言 (中文/英文等)
         */
        public Builder setLocale(Locale locale) {
            this.locale = Validate.notNull(locale, "locale cannot be null");
            return this;
        }

        /**
         * 设置为中文
         */
        public Builder setChinese() {
            return setLocale(Locale.CHINESE);
        }

        /**
         * 设置为英文
         */
        public Builder setEnglish() {
            return setLocale(Locale.ENGLISH);
        }

        /**
         * 设置是否显示零值单位
         *
         * @param showZeroValues true: "1h 0m 30s", false: "1h 30s"
         */
        public Builder setShowZeroValues(boolean showZeroValues) {
            this.showZeroValues = showZeroValues;
            return this;
        }

        /**
         * 设置单位之间的分隔符
         */
        public Builder setSeparator(String separator) {
            this.separator = Validate.notNull(separator, "separator cannot be null");
            return this;
        }

        /**
         * 构建 DurationFormatter 实例
         *
         * @return 不可变的 DurationFormatter 实例
         */
        public DurationFormatter build() {
            return new DurationFormatter(this);
        }
    }
}
