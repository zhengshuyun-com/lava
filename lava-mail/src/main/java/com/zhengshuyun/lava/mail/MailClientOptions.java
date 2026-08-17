/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import java.time.Clock;
import java.time.Duration;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/**
 * 单个发件器或收件器实例共享的限制与时间行为。
 *
 * @param limits MIME 内容限制
 * @param clock 生成发送时间和判断 token 过期时间使用的时钟
 * @param tokenRefreshAhead OAuth2 token 到期前提前刷新的时间
 */
public record MailClientOptions(MailLimits limits, Clock clock, Duration tokenRefreshAhead) {
    /** 默认客户端选项。 */
    public static final MailClientOptions DEFAULT = new MailClientOptions(
            MailLimits.DEFAULT, Clock.systemUTC(), Duration.ofMinutes(1));

    /**
     * 校验客户端选项。
     *
     * @param limits MIME 内容限制
     * @param clock 客户端时钟
     * @param tokenRefreshAhead token 提前刷新时间
     */
    public MailClientOptions {
        ValidationUtils.requireNonNull(limits, "limits");
        ValidationUtils.requireNonNull(clock, "clock");
        ValidationUtils.requireNonNull(tokenRefreshAhead, "tokenRefreshAhead");
        if (tokenRefreshAhead.isNegative()) {
            throw new IllegalArgumentException("tokenRefreshAhead must not be negative");
        }
    }
}
