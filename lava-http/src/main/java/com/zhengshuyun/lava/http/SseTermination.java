/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

/**
 * SSE 会话终止原因。
 */
public enum SseTermination {
    CANCELLED,
    REMOTE_CLOSED,
    FAILED
}
