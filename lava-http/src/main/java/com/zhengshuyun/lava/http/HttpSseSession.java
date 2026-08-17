/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import okhttp3.sse.EventSource;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/** 具有单一原子终态转换的线程安全 SSE 会话。 */
final class HttpSseSession implements AutoCloseable {
    /** SSE 会话的可观测生命周期状态。 */
    public enum State {
        /** 已创建但尚未完成握手。 */
        CONNECTING,
        /** 握手成功，正在接收事件。 */
        OPEN,
        /** 调用方或客户端主动取消。 */
        CANCELLED,
        /** 服务端正常关闭事件流。 */
        REMOTE_CLOSED,
        /** 传输或监听器回调失败。 */
        FAILED
    }

    /** 接收握手、事件和终态通知的监听器。 */
    private final HttpSseListener listener;
    /** 终态送达后从客户端活动会话集合移除自己的回调。 */
    private final Consumer<HttpSseSession> terminalHook;
    /** 通过 CAS 协调并发到达的生命周期转换。 */
    private final AtomicReference<State> state = new AtomicReference<>(State.CONNECTING);
    /** 延迟绑定的底层事件源，供取消时终止读取。 */
    private final AtomicReference<@Nullable EventSource> eventSource = new AtomicReference<>();
    /** 防御性保证终态监听器最多收到一次通知。 */
    private final AtomicBoolean terminalDelivered = new AtomicBoolean();
    /** 串行化业务回调，避免事件回调与终态回调交错执行。 */
    private final Object callbackLock = new Object();

    HttpSseSession(HttpSseListener listener, Consumer<HttpSseSession> terminalHook) {
        this.listener = listener;
        this.terminalHook = terminalHook;
    }

    void bind(EventSource source) {
        eventSource.compareAndSet(null, source);
        State current = state.get();
        if (current == State.CANCELLED || current == State.FAILED) {
            source.cancel();
        }
    }

    void opened(HttpSseOpen open) {
        if (!state.compareAndSet(State.CONNECTING, State.OPEN)) {
            return;
        }
        Throwable callbackFailure = null;
        synchronized (callbackLock) {
            if (state.get() != State.OPEN) {
                return;
            }
            try {
                listener.onOpen(this, open);
            } catch (Throwable throwable) {
                callbackFailure = throwable;
            }
        }
        if (callbackFailure != null) {
            fail(new HttpSseFailure(HttpFailureKind.IO, callbackFailure, open.statusCode(),
                    open.headers().redacted(), null));
        }
    }

    void event(HttpSseEvent event) {
        Throwable callbackFailure = null;
        synchronized (callbackLock) {
            if (state.get() != State.OPEN) {
                return;
            }
            try {
                listener.onEvent(this, event);
            } catch (Throwable throwable) {
                callbackFailure = throwable;
            }
        }
        if (callbackFailure != null) {
            fail(new HttpSseFailure(HttpFailureKind.IO, callbackFailure, null, null, null));
        }
    }

    void remoteClosed() {
        complete(State.REMOTE_CLOSED, new HttpSseTerminal(HttpSseTermination.REMOTE_CLOSED, null));
    }

    void fail(HttpSseFailure failure) {
        complete(State.FAILED, new HttpSseTerminal(HttpSseTermination.FAILED, failure));
    }

    private void complete(State terminalState, HttpSseTerminal terminal) {
        // 远端关闭、显式取消和失败可能并发到达；CAS 决定唯一的最终结果。
        while (true) {
            State current = ValidationUtils.requireNonNull(state.get());
            if (isTerminal(current)) {
                return;
            }
            if (state.compareAndSet(current, terminalState)) {
                break;
            }
        }

        if (terminalState == State.CANCELLED || terminalState == State.FAILED) {
            EventSource source = eventSource.get();
            if (source != null) {
                // 主动终态必须停止底层读取，防止终态后继续派发事件。
                source.cancel();
            }
        }
        deliverTerminal(terminal);
    }

    private void deliverTerminal(HttpSseTerminal terminal) {
        if (!terminalDelivered.compareAndSet(false, true)) {
            return;
        }
        try {
            synchronized (callbackLock) {
                try {
                    listener.onTerminal(this, terminal);
                } catch (Throwable ignored) {
                    // 终态回调不能再创建第二个终态事件。
                }
            }
        } finally {
            terminalHook.accept(this);
        }
    }

    /** 主动取消会话，并停止底层事件流读取。 */
    public void cancel() {
        complete(State.CANCELLED, new HttpSseTerminal(HttpSseTermination.CANCELLED, null));
    }

    /**
     * 返回当前会话状态。
     *
     * @return 当前状态
     */
    public State getState() {
        return ValidationUtils.requireNonNull(state.get());
    }

    /**
     * 判断会话是否已进入任一终态。
     *
     * @return 已终止时返回 true
     */
    public boolean isClosed() {
        return isTerminal(ValidationUtils.requireNonNull(state.get()));
    }

    /**
     * 判断会话是否由本地取消终止。
     *
     * @return 终态为取消时返回 true
     */
    public boolean isCancelled() {
        return state.get() == State.CANCELLED;
    }

    /** 等同于 {@link #cancel()}。 */
    @Override
    public void close() {
        cancel();
    }

    private static boolean isTerminal(State state) {
        return state == State.CANCELLED || state == State.REMOTE_CLOSED || state == State.FAILED;
    }
}
