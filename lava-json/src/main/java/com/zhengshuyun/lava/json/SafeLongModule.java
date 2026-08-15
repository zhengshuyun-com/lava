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

import java.util.OptionalLong;

/**
 * Long值JS安全序列化
 *
 * <p>覆盖范围:
 * <ul>
 *   <li>{@code Long}, {@code long}, {@code long[]}, {@code OptionalLong}</li>
 *   <li>以 {@code Long} 为元素的集合与数组: {@code List}, {@code Set}, {@code Iterable},
 *       {@code Stream}, {@code Long[]}, {@code Optional<Long>}</li>
 *   <li>{@code Map} 的 value, 以及声明为 {@code Object} 时按运行时类型解析出的 {@code Long}</li>
 *   <li>嵌套与多维: {@code List<List<Long>>}, {@code long[][]}, {@code List<long[]>}</li>
 * </ul>
 *
 * <p>不覆盖的情况:
 * <ul>
 *   <li>{@code Map} 的 key: JSON 对象的 key 本身就是字符串, 不存在精度问题</li>
 *   <li>{@code readTree(..)} 解析出的 {@code LongNode}: 树模型如实保留原文档的数字形态,
 *       转换会篡改调用方的文档. 需要转换时先转成对象再序列化</li>
 *   <li>{@code AtomicLong}, {@code LongAdder}, {@code BigInteger}: 并发计数器和大整数类型,
 *       不属于本模块语义, 需要时自行注册</li>
 * </ul>
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
        // 以下两个类型走 Jackson 专用序列化器, 不查元素序列化器, 必须单独注册
        addSerializer(long[].class, new SafeLongArraySerializer());
        addSerializer(OptionalLong.class, new SafeOptionalLongSerializer());
    }
}
