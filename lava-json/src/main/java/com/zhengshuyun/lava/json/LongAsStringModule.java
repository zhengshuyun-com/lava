/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.json;

import tools.jackson.databind.module.SimpleModule;

import java.util.OptionalLong;

/**
 * 显式启用后将每个 {@code long}/{@link Long} 值序列化为 JSON string 的模块。
 *
 * <p>该规则不随数值大小变化，覆盖基本类型数组、装箱数组、集合、嵌套值和 {@link OptionalLong}。
 * 字段可通过下列注解显式保持为 number：
 * {@code @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)}.
 */
public final class LongAsStringModule extends SimpleModule {

    /**
     * 创建固定的 long-as-string 模块。
     *
     * <p>注册后，long、long 数组和 {@link OptionalLong} 默认写为 JSON 字符串；
     * 单个字段仍可使用 {@code @JsonFormat(shape = NUMBER)} 覆盖。
     */
    public LongAsStringModule() {
        super(LongAsStringModule.class.getName());
        addSerializer(Long.class, new LongAsStringSerializer());
        addSerializer(Long.TYPE, new LongAsStringSerializer());
        addSerializer(long[].class, new LongArrayAsStringSerializer());
        addSerializer(OptionalLong.class, new OptionalLongAsStringSerializer());
    }
}
