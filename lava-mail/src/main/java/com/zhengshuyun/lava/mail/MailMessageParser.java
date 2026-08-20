/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import jakarta.mail.*;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 有界解析 MIME 树，并保持附件内容与消息摘要分离。
 */
final class MailMessageParser {
    private static final int BUFFER_SIZE = 8192;

    private MailMessageParser() {
    }

    static MailMessageSummary summary(Message message, MailMessageId id, MailLimits limits) {
        ValidationUtils.requireNonNull(message, "message");
        ValidationUtils.requireNonNull(id, "id");
        ValidationUtils.requireNonNull(limits, "limits");
        try {
            List<MailAttachmentInfo> attachments = new ArrayList<>();
            // 列表阶段只收集结构和元数据，不打开附件数据流。
            collectAttachments(message, 1, limits, attachments);
            String[] messageIds = message.getHeader("Message-ID");
            return new MailMessageSummary(
                    id,
                    messageIds == null || messageIds.length == 0 ? null : messageIds[0],
                    addresses(message.getFrom()),
                    addresses(message.getRecipients(Message.RecipientType.TO)),
                    addresses(message.getRecipients(Message.RecipientType.CC)),
                    message.getSubject() == null ? "" : message.getSubject(),
                    instant(message.getSentDate()),
                    instant(message.getReceivedDate()),
                    !message.isSet(Flags.Flag.SEEN),
                    attachments);
        } catch (Exception exception) {
            throw MailFailures.wrap("parse message summary", exception);
        }
    }

    static MailMessage message(Message source, MailMessageId id, MailLimits limits) {
        MailMessageSummary summary = summary(source, id, limits);
        BodyCollector bodies = new BodyCollector(limits);
        try {
            // 纯文本和 HTML 各保留遍历中遇到的第一段，并共享单次解码预算。
            collectBodies(source, 1, bodies);
            return new MailMessage(summary, bodies.text, bodies.html);
        } catch (Exception exception) {
            throw MailFailures.wrap("parse message body", exception);
        }
    }

    static long downloadAttachment(
            Message source, int requestedIndex, OutputStream destination, MailLimits limits) {
        if (requestedIndex < 0) {
            throw new IllegalArgumentException("attachmentIndex must not be negative");
        }
        ValidationUtils.requireNonNull(destination, "destination");
        try {
            AttachmentDownload search = new AttachmentDownload(requestedIndex, destination, limits);
            findAttachment(source, 1, search);
            if (!search.found) {
                throw new MailException(MailFailureKind.PARSING, "attachment index does not exist");
            }
            return search.written;
        } catch (MailException exception) {
            throw exception;
        } catch (Exception exception) {
            throw MailFailures.wrap("download attachment", exception);
        }
    }

