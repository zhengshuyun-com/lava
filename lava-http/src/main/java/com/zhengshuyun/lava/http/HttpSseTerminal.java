/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

/**
 * SSE 终态事件；仅当终止原因为 {@code FAILED} 时才携带失败信息。
 *
 * @param termination 会话终止原因
 * @param failure 失败详情；非失败终态时为 null
 */
record HttpSseTerminal(HttpSseTermination termination, @Nullable HttpSseFailure failure) {
    public HttpSseTerminal {
        ValidationUtils.requireNonNull(termination, "termination must not be null");
        if ((termination == HttpSseTermination.FAILED) != (failure != null)) {
            throw new IllegalArgumentException("failure must be present exactly for FAILED termination");
        }
    }
}
