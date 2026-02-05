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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
     * 每个测试前重置执行器状态
     */
    @BeforeEach
    void setUp() throws Exception {
        resetExecutor();
    }

    /**
     * 使用反射重置执行器状态
     */
    private void resetExecutor() throws Exception {
        Field executorField = ScheduleUtil.class.getDeclaredField("taskExecutor");
        executorField.setAccessible(true);
        executorField.set(null, null);

        Field initializedField = ScheduleUtil.class.getDeclaredField("executorInitialized");
        initializedField.setAccessible(true);
        initializedField.set(null, false);
    }

    /**
     * 测试固定周期任务(毫秒)
     * <p>
     * 每 500ms 执行一次, 等待 1.5 秒, 期望执行 3 次左右
     */
    @Test
    @DisplayName("固定周期任务 - 毫秒")
    void testAddFixedRateTask_millis() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        String taskId = ScheduleUtil.addFixedRateTask(500, () -> {
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
     * 测试固定周期任务(Duration)
     */
    @Test
    @DisplayName("固定周期任务 - Duration")
    void testAddFixedRateTask_duration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        String taskId = ScheduleUtil.addFixedRateTask(Duration.ofMillis(500), () -> {
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
    void testAddCronTask() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        // 每秒执行
        String taskId = ScheduleUtil.addCronTask("* * * * * ?", () -> {
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
     * 测试延迟任务(一次性)
     */
    @Test
    @DisplayName("延迟任务 - 一次性")
    void testAddDelayedTask() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        String taskId = ScheduleUtil.addDelayedTask(500, () -> {
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
     * 测试延迟任务(Duration)
     */
    @Test
    @DisplayName("延迟任务 - Duration")
    void testAddDelayedTask_duration() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        String taskId = ScheduleUtil.addDelayedTask(Duration.ofMillis(300), () -> {
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

        String taskId = ScheduleUtil.addFixedRateTask(200, counter::incrementAndGet);

        // 等待执行几次
        Thread.sleep(500);
        int countBefore = counter.get();
        assertTrue(countBefore >= 1);

        // 删除任务
        ScheduleUtil.removeTask(taskId);

        // 等待一段时间, 确认不再执行
        Thread.sleep(500);
        int countAfter = counter.get();

        // 删除后执行次数应该不变(允许有一次正在执行的)
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
    void testAddFixedRateTask_invalidInterval() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addFixedRateTask(0, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addFixedRateTask(-1, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addFixedRateTask(Duration.ZERO, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addFixedRateTask(Duration.ofSeconds(-1), () -> {}));
    }

    /**
     * 测试参数校验 - 延迟时间必须大于 0
     */
    @Test
    @DisplayName("参数校验 - 延迟时间必须大于 0")
    void testAddDelayedTask_invalidDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addDelayedTask(0, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addDelayedTask(-1, () -> {}));
    }

    /**
     * 测试参数校验 - Cron 表达式不能为空
     */
    @Test
    @DisplayName("参数校验 - Cron 表达式不能为空")
    void testAddCron_invalidCronTask() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addCronTask(null, () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addCronTask("", () -> {}));

        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.addCronTask("   ", () -> {}));
    }

    /**
     * 测试 initTaskExecutor - 正常初始化
     */
    @Test
    @DisplayName("initTaskExecutor - 正常初始化")
    void testInitTaskExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newFixedThreadPool(4);
        ScheduleUtil.initTaskExecutor(customExecutor);

        // 验证初始化成功
        Field executorField = ScheduleUtil.class.getDeclaredField("taskExecutor");
        executorField.setAccessible(true);
        assertSame(customExecutor, executorField.get(null));

        Field initializedField = ScheduleUtil.class.getDeclaredField("executorInitialized");
        initializedField.setAccessible(true);
        assertTrue((Boolean) initializedField.get(null));

        customExecutor.shutdown();
    }

    /**
     * 测试 initTaskExecutor - null 参数校验
     */
    @Test
    @DisplayName("initTaskExecutor - null 参数校验")
    void testInitTaskExecutor_null() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.initTaskExecutor(null));
    }

    /**
     * 测试 initTaskExecutor - 重复初始化抛异常
     */
    @Test
    @DisplayName("initTaskExecutor - 重复初始化抛异常")
    void testInitTaskExecutor_duplicate() {
        ExecutorService executor1 = Executors.newFixedThreadPool(2);
        ScheduleUtil.initTaskExecutor(executor1);

        ExecutorService executor2 = Executors.newFixedThreadPool(4);
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.initTaskExecutor(executor2));

        executor1.shutdown();
        executor2.shutdown();
    }

    /**
     * 测试 initTaskExecutor - 懒加载后不能初始化
     */
    @Test
    @DisplayName("initTaskExecutor - 懒加载后不能初始化")
    void testInitTaskExecutor_afterLazyInit() throws InterruptedException {
        // 先触发懒加载
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        String taskId = ScheduleUtil.addFixedRateTask(100, () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        // 等待任务执行一次，确保懒加载完成
        latch.await(1, TimeUnit.SECONDS);

        // 尝试初始化应该失败
        ExecutorService customExecutor = Executors.newFixedThreadPool(4);
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.initTaskExecutor(customExecutor));

        ScheduleUtil.removeTask(taskId);
        customExecutor.shutdown();
    }

    /**
     * 测试废弃方法 setTaskExecutor 兼容性
     */
    @Test
    @DisplayName("废弃方法 setTaskExecutor 兼容性测试")
    @SuppressWarnings("deprecation")
    void testDeprecatedSetTaskExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newFixedThreadPool(2);
        ScheduleUtil.setTaskExecutor(customExecutor);

        // 验证初始化成功
        Field executorField = ScheduleUtil.class.getDeclaredField("taskExecutor");
        executorField.setAccessible(true);
        assertSame(customExecutor, executorField.get(null));

        customExecutor.shutdown();
    }
}
