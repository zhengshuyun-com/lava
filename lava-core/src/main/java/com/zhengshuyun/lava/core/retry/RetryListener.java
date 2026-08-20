/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.core.retry;

/**
 * 接收已完成的重试尝试。监听器失败会传播给调用方。
 */
@FunctionalInterface
public interface RetryListener<T> {

    /**
     * 接收一次已完成尝试的状态。
     *
     * @param attempt 本次尝试的不可变状态
     */
    void onAttempt(RetryAttempt<T> attempt);
}
