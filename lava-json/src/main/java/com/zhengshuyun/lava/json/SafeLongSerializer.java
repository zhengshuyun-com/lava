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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import org.jspecify.annotations.Nullable;

/**
 * Long值JS安全序列化器
 *
 * <p>规则：</p>
 * <ul>
 *   <li>在 JS 安全范围内 (-2^53+1 到 2^53-1)：保持数字类型</li>
 *   <li>超出 JS 安全范围：转为字符串</li>
 * </ul>
 *
 * <p>字段上的 {@code @JsonFormat(shape = ...)} 优先于本规则:
 * {@code Shape.STRING} 强制输出字符串, {@code Shape.NUMBER} 强制输出数字.
 *
 * @author Toint
 * @since 2026/1/2
 */
public class SafeLongSerializer extends ValueSerializer<Long> {
    /**
     * JavaScript Number.MAX_SAFE_INTEGER = 2^53 - 1 = 9007199254740991
     */
    private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;

    /**
     * JavaScript Number.MIN_SAFE_INTEGER = -(2^53 - 1) = -9007199254740991
     */
    private static final long JS_MIN_SAFE_INTEGER = -9007199254740991L;

    /**
     * 字段注解指定的形状, null 表示按安全范围自动判断
     */
    @Nullable
    private final Shape forcedShape;

    public SafeLongSerializer() {
        this(null);
    }

    private SafeLongSerializer(@Nullable Shape forcedShape) {
        this.forcedShape = forcedShape;
    }

    /**
     * 判断值是否在 JavaScript 安全整数范围内
     */
    public static boolean isSafe(long value) {
        return value >= JS_MIN_SAFE_INTEGER && value <= JS_MAX_SAFE_INTEGER;
    }

    /**
     * 尊重字段上的 {@code @JsonFormat(shape = ...)}, 未指定时沿用安全范围判断
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, @Nullable BeanProperty property) {
        if (property == null) {
            return this;
        }

        Shape shape = property.findPropertyFormat(ctxt.getConfig(), Long.class).getShape();
        if (shape == Shape.STRING || shape == Shape.NUMBER || shape == Shape.NUMBER_INT) {
            return new SafeLongSerializer(shape);
        }
        return this;
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
            return;
        }

        writeLong(value, gen);
    }

    /**
     * 按当前规则写出单个值, 供数组序列化器复用
     */
    void writeLong(long value, JsonGenerator gen) {
        if (forcedShape == Shape.STRING) {
            gen.writeString(Long.toString(value));
            return;
        }
        if (forcedShape != null) {
            // NUMBER / NUMBER_INT: 调用方明确要数字, 不做安全范围转换
            gen.writeNumber(value);
            return;
        }

        if (isSafe(value)) {
            gen.writeNumber(value);
        } else {
            gen.writeString(Long.toString(value));
        }
    }
}
