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

import tools.jackson.databind.module.SimpleModule;

/**
 * Long值JS安全序列化
 *
 * <p>覆盖 {@code Long}, {@code long}, {@code long[]} 以及以 {@code Long} 为元素的集合.
 * {@code Map} 的 key 不受影响, JSON 对象的 key 本身就是字符串, 不存在精度问题.
 *
 * <p>字段上的 {@code @JsonFormat(shape = ...)} 优先于安全范围规则.
 *
 * @author Toint
 * @since 2026/1/2
 */
public class SafeLongModule extends SimpleModule {
    public SafeLongModule() {
        super(SafeLongModule.class.getName());
        addSerializer(Long.class, new SafeLongSerializer());
        addSerializer(Long.TYPE, new SafeLongSerializer());
        // long[] 走 Jackson 专用数组序列化器, 不查元素序列化器, 必须单独注册
        addSerializer(long[].class, new SafeLongArraySerializer());
    }
}
