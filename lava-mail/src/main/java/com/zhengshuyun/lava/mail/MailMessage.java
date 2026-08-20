/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

/**
 * 邮件摘要和经过大小限制的正文，不会在此对象中保留附件字节。
 *
 * <p>HTML 正文来自远端且未净化，渲染前必须由应用执行安全处理。</p>
 *
 * @param summary  消息摘要
 * @param textBody 第一段纯文本正文，没有时为 {@code null}
 * @param htmlBody 第一段 HTML 正文，没有时为 {@code null}
 */
public record MailMessage(
        MailMessageSummary summary,
        @Nullable String textBody,
        @Nullable String htmlBody) {
    /**
     * 校验邮件消息。
     *
     * @param summary  消息摘要
     * @param textBody 可选纯文本正文
     * @param htmlBody 可选 HTML 正文
     */
    public MailMessage {
        ValidationUtils.requireNonNull(summary, "summary");
    }
}
