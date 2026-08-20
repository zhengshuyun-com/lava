/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

/**
 * 简洁命名的通用 SSE 事件。
 *
 * @param id   服务端事件 ID；未提供时为 null
 * @param type 事件类型；空白值归一化为 {@link #DEFAULT_TYPE}
 * @param data 事件数据
 */
public record SseEvent(@Nullable String id, String type, String data) {
    /**
     * SSE 规范约定的默认事件类型。
     */
    public static final String DEFAULT_TYPE = "message";

    public SseEvent {
        if (type == null || type.isBlank()) {
            type = DEFAULT_TYPE;
        }
        ValidationUtils.requireNonNull(data, "data must not be null");
    }

    /**
     * 判断该事件是否使用默认类型。
     *
     * @return 类型为 {@code message} 时返回 true
     */
    public boolean isDefaultType() {
        return DEFAULT_TYPE.equals(type);
    }

    /**
     * 将兼容 API 的事件转换为通用事件。
     *
     * @param event 兼容 API 事件
     * @return 通用事件
     */
    static SseEvent from(HttpSseEvent event) {
        return new SseEvent(event.id(), event.type(), event.data());
    }
}
