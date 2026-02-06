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

/**
 * 任务包装器
 * <p>
 * 封装用户任务, 提供异常隔离能力, 防止任务失败影响调度器
 *
 * @author Toint
 * @since 2026/2/5
 */
class TaskWrapper implements Runnable {

    /** 用户任务 */
    private final Runnable task;

    TaskWrapper(Runnable task) {
        this.task = task;
    }

    @Override
    public void run() {
        try {
            task.run();
        } catch (Throwable e) {
            // 捕获所有异常, 防止任务失败影响调度器
            System.err.println("Task execution failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
