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
import java.util.List;
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
 * @since 2026/2/6
 */
class ScheduleUtilTest {

    @BeforeEach
    void setUp() throws Exception {
        resetExecutor();
    }

    private void resetExecutor() throws Exception {
        Field executorField = ScheduleUtil.class.getDeclaredField("taskExecutor");
        executorField.setAccessible(true);
        executorField.set(null, null);
    }

    @Test
    @DisplayName("固定间隔任务")
    void testInterval() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                })
                .setId("interval-task")
                .setTrigger(Trigger.interval(500).build())
                .schedule();

        assertNotNull(task);
        assertEquals("interval-task", task.getId());

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "任务应该在 3 秒内执行 3 次");
        assertTrue(counter.get() >= 3);

        task.delete();
    }

    @Test
    @DisplayName("固定间隔任务 - 带初始延迟")
    void testInterval_withInitialDelay() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                })
                .setTrigger(Trigger.interval(500)
                        .initialDelay(1000)
                        .build())
                .schedule();

        // 500ms 时应该还没执行(初始延迟 1000ms)
        Thread.sleep(500);
        assertEquals(0, counter.get());

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertTrue(counter.get() >= 1);

        task.delete();
    }

    @Test
    @DisplayName("固定间隔任务 - 限制重复次数")
    void testInterval_withRepeatCount() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3); // repeatCount=2, 总执行 = 2 + 1 = 3

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                })
                .setTrigger(Trigger.interval(200)
                        .repeatCount(2)
                        .build())
                .schedule();

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "应该执行 3 次(repeatCount=2)");

        // 再等待, 确认不会继续执行
        Thread.sleep(500);
        assertEquals(3, counter.get(), "总执行次数应该是 3");
        // 任务完成后 Quartz 自动清理, 无需手动 delete
    }

    @Test
    @DisplayName("Cron 任务")
    void testCron() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                })
                .setId("cron-task")
                .setTrigger(Trigger.cron("* * * * * ?").build())
                .schedule();

        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue(completed, "Cron 任务应该在 3 秒内执行 2 次");
        assertTrue(counter.get() >= 2);

        task.delete();
    }

    @Test
    @DisplayName("延迟任务 - 一次性")
    void testDelay() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                })
                .setTrigger(Trigger.delay(500).build())
                .schedule();

        // 300ms 时应该还没执行
        Thread.sleep(300);
        assertEquals(0, counter.get());

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(1, counter.get());

        // 再等待, 确认只执行一次
        Thread.sleep(500);
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("自动生成任务 ID")
    void testAutoGeneratedId() {
        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                })
                .setTrigger(Trigger.delay(60000).build())
                .schedule();

        assertNotNull(task.getId());
        assertFalse(task.getId().isBlank());

        task.delete();
    }

    @Test
    @DisplayName("删除任务 - deleteTask")
    void testDeleteTask() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        ScheduledTask task = ScheduleUtil.taskBuilder(counter::incrementAndGet)
                .setId("to-delete")
                .setTrigger(Trigger.interval(200).build())
                .schedule();

        Thread.sleep(500);
        int countBefore = counter.get();
        assertTrue(countBefore >= 1);

        boolean deleted = ScheduleUtil.deleteTask("to-delete");
        assertTrue(deleted, "删除存在的任务应返回 true");

        Thread.sleep(500);
        int countAfter = counter.get();
        assertTrue(countAfter <= countBefore + 1);
    }

    @Test
    @DisplayName("删除不存在的任务 - 幂等, 返回 false")
    void testDeleteTask_notExists() {
        boolean deleted = ScheduleUtil.deleteTask("not-exists");
        assertFalse(deleted, "删除不存在的任务应返回 false");
    }

    @Test
    @DisplayName("hasTask - 任务存在/不存在")
    void testHasTask() {
        assertFalse(ScheduleUtil.hasTask("has-task-check"));

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                })
                .setId("has-task-check")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();

        assertTrue(ScheduleUtil.hasTask("has-task-check"));

        task.delete();
        assertFalse(ScheduleUtil.hasTask("has-task-check"));
    }

    @Test
    @DisplayName("ScheduledTask.exists()")
    void testScheduledTask_exists() {
        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                })
                .setId("exists-check")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();

        assertTrue(task.exists());

        boolean deleted = task.delete();
        assertTrue(deleted, "第一次删除应返回 true");
        assertFalse(task.exists());

        // 再次删除同一任务(幂等)
        boolean deletedAgain = task.delete();
        assertFalse(deletedAgain, "第二次删除应返回 false");
    }

    @Test
    @DisplayName("暂停和恢复任务")
    void testPauseAndResume() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        ScheduledTask task = ScheduleUtil.taskBuilder(counter::incrementAndGet)
                .setId("pause-resume")
                .setTrigger(Trigger.interval(200).build())
                .schedule();

        Thread.sleep(500);
        int countBefore = counter.get();
        assertTrue(countBefore >= 1);

        // 暂停
        task.pause();
        assertTrue(task.isPaused());

        Thread.sleep(500);
        int countAfterPause = counter.get();
        assertTrue(countAfterPause <= countBefore + 1);

        // 恢复
        task.resume();
        assertFalse(task.isPaused());

        Thread.sleep(500);
        int countAfterResume = counter.get();
        assertTrue(countAfterResume > countAfterPause);

        task.delete();
    }

    @Test
    @DisplayName("立即触发任务")
    void testTriggerNow() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                })
                .setId("trigger-now")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();

        assertEquals(0, counter.get());

        task.triggerNow();

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(1, counter.get());

        task.delete();
    }

    @Test
    @DisplayName("reschedule - 重新调度")
    void testReschedule() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        ScheduledTask task = ScheduleUtil.taskBuilder(counter::incrementAndGet)
                .setId("reschedule-task")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();

        Thread.sleep(500);
        assertEquals(0, counter.get());

        // 重新调度为每秒执行
        ScheduleUtil.reschedule("reschedule-task",
                Trigger.cron("* * * * * ?").build());

        Thread.sleep(2000);
        assertTrue(counter.get() >= 1, "重新调度后应该开始执行");

        task.delete();
    }

    @Test
    @DisplayName("getTask - 获取任务句柄")
    void testGetTask() {
        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                })
                .setId("get-task")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();

        ScheduledTask found = ScheduleUtil.getTask("get-task");
        assertEquals("get-task", found.getId());
        assertFalse(found.isPaused());
        assertNotNull(found.getNextFireTime());

        task.delete();
    }

    @Test
    @DisplayName("getTask - 不存在的任务抛异常")
    void testGetTask_notExists() {
        assertThrows(ScheduleException.class, () ->
                ScheduleUtil.getTask("not-exists"));
    }

    @Test
    @DisplayName("getAllTasks")
    void testGetAllTasks() {
        ScheduledTask task1 = ScheduleUtil.taskBuilder(() -> {
                })
                .setId("all-task-1")
                .setTrigger(Trigger.cron("0 0 2 * * ?").build())
                .schedule();

        ScheduledTask task2 = ScheduleUtil.taskBuilder(() -> {
                })
                .setId("all-task-2")
                .setTrigger(Trigger.cron("0 0 3 * * ?").build())
                .schedule();

        List<ScheduledTask> tasks = ScheduleUtil.getAllTasks();
        assertTrue(tasks.stream().anyMatch(t -> "all-task-1".equals(t.getId())));
        assertTrue(tasks.stream().anyMatch(t -> "all-task-2".equals(t.getId())));

        task1.delete();
        task2.delete();
    }

    @Test
    @DisplayName("getNextFireTime / getPreviousFireTime")
    void testFireTimes() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ScheduledTask task = ScheduleUtil.taskBuilder(latch::countDown)
                .setId("fire-times")
                .setTrigger(Trigger.cron("* * * * * ?").build())
                .schedule();

        assertNotNull(task.getNextFireTime());
        assertNull(task.getPreviousFireTime()); // 还没执行过

        latch.await(2, TimeUnit.SECONDS);
        Thread.sleep(100);

        assertNotNull(task.getPreviousFireTime());

        task.delete();
    }

    @Test
    @DisplayName("getQuartzScheduler")
    void testGetQuartzScheduler() {
        assertNotNull(ScheduleUtil.getQuartzScheduler());
    }

    @Test
    @DisplayName("任务异常不影响调度器")
    void testTaskException() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        ScheduledTask task = ScheduleUtil.taskBuilder(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                    throw new RuntimeException("模拟异常");
                })
                .setId("exception-task")
                .setTrigger(Trigger.interval(200).build())
                .schedule();

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "任务异常不应影响后续调度");
        assertTrue(counter.get() >= 3);

        task.delete();
    }

    @Test
    @DisplayName("TaskBuilder - trigger 未设置抛异常")
    void testTaskBuilder_noTrigger() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.taskBuilder(() -> {
                }).schedule());
    }

    @Test
    @DisplayName("newTask - null 参数校验")
    void testNewTask_null() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.taskBuilder(null));
    }

    @Test
    @DisplayName("无效的 Cron 表达式")
    void testInvalidCron() {
        assertThrows(Exception.class, () ->
                ScheduleUtil.taskBuilder(() -> {
                        })
                        .setTrigger(Trigger.cron("invalid-cron").build())
                        .schedule());
    }

    @Test
    @DisplayName("initTaskExecutor - 正常初始化")
    void testInitTaskExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newFixedThreadPool(4);
        ScheduleUtil.initTaskExecutor(customExecutor);

        Field executorField = ScheduleUtil.class.getDeclaredField("taskExecutor");
        executorField.setAccessible(true);
        assertSame(customExecutor, executorField.get(null));

        customExecutor.shutdown();
    }

    @Test
    @DisplayName("initTaskExecutor - null 参数校验")
    void testInitTaskExecutor_null() {
        assertThrows(IllegalArgumentException.class, () ->
                ScheduleUtil.initTaskExecutor(null));
    }

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
}
