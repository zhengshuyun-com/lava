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

/**
 * 执行操作时可能抛出受检异常的 supplier。
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

    /**
     * 计算并返回一个结果。
     *
     * @return 计算结果
     * @throws Exception 计算失败时抛出
     */
    T get() throws Exception;
}
