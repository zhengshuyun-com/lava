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

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Quartz Job 实现
 * <p>
 * 将任务提交到执行器异步执行, 本方法立即返回, 不阻塞 Quartz 调度线程
 *
 * @author Toint
 * @since 2026/2/5
 */
public class ExecutorJob implements Job {

    /** JobDataMap 中存储 TaskWrapper 的 key */
    static final String TASK_WRAPPER_KEY = "taskWrapper";

    @Override
    public void execute(JobExecutionContext context) {
        // 从 JobDataMap 获取任务包装器
        TaskWrapper wrapper = (TaskWrapper) context.getJobDetail()
                .getJobDataMap()
                .get(TASK_WRAPPER_KEY);

        // 提交到执行器异步执行, 立即返回
        ScheduleUtil.getTaskExecutor().submit(wrapper);
    }
}
