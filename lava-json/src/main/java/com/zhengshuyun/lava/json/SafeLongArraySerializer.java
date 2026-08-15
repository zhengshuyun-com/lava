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

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import org.jspecify.annotations.Nullable;

/**
 * {@code long[]} 的 JS 安全序列化器
 *
 * <p>Jackson 对 {@code long[]} 使用专用的数组序列化器, 直接写数字而不查元素序列化器,
 * 因此 {@link SafeLongSerializer} 覆盖不到 {@code long[]} 字段, 需要单独注册.
 * {@code Long[]} 走元素序列化器, 不需要本类.
 *
 * @author Toint
 * @since 2026/8/16
 */
public class SafeLongArraySerializer extends ValueSerializer<long[]> {

    private final SafeLongSerializer elementSerializer;

    public SafeLongArraySerializer() {
        this(new SafeLongSerializer());
    }

    private SafeLongArraySerializer(SafeLongSerializer elementSerializer) {
        this.elementSerializer = elementSerializer;
    }

    /**
     * 把字段上的 {@code @JsonFormat(shape = ...)} 透传给元素规则
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        ValueSerializer<?> contextual = elementSerializer.createContextual(ctxt, property);
        if (contextual == elementSerializer) {
            return this;
        }
        return new SafeLongArraySerializer((SafeLongSerializer) contextual);
    }

    @Override
    public void serialize(long[] value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeStartArray(value, value.length);
        for (long item : value) {
            elementSerializer.writeLong(item, gen);
        }
        gen.writeEndArray();
    }
}
