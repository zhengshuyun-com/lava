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

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

/**
 * SSE 事件.
 *
 * <h2>事件类型的默认值</h2>
 * 服务端可以只发送 {@code data:} 而不发送 {@code event:} 字段 (OpenAI 等主流流式接口就是这种格式) ,
 * 此时 OkHttp 会回传 {@code null}. 按
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html#dispatchMessage">WHATWG SSE 规范</a>,
 * 事件类型缓冲区为空时事件类型即为 {@code message}, 因此本类会把 {@code null} 归一化为
 * {@link #DEFAULT_TYPE}, 保证 {@link #type()} 永不为 null, 调用方无需判空.
 *
 * @param id   事件 ID, 服务端未发送 {@code id:} 时为 null
 * @param type 事件类型, 永不为 null, 服务端未指定时为 {@value #DEFAULT_TYPE}
 * @param data 事件数据
 * @author Toint
 * @since 2026/4/18
 */
record HttpSseEvent(@Nullable String id, String type, String data) {

    /**
     * SSE 规范定义的默认事件类型
     */
    public static final String DEFAULT_TYPE = "message";

    public HttpSseEvent {
        ValidationUtils.requireNonNull(data, "data must not be null");
        // 归一化: null/空白 -> "message", 而不是抛异常
        if (type == null || type.isBlank()) {
            type = DEFAULT_TYPE;
        }
    }

    /**
     * 是否为默认事件类型 (服务端未显式声明 {@code event:})
     */
    public boolean isDefaultType() {
        return DEFAULT_TYPE.equals(type);
    }
}
