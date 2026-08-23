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

package com.zhengshuyun.lava.schedule;

/**
 * 调度器无法完成调度操作时抛出的通用异常。
 */
public final class ScheduleException extends RuntimeException {

    /**
     * 创建带消息的调度异常。
     *
     * @param message 异常消息
     */
    public ScheduleException(String message) {
        super(message);
    }

    /**
     * 创建带消息和原因的调度异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public ScheduleException(String message, Throwable cause) {
        super(message, cause);
    }
}
