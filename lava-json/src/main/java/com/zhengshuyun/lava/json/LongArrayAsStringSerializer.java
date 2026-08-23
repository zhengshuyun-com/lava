/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhengshuyun.lava.json;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

final class LongArrayAsStringSerializer extends ValueSerializer<long[]> {

    /**
     * 负责数组元素输出形态的 long 序列化器。
     */
    private final LongAsStringSerializer elementSerializer;

    LongArrayAsStringSerializer() {
        this(new LongAsStringSerializer());
    }

    private LongArrayAsStringSerializer(LongAsStringSerializer elementSerializer) {
        this.elementSerializer = elementSerializer;
    }

    /**
     * 按属性上下文创建数组元素序列化器。
     *
     * @param context  当前 Jackson 序列化上下文
     * @param property 当前属性；根值序列化时可能为 null
     * @return 当前或按属性格式调整后的数组序列化器
     */
    @Override
    public ValueSerializer<?> createContextual(
            SerializationContext context, @Nullable BeanProperty property) {
        ValueSerializer<?> contextual = elementSerializer.createContextual(context, property);
        if (contextual == elementSerializer) {
            return this;
        }
        return new LongArrayAsStringSerializer((LongAsStringSerializer) contextual);
    }

    /**
     * 将 long 数组写为 JSON 数组，并按元素序列化器输出每个值。
     *
     * @param value     待序列化的 long 数组
     * @param generator JSON 输出生成器
     * @param context   当前 Jackson 序列化上下文
     */
    @Override
    public void serialize(long[] value, JsonGenerator generator, SerializationContext context) {
        generator.writeStartArray(value, value.length);
        for (long item : value) {
            elementSerializer.write(item, generator);
        }
        generator.writeEndArray();
    }
}
