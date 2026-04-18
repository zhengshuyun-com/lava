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

package com.zhengshuyun.lava.http;

import okhttp3.sse.EventSource;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SSE 会话.
 *
 * @author Toint
 * @since 2026/4/18
 */
public final class HttpSseSession implements AutoCloseable {
    /** 底层 EventSource */
    private final AtomicReference<EventSource> eventSourceRef = new AtomicReference<>();

    /** 是否已关闭 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    HttpSseSession() {
    }

    /**
     * 绑定底层 EventSource.
     */
    void bind(EventSource eventSource) {
        eventSourceRef.set(eventSource);
    }

    /**
     * 标记关闭.
     */
    void markClosed() {
        closed.set(true);
    }

    /**
     * 主动取消会话.
     */
    public void cancel() {
        if (closed.compareAndSet(false, true)) {
            EventSource eventSource = eventSourceRef.get();
            if (eventSource != null) {
                eventSource.cancel();
            }
        }
    }

    /**
     * 是否已关闭.
     */
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        cancel();
    }
}
