/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

/**
 * 通用 SSE 监听器；回调在传输回调线程执行，终态恰好一次。
 */
public interface SseListener {
    /**
     * SSE 握手成功后调用。
     *
     * @param session    当前会话，可用于取消
     * @param statusCode HTTP 响应状态码
     * @param headers    响应头
     */
    default void onOpen(SseSession session, int statusCode, HttpHeaders headers) {
    }

    /**
     * 收到一条 SSE 事件时调用。
     *
     * @param session 当前会话，可用于取消
     * @param event   已解析的 SSE 事件
     */
    default void onEvent(SseSession session, SseEvent event) {
    }

    /**
     * 会话进入唯一终态时调用。
     *
     * @param session  已终止的会话
     * @param terminal 终态原因与失败信息
     */
    default void onTerminal(SseSession session, SseTerminal terminal) {
    }
}
