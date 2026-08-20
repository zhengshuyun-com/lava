/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.ai;

import com.zhengshuyun.lava.http.SseSession;
import com.zhengshuyun.lava.http.SseTerminal;

/**
 * AI 增量流监听器；底层 SSE 终态仍只通知一次。
 */
public interface AiStreamListener<T> {
    void onChunk(SseSession session, T chunk);

    default void onTerminal(SseSession session, SseTerminal terminal) {
    }
}
