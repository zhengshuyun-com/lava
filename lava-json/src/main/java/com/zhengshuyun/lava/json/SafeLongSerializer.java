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

package com.zhengshuyun.lava.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Long值JS安全序列化器
 *
 * <p>规则：</p>
 * <ul>
 *   <li>在 JS 安全范围内 (-2^53+1 到 2^53-1)：保持数字类型</li>
 *   <li>超出 JS 安全范围：转为字符串</li>
 * </ul>
 *
 * @author Toint
 * @since 2026/1/2
 */
public class SafeLongSerializer extends JsonSerializer<Long> {
    /**
     * JavaScript Number.MAX_SAFE_INTEGER = 2^53 - 1 = 9007199254740991
     */
    private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;

    /**
     * JavaScript Number.MIN_SAFE_INTEGER = -(2^53 - 1) = -9007199254740991
     */
    private static final long JS_MIN_SAFE_INTEGER = -9007199254740991L;

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 在 JS 安全范围内, 写数字
        if (value >= JS_MIN_SAFE_INTEGER && value <= JS_MAX_SAFE_INTEGER) {
            gen.writeNumber(value);
        } else {
            // 超出范围, 写字符串
            gen.writeString(value.toString());
        }
    }
}
