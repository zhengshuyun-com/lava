/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.http;

/**
 * SSE 事件监听器.
 *
 * @author Toint
 * @since 2026/4/18
 */
public interface HttpSseListener {
    /**
     * 建连成功.
     *
     * @param session SSE 会话
     * @param open    建连成功上下文
     */
    default void onOpen(HttpSseSession session, HttpSseOpen open) {
    }

    /**
     * 收到 SSE 事件.
     *
     * @param session SSE 会话
     * @param event   SSE 事件
     */
    default void onEvent(HttpSseSession session, HttpSseEvent event) {
    }

    /**
     * 连接正常关闭.
     *
     * @param session SSE 会话
     */
    default void onClosed(HttpSseSession session) {
    }

    /**
     * 连接失败.
     *
     * @param session SSE 会话
     * @param failure 失败上下文
     */
    default void onFailure(HttpSseSession session, HttpSseFailure failure) {
    }
}
