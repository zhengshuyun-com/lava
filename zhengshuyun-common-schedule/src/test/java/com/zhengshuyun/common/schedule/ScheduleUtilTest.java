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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScheduleUtil 测试
 *
 * @author Toint
 * @since 2026/2/5
 */
class ScheduleUtilTest {

    /**
     * 测试固定周期任务（毫秒）
     * <p>
     * 每 500ms 执行一次, 等待 1.5 秒, 期望执行 3 次左右
     */
    @Test
    @DisplayName("固定周期任务 - 毫秒")
    void testScheduleEvery_millis() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        String taskId = ScheduleUtil.scheduleEvery(500, () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertNotNull(taskId);

        // 等待执行 3 次
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "任务应该在 2 秒内执行 3 次");
        assertTrue(counter.get() >= 3, "执行次数应该 >= 3, 实际: " + counter.get());

        // 清理
        ScheduleUtil.removeTask(taskId);
    }

    /**
     * 测试固定周期任务（Duration）
     */
    @Test
    @DisplayName("固定周期任务 - Duration")
    void testScheduleEvery_duration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        String taskId = ScheduleUtil.scheduleEvery(Duration.ofMillis(500), () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertNotNull(taskId);
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertTrue(counter.get() >= 2);

        ScheduleUtil.removeTask(taskId);
    }

    /**
     * 测试 Cron 任务
     * <p>
     * 使用每秒执行的 Cron 表达式, 等待执行 2 次
     */
    @Test
    @DisplayName("Cron 任务")
    void testScheduleCron() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        // 每秒执行
        String taskId = ScheduleUtil.scheduleCron("* * * * * ?", () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertNotNull(taskId);
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "Cron 任务应该在 3 秒内执行 2 次");
        assertTrue(counter.get() >= 2);

        ScheduleUtil.removeTask(taskId);
    }

    /**
     * 测试延迟任务（一次性）
     */
    @Test
    @DisplayName("延迟任务 - 一次性")
    void testScheduleOnce() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        String taskId = ScheduleUtil.scheduleOnce(500, () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertNotNull(taskId);

        // 300ms 时应该还没执行
        Thread.sleep(300);
        assertEquals(0, counter.get());

        // 等待执行
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(1, counter.get());

        // 再等待 500ms, 确认只执行一次
        Thread.sleep(500);
        assertEquals(1, counter.get());
    }

    /**
     * 测试延迟任务（Duration）
     */
    @Test
    @DisplayName("延迟任务 - Duration")
    void testScheduleOnce_duration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        String taskId = ScheduleUtil.scheduleOnce(Duration.ofMillis(300), () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertNotNull(taskId);
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(1, counter.get());
    }

    /**
     * 测试删除任务
     */
    @Test
    @DisplayName("删除任务")
    void testRemoveTask() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        String taskId = ScheduleUtil.scheduleEvery(200, counter::incrementAndGet);

        // 等待执行几次
        Thread.sleep(500);
        int countBefore = counter.get();
        assertTrue(countBefore >= 1);

        // 删除任务
        ScheduleUtil.removeTask(taskId);

        // 等待一段时间, 确认不再执行
        Thread.sleep(500);
        int countAfter = counter.get();

        // 删除后执行次数应该不变（允许有一次正在执行的）
        assertTrue(countAfter <= countBefore + 1,
                "删除后不应继续执行, before: " + countBefore + ", after: " + countAfter);
    }

    /**
     * 测试删除不存在的任务
     */
    @Test
    @DisplayName("删除不存在的任务 - 抛出异常")
    void testRemoveTask_notExists() {
        assertThrows(ScheduleException.class, () ->
                ScheduleUtil.removeTask("not-exists-task-id"));
    }

    /**
     * 测试参数校验 - 间隔时间必须大于 0
     */
    @Test
    @DisplayName("参数校验 - 间隔时间必须大于 0")
    void testScheduleEvery_invalidInterval() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleEvery(0, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleEvery(-1, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleEvery(Duration.ZERO, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleEvery(Duration.ofSeconds(-1), () -> {}));
    }

    /**
     * 测试参数校验 - 延迟时间必须大于 0
     */
    @Test
    @DisplayName("参数校验 - 延迟时间必须大于 0")
    void testScheduleOnce_invalidDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleOnce(0, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleOnce(-1, () -> {}));
    }

    /**
     * 测试参数校验 - Cron 表达式不能为空
     */
    @Test
    @DisplayName("参数校验 - Cron 表达式不能为空")
    void testScheduleCron_invalidCron() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleCron(null, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleCron("", () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.scheduleCron("   ", () -> {}));
    }
}
