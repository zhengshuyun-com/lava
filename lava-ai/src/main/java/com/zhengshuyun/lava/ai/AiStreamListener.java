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
