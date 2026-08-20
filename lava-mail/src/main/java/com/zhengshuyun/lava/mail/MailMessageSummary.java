/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * 邮箱列表返回的消息头、状态和附件元数据，不包含正文或附件字节。
 *
 * @param id                稳定的 IMAP 消息标识
 * @param internetMessageId Message-ID 消息头，没有时为 {@code null}
 * @param from              发件人列表
 * @param to                主送收件人列表
 * @param cc                抄送收件人列表
 * @param subject           主题，没有主题时为空字符串
 * @param sentAt            发件时间，没有时为 {@code null}
 * @param receivedAt        收件时间，没有时为 {@code null}
 * @param unread            是否未读
 * @param attachments       附件元数据列表
 */
public record MailMessageSummary(
        MailMessageId id,
        @Nullable String internetMessageId,
        List<MailAddress> from,
        List<MailAddress> to,
        List<MailAddress> cc,
        String subject,
        @Nullable Instant sentAt,
        @Nullable Instant receivedAt,
        boolean unread,
        List<MailAttachmentInfo> attachments) {
    /**
     * 校验摘要并复制所有列表字段。
     *
     * @param id                消息标识
     * @param internetMessageId 可选 Message-ID
     * @param from              发件人列表
     * @param to                主送收件人列表
     * @param cc                抄送收件人列表
     * @param subject           主题
     * @param sentAt            可选发件时间
     * @param receivedAt        可选收件时间
     * @param unread            是否未读
     * @param attachments       附件元数据列表
     */
    public MailMessageSummary {
        ValidationUtils.requireNonNull(id, "id");
        from = List.copyOf(ValidationUtils.requireNonNull(from, "from"));
        to = List.copyOf(ValidationUtils.requireNonNull(to, "to"));
        cc = List.copyOf(ValidationUtils.requireNonNull(cc, "cc"));
        ValidationUtils.requireNonNull(subject, "subject");
        attachments = List.copyOf(ValidationUtils.requireNonNull(attachments, "attachments"));
    }
}
