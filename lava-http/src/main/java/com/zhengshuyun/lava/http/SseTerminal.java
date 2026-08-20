/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

/**
 * SSE 单一终态通知。
 *
 * @param termination 会话终止原因
 * @param failure     失败详情；非失败终态时为 null
 */
public record SseTerminal(SseTermination termination, @Nullable SseFailure failure) {
    public SseTerminal {
        ValidationUtils.requireNonNull(termination, "termination must not be null");
        if ((termination == SseTermination.FAILED) != (failure != null)) {
            throw new IllegalArgumentException("failure must be present exactly for FAILED");
        }
    }

    /**
     * 将兼容 API 的终态事件转换为通用终态事件。
     *
     * @param terminal 兼容 API 终态事件
     * @return 通用终态事件
     */
    static SseTerminal from(HttpSseTerminal terminal) {
        SseTermination termination = switch (terminal.termination()) {
            case CANCELLED -> SseTermination.CANCELLED;
            case REMOTE_CLOSED -> SseTermination.REMOTE_CLOSED;
            case FAILED -> SseTermination.FAILED;
        };
        return new SseTerminal(termination,
                termination == SseTermination.FAILED ? SseFailure.from(terminal.failure()) : null);
    }
}