    private static void collectAttachments(
            Part part, int depth, MailLimits limits, List<MailAttachmentInfo> result) throws Exception {
        requireDepth(depth, limits);
        if (isAttachment(part)) {
            int index = result.size();
            String fileName = part.getFileName();
            result.add(new MailAttachmentInfo(
                    index,
                    fileName == null || fileName.isBlank() ? "attachment-" + index : fileName,
                    baseContentType(part.getContentType()),
                    part.getSize(),
                    contentId(part)));
            return;
        }
        Object nested = nestedContent(part);
        if (nested instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                collectAttachments(multipart.getBodyPart(index), depth + 1, limits, result);
            }
        } else if (nested instanceof Message message) {
            collectAttachments(message, depth + 1, limits, result);
        }
    }

    private static void collectBodies(Part part, int depth, BodyCollector result) throws Exception {
        requireDepth(depth, result.limits);
        if (isAttachment(part)) {
            return;
        }
        if (part.isMimeType("text/plain") && result.text == null) {
            result.text = readText(part, result);
            return;
        }
        if (part.isMimeType("text/html") && result.html == null) {
            result.html = readText(part, result);
            return;
        }
        Object nested = nestedContent(part);
        if (nested instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                collectBodies(multipart.getBodyPart(index), depth + 1, result);
            }
        } else if (nested instanceof Message message) {
            collectBodies(message, depth + 1, result);
        }
    }

    private static void findAttachment(Part part, int depth, AttachmentDownload search)
            throws Exception {
        requireDepth(depth, search.limits);
        if (isAttachment(part)) {
            if (search.currentIndex++ == search.requestedIndex) {
                long maximum = Math.min(
                        search.limits.maxAttachmentBytes(),
                        search.limits.maxDecodedBytesPerOperation());
                try (InputStream input = part.getInputStream()) {
                    // 输入流由解析器打开并关闭；目标流属于调用方，只借用且不 flush/close。
                    search.written = copyBounded(input, search.destination, maximum);
                }
                search.found = true;
            }
            return;
        }
        Object nested = nestedContent(part);
        if (nested instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount() && !search.found; index++) {
                findAttachment(multipart.getBodyPart(index), depth + 1, search);
            }
        } else if (nested instanceof Message message) {
            findAttachment(message, depth + 1, search);
        }
    }

    private static @Nullable Object nestedContent(Part part) throws Exception {
        if (part.isMimeType("multipart/*") || part.isMimeType("message/rfc822")) {
            return part.getContent();
        }
        return null;
    }

    private static boolean isAttachment(Part part) throws Exception {
        String disposition = part.getDisposition();
        return Part.ATTACHMENT.equalsIgnoreCase(disposition) || part.getFileName() != null;
    }

    private static String readText(Part part, BodyCollector collector) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long remainingTotal = collector.limits.maxDecodedBytesPerOperation() - collector.totalBytes;
        long maximum = Math.min(collector.limits.maxBodyBytes(), remainingTotal);
        if (maximum < 1) {
            throw sizeLimit("decoded content exceeds maxDecodedBytesPerOperation");
        }
        long count;
        try (InputStream input = part.getInputStream()) {
            // 先限制解码字节再创建字符串，防止传输编码展开后发生无界分配。
            count = copyBounded(input, output, maximum);
        }
        collector.totalBytes += count;
        return output.toString(charset(part.getContentType()));
    }

    private static long copyBounded(InputStream input, OutputStream output, long maximum)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        while (true) {
            int count = input.read(buffer);
            if (count < 0) {
                return total;
            }
            if (total > maximum - count) {
                throw sizeLimit("decoded MIME part exceeds configured size limit");
            }
            output.write(buffer, 0, count);
            total += count;
        }
    }

    private static Charset charset(String contentType) {
        try {
            String name = new ContentType(contentType).getParameter("charset");
            return name == null ? StandardCharsets.UTF_8 : Charset.forName(name);
        } catch (Exception exception) {
            throw new MailException(MailFailureKind.PARSING, "message declares an invalid charset", exception);
        }
    }

    private static String baseContentType(String value) {
        try {
            return new ContentType(value).getBaseType().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "application/octet-stream";
        }
    }

    private static List<MailAddress> addresses(@Nullable Address[] values) {
        if (values == null) {
            return List.of();
        }
        List<MailAddress> result = new ArrayList<>(values.length);
        for (Address address : values) {
            if (!(address instanceof InternetAddress internetAddress)) {
                throw new MailException(MailFailureKind.PARSING, "message contains a non-Internet address");
            }
            result.add(new MailAddress(internetAddress.getAddress(), internetAddress.getPersonal()));
        }
        return List.copyOf(result);
    }

    private static @Nullable Instant instant(@Nullable Date date) {
        return date == null ? null : date.toInstant();
    }

    private static @Nullable String normalizeContentId(@Nullable String value) {
        if (value == null) {
            return null;
        }
        if (value.length() >= 2 && value.charAt(0) == '<' && value.charAt(value.length() - 1) == '>') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static @Nullable String contentId(Part part) throws Exception {
        String[] values = part.getHeader("Content-ID");
        return values == null || values.length == 0 ? null : normalizeContentId(values[0]);
    }

    private static void requireDepth(int depth, MailLimits limits) {
        if (depth > limits.maxMimeDepth()) {
            throw sizeLimit("MIME nesting exceeds maxMimeDepth");
        }
    }

    private static MailException sizeLimit(String message) {
        return new MailException(MailFailureKind.SIZE_LIMIT, message);
    }

    private static final class BodyCollector {
        private final MailLimits limits;
        private long totalBytes;
        private @Nullable String text;
        private @Nullable String html;

        private BodyCollector(MailLimits limits) {
            this.limits = limits;
        }
    }

    private static final class AttachmentDownload {
        private final int requestedIndex;
        private final OutputStream destination;
        private final MailLimits limits;
        private int currentIndex;
        private long written;
        private boolean found;

        private AttachmentDownload(int requestedIndex, OutputStream destination, MailLimits limits) {
            this.requestedIndex = requestedIndex;
            this.destination = destination;
            this.limits = limits;
        }
    }
}
