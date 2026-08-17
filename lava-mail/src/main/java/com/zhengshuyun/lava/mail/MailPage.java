/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import org.jspecify.annotations.Nullable;

import java.util.List;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/**
 * 一页不可变结果及读取更早数据的游标。
 *
 * @param items 当前页数据
 * @param nextCursor 下一页游标，没有更早数据时为 {@code null}
 * @param <T> 页元素类型
 */
public record MailPage<T>(List<T> items, @Nullable MailCursor nextCursor) {
    /**
     * 复制当前页数据为不可变列表。
     *
     * @param items 当前页数据
     * @param nextCursor 可选下一页游标
     */
    public MailPage {
        items = List.copyOf(ValidationUtils.requireNonNull(items, "items"));
    }
}
