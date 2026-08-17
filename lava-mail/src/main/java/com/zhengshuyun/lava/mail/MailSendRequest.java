/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import org.jspecify.annotations.Nullable;

import java.util.List;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/**
 * 提交一封邮件所需的完整数据。
 *
 * <p>至少需要一个主送、抄送或密送收件人，并且纯文本正文和 HTML 正文至少提供一种。
 * 所有集合在构造时都会复制为不可变列表。</p>
 *
 * @param from 发件人
 * @param to 主送收件人
 * @param cc 抄送收件人
 * @param bcc 密送收件人
 * @param replyTo 回复地址
 * @param subject 主题，允许为空字符串但不能包含控制字符
 * @param textBody 纯文本正文
 * @param htmlBody HTML 正文
 * @param attachments 附件列表
 */
public record MailSendRequest(
        MailAddress from,
        List<MailAddress> to,
        List<MailAddress> cc,
        List<MailAddress> bcc,
        List<MailAddress> replyTo,
        String subject,
        @Nullable String textBody,
        @Nullable String htmlBody,
        List<MailAttachment> attachments) {

    /**
     * 校验发信请求并复制所有列表字段。
     *
     * @param from 发件人
     * @param to 主送收件人
     * @param cc 抄送收件人
     * @param bcc 密送收件人
     * @param replyTo 回复地址
     * @param subject 主题
     * @param textBody 可选纯文本正文
     * @param htmlBody 可选 HTML 正文
     * @param attachments 附件列表
     */
    public MailSendRequest {
        ValidationUtils.requireNonNull(from, "from");
        to = List.copyOf(ValidationUtils.requireNonNull(to, "to"));
        cc = List.copyOf(ValidationUtils.requireNonNull(cc, "cc"));
        bcc = List.copyOf(ValidationUtils.requireNonNull(bcc, "bcc"));
        replyTo = List.copyOf(ValidationUtils.requireNonNull(replyTo, "replyTo"));
        ValidationUtils.requireNonNull(subject, "subject");
        attachments = List.copyOf(ValidationUtils.requireNonNull(attachments, "attachments"));
        if (to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) {
            throw new IllegalArgumentException("at least one recipient is required");
        }
        if (subject.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("subject must not contain control characters");
        }
        if (textBody == null && htmlBody == null) {
            throw new IllegalArgumentException("textBody or htmlBody is required");
        }
    }

    /**
     * 创建只有主送收件人和纯文本正文的简单发信请求。
     *
     * @param from 发件人
     * @param to 主送收件人
     * @param subject 主题
     * @param body 纯文本正文
     * @return 发信请求
     */
    public static MailSendRequest text(
            MailAddress from, List<MailAddress> to, String subject, String body) {
        return new MailSendRequest(
                from, to, List.of(), List.of(), List.of(), subject,
                ValidationUtils.requireNonNull(body, "body"), null, List.of());
    }
}
