/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.core.retry;

import java.time.Duration;

/**
 * 供 {@link RetryExecutor} 注入的休眠边界。
 */
@FunctionalInterface
public interface RetrySleeper {

    /**
     * 暂停当前线程指定时长。
     *
     * @param duration 非负的暂停时长
     * @throws InterruptedException 线程在暂停期间被中断时抛出
     */
    void sleep(Duration duration) throws InterruptedException;
}
