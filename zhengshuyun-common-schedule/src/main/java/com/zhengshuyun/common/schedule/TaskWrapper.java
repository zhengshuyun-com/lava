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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务包装器
 * <p>
 * 封装用户任务, 提供并发控制和异常隔离能力
 *
 * @author Toint
 * @since 2026/2/5
 */
class TaskWrapper implements Runnable {

    /** 用户任务 */
    private final Runnable task;

    /** 任务配置 */
    private final TaskConfig config;

    /**
     * 任务是否正在执行
     * <p>
     * 用于并发控制: 当 allowConcurrent=false 时, 通过 CAS 确保同一时刻只有一个实例在执行
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    TaskWrapper(Runnable task, TaskConfig config) {
        this.task = task;
        this.config = config;
    }

    @Override
    public void run() {
        // 并发控制: 如果禁止并发且上一次还在执行, 则跳过本次
        if (!config.isAllowConcurrent()) {
            if (!running.compareAndSet(false, true)) {
                // 上一次任务还在执行, 跳过本次调度
                return;
            }
        }

        try {
            task.run();
        } catch (Throwable e) {
            // 捕获所有异常, 防止任务失败影响调度器
            // 打印到 stderr 以便观测（生产环境建议使用日志框架）
            System.err.println("Task execution failed: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            // 重置运行状态
            running.set(false);
        }
    }
}
