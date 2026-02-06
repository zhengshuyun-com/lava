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

package com.zhengshuyun.common.schedule;

import com.zhengshuyun.common.core.lang.Validate;
import com.zhengshuyun.common.core.time.ZoneIds;
import org.quartz.*;

import java.util.Date;
import java.util.TimeZone;

/**
 * 不可变触发器
 * <p>
 * 支持三种互斥模式: cron, interval(固定间隔), delay(延迟一次)
 * <pre>{@code
 * // 固定间隔
 * Trigger trigger = Trigger.builder()
 *     .setInterval(5000)
 *     .setInitialDelay(1000)
 *     .setRepeatCount(10)
 *     .build();
 *
 * // Cron
 * Trigger trigger = Trigger.builder()
 *     .setCron("0 0 2 * * ?")
 *     .build();
 *
 * // 延迟一次
 * Trigger trigger = Trigger.builder()
 *     .setDelay(10000)
 *     .build();
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/6
 */
public final class Trigger {

    /**
     * Cron 表达式
     */
    private final String cron;

    /**
     * 固定间隔(毫秒)
     */
    private final long intervalMillis;

    /**
     * 延迟时间(毫秒), 一次性执行
     */
    private final long delayMillis;

    /**
     * 初始延迟(毫秒), 仅 interval 模式有效
     */
    private final long initialDelayMillis;

    /**
     * 重复次数, -1 表示无限
     */
    private final int repeatCount;

    private Trigger(Builder builder) {
        this.cron = builder.cron;
        this.intervalMillis = builder.intervalMillis;
        this.delayMillis = builder.delayMillis;
        this.initialDelayMillis = builder.initialDelayMillis;
        this.repeatCount = builder.repeatCount;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 转换为 Quartz Trigger
     *
     * @param triggerId 触发器 ID
     * @return Quartz Trigger
     */
    org.quartz.Trigger toQuartzTrigger(String triggerId) {
        if (cron != null) {
            return TriggerBuilder.newTrigger()
                    .withIdentity(triggerId)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                            .inTimeZone(TimeZone.getTimeZone(ZoneIds.UTC)))
                    .build();
        }

        if (intervalMillis > 0) {
            TriggerBuilder<org.quartz.Trigger> tb = TriggerBuilder.newTrigger()
                    .withIdentity(triggerId);

            if (initialDelayMillis > 0) {
                tb.startAt(new Date(System.currentTimeMillis() + initialDelayMillis));
            } else {
                tb.startNow();
            }

            SimpleScheduleBuilder ssb = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(intervalMillis);

            if (repeatCount == -1) {
                ssb.repeatForever();
            } else {
                ssb.withRepeatCount(repeatCount);
            }

            return tb.withSchedule(ssb).build();
        }

        // delay 模式
        Date startTime = new Date(System.currentTimeMillis() + delayMillis);
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerId)
                .startAt(startTime)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(0))
                .build();
    }

    public static final class Builder {

        /**
         * Cron 表达式
         */
        private String cron;

        /**
         * 固定间隔(毫秒)
         */
        private long intervalMillis;

        /**
         * 延迟时间(毫秒), 一次性执行
         */
        private long delayMillis;

        /**
         * 初始延迟(毫秒), 仅 interval 模式有效
         */
        private long initialDelayMillis;

        /**
         * 重复次数, -1 表示无限
         */
        private int repeatCount = -1;

        private Builder() {
        }

        /**
         * 设置 Cron 表达式
         *
         * @param cron Cron 表达式
         * @return this
         */
        public Builder setCron(String cron) {
            this.cron = cron;
            return this;
        }

        /**
         * 设置固定间隔(毫秒)
         *
         * @param intervalMillis 间隔时间(毫秒)
         * @return this
         */
        public Builder setInterval(long intervalMillis) {
            this.intervalMillis = intervalMillis;
            return this;
        }

        /**
         * 设置延迟时间(毫秒), 一次性执行
         *
         * @param delayMillis 延迟时间(毫秒)
         * @return this
         */
        public Builder setDelay(long delayMillis) {
            this.delayMillis = delayMillis;
            return this;
        }

        /**
         * 设置初始延迟(毫秒), 仅 interval 模式有效
         *
         * @param initialDelayMillis 初始延迟(毫秒)
         * @return this
         */
        public Builder setInitialDelay(long initialDelayMillis) {
            this.initialDelayMillis = initialDelayMillis;
            return this;
        }

        /**
         * 设置重复次数, 仅 interval 模式有效
         * <p>
         * Quartz 语义: 总执行次数 = repeatCount + 1, -1 表示无限重复(默认)
         *
         * @param repeatCount 重复次数
         * @return this
         */
        public Builder setRepeatCount(int repeatCount) {
            this.repeatCount = repeatCount;
            return this;
        }

        /**
         * 构建 Trigger, 校验三种模式互斥
         *
         * @return 不可变的 Trigger 实例
         */
        public Trigger build() {
            int modeCount = 0;
            if (cron != null) modeCount++;
            if (intervalMillis > 0) modeCount++;
            if (delayMillis > 0) modeCount++;

            Validate.isTrue(modeCount == 1,
                    "必须且只能设置 cron, interval, delay 中的一种");

            if (cron != null) {
                Validate.notBlank(cron, "cron must not be blank");
            }

            if (initialDelayMillis > 0 && intervalMillis <= 0) {
                throw new IllegalArgumentException("initialDelay 仅在 interval 模式下有效");
            }

            if (repeatCount != -1 && intervalMillis <= 0) {
                throw new IllegalArgumentException("repeatCount 仅在 interval 模式下有效");
            }

            return new Trigger(this);
        }
    }
}
