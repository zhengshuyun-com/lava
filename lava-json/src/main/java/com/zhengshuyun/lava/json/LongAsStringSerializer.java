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

import com.fasterxml.jackson.annotation.JsonFormat;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

final class LongAsStringSerializer extends ValueSerializer<Long> {

    /**
     * 是否按 JSON number 而非 JSON string 输出 long 值。
     */
    private final boolean writeNumber;

    LongAsStringSerializer() {
        this(false);
    }

    private LongAsStringSerializer(boolean writeNumber) {
        this.writeNumber = writeNumber;
    }

    /**
     * 按当前属性上下文决定输出为 JSON 数字还是字符串。
     *
     * @param context  当前 Jackson 序列化上下文
     * @param property 当前属性；根值序列化时可能为 null
     * @return 当前或按属性格式调整后的序列化器
     */
    @Override
    public ValueSerializer<?> createContextual(
            SerializationContext context, @Nullable BeanProperty property) {
        if (property == null) {
            return this;
        }
        JsonFormat.Shape shape = property
                .findPropertyFormat(context.getConfig(), Long.class)
                .getShape();
        boolean numeric = shape == JsonFormat.Shape.NUMBER || shape == JsonFormat.Shape.NUMBER_INT;
        return numeric == writeNumber ? this : new LongAsStringSerializer(numeric);
    }

    /**
     * 序列化一个 long 值。
     *
     * @param value     待序列化的值
     * @param generator JSON 输出生成器
     * @param context   当前 Jackson 序列化上下文
     */
    @Override
    public void serialize(Long value, JsonGenerator generator, SerializationContext context) {
        write(value, generator);
    }

    void write(long value, JsonGenerator generator) {
        if (writeNumber) {
            generator.writeNumber(value);
        } else {
            generator.writeString(Long.toString(value));
        }
    }
}
