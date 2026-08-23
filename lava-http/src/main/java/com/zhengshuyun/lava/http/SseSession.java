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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 简洁命名的可取消 SSE 会话句柄。
 */
public final class SseSession implements AutoCloseable {
    /**
     * 调用方注册的通用 SSE 监听器。
     */
    private final SseListener listener;
    /**
     * 异步创建后绑定的兼容 API 会话。
     */
    private final AtomicReference<HttpSseSession> delegate = new AtomicReference<>();
    /**
     * 处理绑定发生前就调用 cancel 的竞态。
     */
    private final AtomicBoolean cancelRequested = new AtomicBoolean();
    /**
     * 已确定的唯一终态，供调用方在回调外查询。
     */
    private final AtomicReference<SseTerminal> terminal = new AtomicReference<>();

    SseSession(SseListener listener) {
        this.listener = listener;
    }

    void bind(HttpSseSession session) {
        delegate.set(session);
        if (cancelRequested.get()) {
            session.cancel();
        }
    }

    void opened(int statusCode, HttpHeaders headers) {
        listener.onOpen(this, statusCode, headers);
    }

    void event(SseEvent event) {
        listener.onEvent(this, event);
    }

    void terminal(SseTerminal terminal) {
        this.terminal.compareAndSet(null, terminal);
        listener.onTerminal(this, terminal);
    }

    /**
     * 主动取消会话；在底层会话尚未绑定时也会记录取消意图。
     */
    public void cancel() {
        cancelRequested.set(true);
        HttpSseSession session = delegate.get();
        if (session != null) {
            session.cancel();
        }
    }

    /**
     * 判断会话是否已取消或收到终态通知。
     *
     * @return 已终止时返回 true
     */
    public boolean isClosed() {
        HttpSseSession session = delegate.get();
        return cancelRequested.get() || terminal.get() != null
                || (session != null && session.isClosed());
    }

    /**
     * 判断会话的实际终态是否为取消。
     *
     * @return 已取消时返回 true
     */
    public boolean isCancelled() {
        SseTerminal current = terminal.get();
        // 终态一旦确定，以实际终态为准；自然结束后调用 close() 不能改写结果。
        return current == null
                ? cancelRequested.get()
                : current.termination() == SseTermination.CANCELLED;
    }

    /**
     * 返回已经确定的终态。
     *
     * @return 终态；仍运行时为空
     */
    public Optional<SseTerminal> terminal() {
        return Optional.ofNullable(terminal.get());
    }

    /**
     * 等同于 {@link #cancel()}。
     */
    @Override
    public void close() {
        cancel();
    }
}
