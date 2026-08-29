/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.zhengshuyun.lava.pay.alipay.exception.AlipayPayProtocolException;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * 支付宝本地日期时间解析工具。
 */
public final class AlipayPayDateTimeUtils {
    private static final DateTimeFormatter DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter();

    private AlipayPayDateTimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 解析可选支付宝本地时间。
     *
     * @param value 协议文本
     * @param name  字段名
     * @return 本地时间；未返回时为 {@code null}
     */
    public static @Nullable LocalDateTime parseOptional(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME);
        } catch (DateTimeParseException exception) {
            throw new AlipayPayProtocolException("支付宝响应字段 " + name + " 不是有效时间");
        }
    }

    /**
     * 解析必填支付宝本地时间。
     *
     * @param value 协议文本
     * @param name  字段名
     * @return 本地时间
     */
    public static LocalDateTime parseRequired(@Nullable String value, String name) {
        LocalDateTime parsed = parseOptional(value, name);
        if (parsed == null) {
            throw new AlipayPayProtocolException("支付宝响应缺少字段 " + name);
        }
        return parsed;
    }
}
