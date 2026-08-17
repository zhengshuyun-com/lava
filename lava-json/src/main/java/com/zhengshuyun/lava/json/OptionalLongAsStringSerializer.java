/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.json;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.OptionalLong;

final class OptionalLongAsStringSerializer extends ValueSerializer<OptionalLong> {

    /** 负责存在值输出形态的 long 序列化器。 */
    private final LongAsStringSerializer valueSerializer;

    OptionalLongAsStringSerializer() {
        this(new LongAsStringSerializer());
    }

    private OptionalLongAsStringSerializer(LongAsStringSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    /**
     * 按属性上下文创建可选 long 的值序列化器。
     *
     * @param context 当前 Jackson 序列化上下文
     * @param property 当前属性；根值序列化时可能为 null
     * @return 当前或按属性格式调整后的序列化器
     */
    @Override
    public ValueSerializer<?> createContextual(
            SerializationContext context, @Nullable BeanProperty property) {
        ValueSerializer<?> contextual = valueSerializer.createContextual(context, property);
        if (contextual == valueSerializer) {
            return this;
        }
        return new OptionalLongAsStringSerializer((LongAsStringSerializer) contextual);
    }

    /**
     * 序列化可选 long；空值写为 JSON null，存在的值按配置写为字符串或数字。
     *
     * @param value 待序列化的可选 long
     * @param generator JSON 输出生成器
     * @param context 当前 Jackson 序列化上下文
     */
    @Override
    public void serialize(OptionalLong value, JsonGenerator generator, SerializationContext context) {
        if (value.isEmpty()) {
            generator.writeNull();
        } else {
            valueSerializer.write(value.getAsLong(), generator);
        }
    }
}
