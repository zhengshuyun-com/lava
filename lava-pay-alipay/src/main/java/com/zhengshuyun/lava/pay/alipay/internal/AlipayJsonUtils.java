/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.json.JsonMapperFactory;

/**
 * 支付宝协议共享 JSON 编解码器。
 */
public final class AlipayJsonUtils {
    private static final JsonCodec CODEC = new JsonCodec(JsonMapperFactory.builder()
            .customize(builder -> builder.changeDefaultPropertyInclusion(
                    inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL)))
            .build());

    /** 禁止实例化支付宝 JSON 工具。 */
    private AlipayJsonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 返回忽略空值的线程安全编解码器。
     *
     * @return 共享 JSON 编解码器
     */
    public static JsonCodec codec() {
        return CODEC;
    }
}
