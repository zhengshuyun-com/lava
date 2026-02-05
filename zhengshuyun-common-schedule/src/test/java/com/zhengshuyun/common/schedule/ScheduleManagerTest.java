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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScheduleManager 测试
 *
 * @author Toint
 * @since 2026/2/5
 */
class ScheduleManagerTest {

    private ScheduleManager manager;

    @BeforeEach
    void setUp() {
        manager = ScheduleUtil.manager();
    }

    /**
     * 测试指定任务 ID
     */
    @Test
    @DisplayName("指定任务 ID")
    void testCustomTaskId() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        String taskId = manager.addCronTask("my-custom-task", "* * * * * ?", () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertEquals("my-custom-task", taskId);

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertTrue(counter.get() >= 1);

        manager.removeTask(taskId);
    }

    /**
     * 测试暂停和恢复任务
     */
    @Test
    @DisplayName("暂停和恢复任务")
    void testPauseAndResume() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        String taskId = manager.addFixedRateTask(200, counter::incrementAndGet);

        // 等待执行几次
        Thread.sleep(500);
        int countBefore = counter.get();
        assertTrue(countBefore >= 1);

        // 暂停任务
        manager.pauseTask(taskId);

        // 等待一段时间
        Thread.sleep(500);
        int countAfterPause = counter.get();

        // 暂停后执行次数应该不变（允许有一次正在执行的）
        assertTrue(countAfterPause <= countBefore + 1);

        // 恢复任务
        manager.resumeTask(taskId);

        // 等待执行
        Thread.sleep(500);
        int countAfterResume = counter.get();

        // 恢复后应该继续执行
        assertTrue(countAfterResume > countAfterPause);

        manager.removeTask(taskId);
    }

    /**
     * 测试立即触发任务
     */
    @Test
    @DisplayName("立即触发任务")
    void testTriggerNow() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        // 每天凌晨 2 点执行（正常情况下不会触发）
        String taskId = manager.addCronTask("0 0 2 * * ?", () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        // 验证还没执行
        assertEquals(0, counter.get());

        // 立即触发
        manager.triggerNow(taskId);

        // 等待执行
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(1, counter.get());

        manager.removeTask(taskId);
    }

    /**
     * 测试修改 Cron 表达式
     */
    @Test
    @DisplayName("修改 Cron 表达式")
    void testUpdateCronTask() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        // 每天凌晨 2 点执行（正常情况下不会触发）
        String taskId = manager.addCronTask("0 0 2 * * ?", counter::incrementAndGet);

        // 验证还没执行
        Thread.sleep(500);
        assertEquals(0, counter.get());

        // 修改为每秒执行
        manager.updateCronTask(taskId, "* * * * * ?");

        // 等待执行
        Thread.sleep(2000);
        assertTrue(counter.get() >= 1, "修改 Cron 后应该开始执行");

        manager.removeTask(taskId);
    }

    /**
     * 测试获取任务信息
     */
    @Test
    @DisplayName("获取任务信息")
    void testGetTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        String taskId = manager.addCronTask("* * * * * ?", latch::countDown);

        TaskInfo info = manager.getTask(taskId);
        assertNotNull(info);
        assertEquals(taskId, info.getTaskId());
        assertEquals(TaskInfo.Status.WAITING, info.getStatus());
        assertNotNull(info.getNextFireTime());

        // 等待执行一次
        latch.await(2, TimeUnit.SECONDS);
        Thread.sleep(100); // 等待状态更新

        info = manager.getTask(taskId);
        assertNotNull(info.getPreviousFireTime());

        manager.removeTask(taskId);
    }

    /**
     * 测试列出所有任务
     */
    @Test
    @DisplayName("列出所有任务")
    void testListAllTasks() {
        String taskId1 = manager.addCronTask("task-1", "0 0 2 * * ?", () -> {});
        String taskId2 = manager.addCronTask("task-2", "0 0 3 * * ?", () -> {});

        List<TaskInfo> tasks = manager.listAllTasks();

        // 至少包含我们添加的两个任务
        assertTrue(tasks.stream().anyMatch(t -> "task-1".equals(t.getTaskId())));
        assertTrue(tasks.stream().anyMatch(t -> "task-2".equals(t.getTaskId())));

        manager.removeTask(taskId1);
        manager.removeTask(taskId2);
    }

    /**
     * 测试并发控制 - 禁止并发
     * <p>
     * 任务执行时间 500ms, 调度间隔 200ms
     * 禁止并发时, 只有第一个任务执行完才会执行下一个
     */
    @Test
    @DisplayName("并发控制 - 禁止并发")
    void testDisallowConcurrent() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        String taskId = manager.addFixedRateTask(200, () -> {
            // 记录并发数
            int current = concurrentCount.incrementAndGet();
            maxConcurrent.updateAndGet(max -> Math.max(max, current));

            try {
                Thread.sleep(500); // 模拟耗时任务
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                concurrentCount.decrementAndGet();
            }

            counter.incrementAndGet();
        }, TaskConfig.defaults().disallowConcurrent());

        // 等待足够时间
        Thread.sleep(2000);

        // 禁止并发, 最大并发数应该是 1
        assertEquals(1, maxConcurrent.get(), "禁止并发时, 最大并发数应该是 1");

        manager.removeTask(taskId);
    }

    /**
     * 测试并发控制 - 允许并发（默认）
     */
    @Test
    @DisplayName("并发控制 - 允许并发")
    void testAllowConcurrent() throws InterruptedException {
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        String taskId = manager.addFixedRateTask(100, () -> {
            int current = concurrentCount.incrementAndGet();
            maxConcurrent.updateAndGet(max -> Math.max(max, current));

            try {
                Thread.sleep(500); // 模拟耗时任务
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                concurrentCount.decrementAndGet();
            }
        }); // 默认允许并发

        // 等待足够时间
        Thread.sleep(1500);

        // 允许并发, 最大并发数应该 > 1
        assertTrue(maxConcurrent.get() > 1, "允许并发时, 最大并发数应该 > 1, 实际: " + maxConcurrent.get());

        manager.removeTask(taskId);
    }

    /**
     * 测试任务异常不影响调度器
     */
    @Test
    @DisplayName("任务异常不影响调度器")
    void testTaskException() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        String taskId = manager.addFixedRateTask(200, () -> {
            counter.incrementAndGet();
            latch.countDown();
            throw new RuntimeException("模拟异常");
        });

        // 等待执行 3 次
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "任务异常不应影响后续调度");
        assertTrue(counter.get() >= 3);

        manager.removeTask(taskId);
    }

    /**
     * 测试获取不存在的任务
     */
    @Test
    @DisplayName("获取不存在的任务 - 抛出异常")
    void testGetTask_notExists() {
        assertThrows(ScheduleException.class, () ->
                manager.getTask("not-exists-task-id"));
    }

    /**
     * 测试无效的 Cron 表达式
     */
    @Test
    @DisplayName("无效的 Cron 表达式 - 抛出异常")
    void testInvalidCron() {
        assertThrows(Exception.class, () ->
                manager.addCronTask("invalid-cron", () -> {}));
    }
}
