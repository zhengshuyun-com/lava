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
import org.quartz.CronScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

import java.util.Date;
import java.util.TimeZone;

/**
 * 不可变触发器
 * <p>
 * 支持三种内置模式和自定义扩展: cron, interval(固定间隔), delay(延迟一次), custom(自定义)
 * <pre>{@code
 * // 固定间隔
 * Trigger trigger = Trigger.interval(5000)
 *     .initialDelay(1000)
 *     .repeatCount(10)
 *     .build();
 *
 * // Cron
 * Trigger trigger = Trigger.cron("0 0 2 * * ?").build();
 *
 * // 延迟一次
 * Trigger trigger = Trigger.delay(10000).build();
 *
 * // 自定义策略
 * Trigger trigger = Trigger.custom(triggerId -> {
 *     return TriggerBuilder.newTrigger()
 *         .withIdentity(triggerId)
 *         .startNow()
 *         .build();
 * });
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/6
 */
public final class Trigger {

    /**
     * 触发器策略接口
     * <p>
     * 用于扩展自定义触发逻辑, 如动态 cron、条件触发等复杂场景
     */
    @FunctionalInterface
    public interface TriggerStrategy {

        /**
         * 转换为 Quartz Trigger
         *
         * @param triggerId 触发器 ID
         * @return Quartz Trigger
         */
        org.quartz.Trigger toQuartzTrigger(String triggerId);
    }

    /**
     * 触发器策略
     */
    private final TriggerStrategy strategy;

    private Trigger(TriggerStrategy strategy) {
        this.strategy = strategy;
    }

    // 静态工厂方法

    /**
     * 创建 Cron 触发器构建器
     *
     * @param cronExpression Cron 表达式
     * @return CronBuilder
     */
    public static CronBuilder cron(String cronExpression) {
        return new CronBuilder(cronExpression);
    }

    /**
     * 创建固定间隔触发器构建器
     *
     * @param intervalMillis 间隔时间(毫秒)
     * @return IntervalBuilder
     */
    public static IntervalBuilder interval(long intervalMillis) {
        return new IntervalBuilder(intervalMillis);
    }

    /**
     * 创建延迟触发器构建器
     *
     * @param delayMillis 延迟时间(毫秒)
     * @return DelayBuilder
     */
    public static DelayBuilder delay(long delayMillis) {
        return new DelayBuilder(delayMillis);
    }

    /**
     * 创建自定义触发器
     *
     * @param strategy 触发器策略
     * @return 不可变的 Trigger 实例
     */
    public static Trigger custom(TriggerStrategy strategy) {
        Validate.notNull(strategy, "strategy must not be null");
        return new Trigger(strategy);
    }

    /**
     * 转换为 Quartz Trigger
     *
     * @param triggerId 触发器 ID
     * @return Quartz Trigger
     */
    org.quartz.Trigger toQuartzTrigger(String triggerId) {
        return strategy.toQuartzTrigger(triggerId);
    }

    // Builder 类

    /**
     * Cron 触发器构建器
     */
    public static final class CronBuilder {

        /**
         * Cron 表达式
         */
        private final String cron;

        private CronBuilder(String cron) {
            this.cron = Validate.notBlank(cron, "cron must not be blank");
        }

        /**
         * 构建 Trigger
         *
         * @return 不可变的 Trigger 实例
         */
        public Trigger build() {
            String cronExpr = this.cron;
            return new Trigger(triggerId ->
                    TriggerBuilder.newTrigger()
                            .withIdentity(triggerId)
                            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr)
                                    .inTimeZone(TimeZone.getTimeZone(ZoneIds.UTC)))
                            .build());
        }
    }

    /**
     * 固定间隔触发器构建器
     */
    public static final class IntervalBuilder {

        /**
         * 固定间隔(毫秒)
         */
        private final long intervalMillis;

        /**
         * 初始延迟(毫秒)
         */
        private long initialDelayMillis;

        /**
         * 重复次数, -1 表示无限
         */
        private int repeatCount = -1;

        private IntervalBuilder(long intervalMillis) {
            Validate.isTrue(intervalMillis > 0, "intervalMillis must be positive");
            this.intervalMillis = intervalMillis;
        }

        /**
         * 设置初始延迟(毫秒)
         *
         * @param initialDelayMillis 初始延迟(毫秒)
         * @return this
         */
        public IntervalBuilder initialDelay(long initialDelayMillis) {
            Validate.isTrue(initialDelayMillis >= 0, "initialDelayMillis must be non-negative");
            this.initialDelayMillis = initialDelayMillis;
            return this;
        }

        /**
         * 设置重复次数
         * <p>
         * Quartz 语义: 总执行次数 = repeatCount + 1, -1 表示无限重复(默认)
         *
         * @param repeatCount 重复次数
         * @return this
         */
        public IntervalBuilder repeatCount(int repeatCount) {
            Validate.isTrue(repeatCount >= -1, "repeatCount must be >= -1");
            this.repeatCount = repeatCount;
            return this;
        }

        /**
         * 构建 Trigger
         *
         * @return 不可变的 Trigger 实例
         */
        public Trigger build() {
            long interval = this.intervalMillis;
            long initialDelay = this.initialDelayMillis;
            int repeat = this.repeatCount;
            return new Trigger(triggerId -> {
                TriggerBuilder<org.quartz.Trigger> tb = TriggerBuilder.newTrigger()
                        .withIdentity(triggerId);

                if (initialDelay > 0) {
                    tb.startAt(new Date(System.currentTimeMillis() + initialDelay));
                } else {
                    tb.startNow();
                }

                SimpleScheduleBuilder ssb = SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(interval);

                if (repeat == -1) {
                    ssb.repeatForever();
                } else {
                    ssb.withRepeatCount(repeat);
                }

                return tb.withSchedule(ssb).build();
            });
        }
    }

    /**
     * 延迟触发器构建器(一次性执行)
     */
    public static final class DelayBuilder {

        /**
         * 延迟时间(毫秒)
         */
        private final long delayMillis;

        private DelayBuilder(long delayMillis) {
            Validate.isTrue(delayMillis > 0, "delayMillis must be positive");
            this.delayMillis = delayMillis;
        }

        /**
         * 构建 Trigger
         *
         * @return 不可变的 Trigger 实例
         */
        public Trigger build() {
            long delay = this.delayMillis;
            return new Trigger(triggerId -> {
                Date startTime = new Date(System.currentTimeMillis() + delay);
                return TriggerBuilder.newTrigger()
                        .withIdentity(triggerId)
                        .startAt(startTime)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withRepeatCount(0))
                        .build();
            });
        }
    }
}
