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
 * 重试工具类
 * <p>
 * 提供 {@link Retrier} 工厂方法, 复杂需求通过 Retrier 的流式 API 配置
 *
 * @author Toint
 * @since 2026/1/15
 */
public final class RetryUtil {

    private RetryUtil() {
    }

    /**
     * 创建 RetrierBuilder 实例
     *
     * @return RetrierBuilder 实例
     */
    public static Retrier.Builder retrier() {
        return Retrier.builder();
    }
}
