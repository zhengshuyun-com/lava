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

import java.util.OptionalLong;

/**
 * {@link OptionalLong} 的 JS 安全序列化器
 *
 * <p>{@link OptionalLong} 是独立类型而非 {@code Optional<Long>}, Jackson 用专用序列化器
 * 直接写 long, 因此 {@link SafeLongSerializer} 覆盖不到, 需要单独注册.
 * {@code Optional<Long>} 走元素序列化器, 不需要本类.
 *
 * <p>空值输出 {@code null}, 与 Jackson 默认行为一致.
 *
 * @author Toint
 * @since 2026/8/16
 */
public class SafeOptionalLongSerializer extends ValueSerializer<OptionalLong> {

    private final SafeLongSerializer valueSerializer;

    public SafeOptionalLongSerializer() {
        this(new SafeLongSerializer());
    }

    private SafeOptionalLongSerializer(SafeLongSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    /**
     * 把字段上的 {@code @JsonFormat(shape = ...)} 透传给内部值
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        ValueSerializer<?> contextual = valueSerializer.createContextual(ctxt, property);
        if (contextual == valueSerializer) {
            return this;
        }
        return new SafeOptionalLongSerializer((SafeLongSerializer) contextual);
    }

    @Override
    public void serialize(OptionalLong value, JsonGenerator gen, SerializationContext ctxt) {
        if (value.isEmpty()) {
            gen.writeNull();
            return;
        }
        valueSerializer.writeLong(value.getAsLong(), gen);
    }
}
