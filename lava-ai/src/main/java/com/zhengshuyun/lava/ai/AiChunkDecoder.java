/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.ai;

import com.zhengshuyun.lava.http.SseEvent;

import java.util.Optional;

/** 将通用 SSE 事件转换为业务 chunk；空 Optional 表示忽略该事件。 */
@FunctionalInterface
public interface AiChunkDecoder<T> {
    Optional<T> decode(SseEvent event) throws Exception;
}
