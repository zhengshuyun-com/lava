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
 * 任务配置
 * <p>
 * 用于配置任务的执行行为, 如是否允许并发执行等
 * <pre>{@code
 * // 默认配置(允许并发)
 * TaskConfig config = TaskConfig.defaults();
 *
 * // 禁止并发执行
 * TaskConfig config = TaskConfig.defaults().disallowConcurrent();
 * }</pre>
 *
 * @author Toint
 * @since 2026/2/5
 */
public class TaskConfig {

    /**
     * 是否允许并发执行, 默认允许
     * <p>
     * 当设置为 false 时, 如果上一次任务还在执行, 本次调度将被跳过
     */
    private boolean allowConcurrent = true;

    private TaskConfig() {
    }

    /**
     * 创建默认配置
     *
     * @return 默认配置实例
     */
    public static TaskConfig defaults() {
        return new TaskConfig();
    }

    /**
     * 禁止并发执行
     * <p>
     * 当上一次任务还在执行时, 本次调度将被跳过
     *
     * @return this
     */
    public TaskConfig disallowConcurrent() {
        this.allowConcurrent = false;
        return this;
    }

    /**
     * 是否允许并发执行
     *
     * @return true 允许并发, false 禁止并发
     */
    public boolean isAllowConcurrent() {
        return allowConcurrent;
    }
}
