/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;

/**
 * 将模块发信模型转换为 Jakarta MIME 消息，并在分配 MIME 结构前执行大小限制。
 */
final class MimeMessageFactory {
    private MimeMessageFactory() {
    }

    static MimeMessage create(
            Session session, MailSendRequest request, MailLimits limits, Clock clock) {
        ValidationUtils.requireNonNull(session, "session");
        ValidationUtils.requireNonNull(request, "request");
        ValidationUtils.requireNonNull(limits, "limits");
        ValidationUtils.requireNonNull(clock, "clock");
        enforceLimits(request, limits);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(toInternetAddress(request.from()));
            setRecipients(message, Message.RecipientType.TO, request.to());
            setRecipients(message, Message.RecipientType.CC, request.cc());
            setRecipients(message, Message.RecipientType.BCC, request.bcc());
            if (!request.replyTo().isEmpty()) {
                message.setReplyTo(toInternetAddresses(request.replyTo()));
            }
            message.setSubject(request.subject(), StandardCharsets.UTF_8.name());
            message.setSentDate(Date.from(clock.instant()));
            setContent(message, request);
            message.saveChanges();
            return message;
        } catch (Exception exception) {
            throw MailFailures.wrap("create MIME message", exception);
        }
    }

    private static void enforceLimits(MailSendRequest request, MailLimits limits) {
        long total = 0;
        // 正文与全部附件共同消耗单次操作预算，防止多个合法小部件组合后突破总上限。
        total = addBody(total, request.textBody(), limits);
        total = addBody(total, request.htmlBody(), limits);
        for (MailAttachment attachment : request.attachments()) {
            long size = attachment.size();
            if (size > limits.maxAttachmentBytes()) {
                throw sizeLimit("attachment exceeds maxAttachmentBytes");
            }
            total = addToTotal(total, size, limits);
        }
    }

    private static long addBody(long total, @Nullable String body, MailLimits limits) {
        if (body == null) {
            return total;
        }
        long remainingTotal = limits.maxDecodedBytesPerOperation() - total;
        long maximum = Math.min(limits.maxBodyBytes(), remainingTotal);
        long size = boundedUtf8Length(body, maximum);
        if (size < 0) {
            if (limits.maxBodyBytes() <= remainingTotal) {
                throw sizeLimit("body exceeds maxBodyBytes");
            }
            throw sizeLimit("decoded content exceeds maxDecodedBytesPerOperation");
        }
        return addToTotal(total, size, limits);
    }

    /**
     * 计算 UTF-8 字节数，一旦超过 {@code maximum} 就立即返回 {@code -1}。
     */
    static long boundedUtf8Length(String value, long maximum) {
        long total = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            int width;
            if (current <= 0x7f) {
                width = 1;
            } else if (current <= 0x7ff) {
                width = 2;
            } else if (Character.isHighSurrogate(current)
                    && index + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(index + 1))) {
                width = 4;
                index++;
            } else if (Character.isSurrogate(current)) {
                // String#getBytes(UTF_8) 遇到未配对代理项时使用单字节问号替代。
                width = 1;
            } else {
                width = 3;
            }
            if (total > maximum - width) {
                return -1;
            }
            total += width;
        }
        return total;
    }

    private static long addToTotal(long total, long size, MailLimits limits) {
        if (size > limits.maxDecodedBytesPerOperation() - total) {
            throw sizeLimit("decoded content exceeds maxDecodedBytesPerOperation");
        }
        return total + size;
    }

    private static void setContent(MimeMessage message, MailSendRequest request)
            throws Exception {
        if (request.attachments().isEmpty()) {
            setMessageBody(message, request.textBody(), request.htmlBody());
            return;
        }
        MimeBodyPart body = createBody(request.textBody(), request.htmlBody());
        // 有附件时使用 multipart/mixed，正文自身仍可用 multipart/alternative 表达双格式。
        MimeMultipart mixed = new MimeMultipart("mixed");
        mixed.addBodyPart(body);
        for (MailAttachment attachment : request.attachments()) {
            MimeBodyPart part = new MimeBodyPart();
            part.setDataHandler(new DataHandler(
                    new ByteArrayDataSource(attachment.content(), attachment.contentType())));
            part.setFileName(attachment.fileName());
            part.setDisposition(MimeBodyPart.ATTACHMENT);
            mixed.addBodyPart(part);
        }
        message.setContent(mixed);
    }

    private static void setMessageBody(
            MimeMessage message, @Nullable String text, @Nullable String html)
            throws MessagingException {
        if (text != null && html != null) {
            message.setContent(createAlternative(text, html));
        } else if (html != null) {
            message.setContent(html, "text/html; charset=UTF-8");
        } else {
            message.setText(ValidationUtils.requireNonNull(text, "text"), StandardCharsets.UTF_8.name());
        }
    }

    private static MimeBodyPart createBody(@Nullable String text, @Nullable String html)
            throws MessagingException {
        MimeBodyPart body = new MimeBodyPart();
        if (text != null && html != null) {
            body.setContent(createAlternative(text, html));
        } else if (html != null) {
            body.setContent(html, "text/html; charset=UTF-8");
        } else {
            body.setText(ValidationUtils.requireNonNull(text, "text"), StandardCharsets.UTF_8.name());
        }
        return body;
    }

    private static MimeMultipart createAlternative(String text, String html)
            throws MessagingException {
        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(text, StandardCharsets.UTF_8.name());
        alternative.addBodyPart(textPart);
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=UTF-8");
        alternative.addBodyPart(htmlPart);
        return alternative;
    }

    private static void setRecipients(
            MimeMessage message, Message.RecipientType type, List<MailAddress> addresses)
            throws MessagingException {
        if (!addresses.isEmpty()) {
            message.setRecipients(type, toInternetAddresses(addresses));
        }
    }

    private static InternetAddress[] toInternetAddresses(List<MailAddress> addresses) {
        return addresses.stream().map(MimeMessageFactory::toInternetAddress).toArray(InternetAddress[]::new);
    }

    private static InternetAddress toInternetAddress(MailAddress address) {
        try {
            InternetAddress result = new InternetAddress(address.address(), true);
            if (address.displayName() != null) {
                result.setPersonal(address.displayName(), StandardCharsets.UTF_8.name());
            }
            result.validate();
            return result;
        } catch (Exception exception) {
            throw MailFailures.wrap("parse mail address", exception);
        }
    }

    private static MailException sizeLimit(String detail) {
        return new MailException(MailFailureKind.SIZE_LIMIT, detail);
    }
}
