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

import com.zhengshuyun.lava.http.SseEvent;

import java.util.Optional;

/**
 * 将通用 SSE 事件转换为业务 chunk；空 Optional 表示忽略该事件。
 */
@FunctionalInterface
public interface AiChunkDecoder<T> {
    Optional<T> decode(SseEvent event) throws Exception;
}
