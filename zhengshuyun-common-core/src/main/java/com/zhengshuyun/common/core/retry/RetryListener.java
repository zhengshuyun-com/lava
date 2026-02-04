/*
 * Copyright 2025 Toint (599818663@qq.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.common.core.retry;

/**
 * 重试监听器
 * <p>
 * 监听器会在每次重试尝试开始和完成时被调用. 
 * <b>重要：如果监听器方法抛出异常, 会中断整个重试流程. </b>
 *
 * @author Toint
 * @since 2026/1/15
 */
public interface RetryListener {

    /**
     * 重试开始时调用
     * <p>
     * 注意：此方法抛出异常会中断重试流程
     *
     * @param attempt     当前尝试次数 (从 1 开始) 
     * @param maxAttempts 最大尝试次数
     * @throws RuntimeException 抛出异常会中断重试流程
     */
    void onRetryStart(int attempt, int maxAttempts);

    /**
     * 重试完成时调用 (指每次重试结束时)
     * <p>
     * 注意：此方法抛出异常会中断重试流程
     *
     * @param attempt 当前尝试次数 (从 1 开始) 
     * @param success 本次尝试是否成功
     * @throws RuntimeException 抛出异常会中断重试流程
     */
    void onRetryComplete(int attempt, boolean success);
}
