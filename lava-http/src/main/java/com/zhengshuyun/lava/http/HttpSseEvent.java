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

import com.zhengshuyun.lava.core.lang.Validate;
import org.jspecify.annotations.Nullable;

/**
 * SSE 事件.
 *
 * @param id   事件 ID, 允许为空
 * @param type 事件类型
 * @param data 事件数据
 * @author Toint
 * @since 2026/4/18
 */
public record HttpSseEvent(@Nullable String id, String type, String data) {
    public HttpSseEvent {
        Validate.notBlank(type, "type must not be blank");
        Validate.notNull(data, "data must not be null");
    }
}
