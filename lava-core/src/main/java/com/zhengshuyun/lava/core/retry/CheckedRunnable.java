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
 * 执行操作时可能抛出受检异常的 runnable。
 */
@FunctionalInterface
public interface CheckedRunnable {

    /**
     * 执行操作。
     *
     * @throws Exception 执行失败时抛出
     */
    void run() throws Exception;
}
