/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

/**
 * 单个 SSE 会话的监听器；会话只会调用 {@link #onTerminal} 一次。
 */
interface HttpSseListener {
    /**
     * SSE 握手成功后调用。
     *
     * @param session 当前会话，可用于取消
     * @param open    握手状态与响应头
     */
    default void onOpen(HttpSseSession session, HttpSseOpen open) {
    }

    /**
     * 收到一条 SSE 事件时调用。
     *
     * @param session 当前会话，可用于取消
     * @param event   已解析的 SSE 事件
     */
    default void onEvent(HttpSseSession session, HttpSseEvent event) {
    }

    /**
     * 会话进入唯一终态时调用。
     *
     * @param session  已终止的会话
     * @param terminal 终态原因与失败信息
     */
    default void onTerminal(HttpSseSession session, HttpSseTerminal terminal) {
    }
}
