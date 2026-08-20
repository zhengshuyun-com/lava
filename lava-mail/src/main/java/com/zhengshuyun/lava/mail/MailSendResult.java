/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * SMTP 提交成功后的结果。
 *
 * @param messageId 客户端生成的 Message-ID，没有时为 {@code null}
 * @param sentAt    提交完成时间
 */
public record MailSendResult(@Nullable String messageId, Instant sentAt) {
    /**
     * 校验发信结果。
     *
     * @param messageId 可选 Message-ID
     * @param sentAt    提交完成时间
     */
    public MailSendResult {
        ValidationUtils.requireNonNull(sentAt, "sentAt");
    }
}
