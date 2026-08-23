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
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 一页不可变结果及读取更早数据的游标。
 *
 * @param items      当前页数据
 * @param nextCursor 下一页游标，没有更早数据时为 {@code null}
 * @param <T>        页元素类型
 */
public record MailPage<T>(List<T> items, @Nullable MailCursor nextCursor) {
    /**
     * 复制当前页数据为不可变列表。
     *
     * @param items      当前页数据
     * @param nextCursor 可选下一页游标
     */
    public MailPage {
        items = List.copyOf(ValidationUtils.requireNonNull(items, "items"));
    }
}
