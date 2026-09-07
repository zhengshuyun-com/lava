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

package com.zhengshuyun.lava.http;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 不依赖具体传输实现的请求体。
 */
@FunctionalInterface
public interface HttpBody {
    /**
     * 将请求体写入传输层提供的输出流。
     *
     * @param output 由传输层关闭的输出流
     * @throws IOException 写入失败时抛出
     */
    void writeTo(OutputStream output) throws IOException;

    /**
     * 返回请求体的媒体类型。
     *
     * @return 媒体类型；未知时返回 null
     */
    default @Nullable String contentType() {
        return null;
    }

    /**
     * 返回请求体长度。
     *
     * @return 字节长度；未知时返回 -1
     */
    default long contentLength() {
        return -1L;
    }

    /**
     * 声明请求体是否只能写入一次，供传输层决定能否自动重试或在重定向后重放。
     *
     * <p>默认视为可重放；持有不可重新打开的输入流或其他一次性来源的实现必须返回
     * {@code true}。此标记不会转移输入资源的所有权。</p>
     *
     * @return 不可安全重复写入时返回 true
     */
    default boolean isOneShot() {
        return false;
    }
}
