/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.mail.internal;

import com.zhengshuyun.lava.mail.MailException;
import com.zhengshuyun.lava.core.lang.Validate;
import com.zhengshuyun.lava.mail.MailAddress;
import com.zhengshuyun.lava.mail.MailAttachment;
import com.zhengshuyun.lava.mail.MailMessage;
import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * 邮件消息解析器
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailMessageParser {

    private MailMessageParser() {
    }

    /**
     * 解析 MimeMessage
     */
    public static MailMessage parse(MimeMessage message, boolean includeBody, boolean includeAttachments) {
        Validate.notNull(message, "message must not be null");

        try {
            ContentAccumulator accumulator = new ContentAccumulator();
            // 统一递归遍历 MIME 树, 把正文和附件拆回 Lava 自己的稳定模型.
            parsePart(message, includeBody, includeAttachments, accumulator);
            return MailMessage.builder()
                    .setMessageId(message.getMessageID())
                    .setFromList(toMailAddresses(message.getFrom()))
                    .setToList(toMailAddresses(message.getRecipients(jakarta.mail.Message.RecipientType.TO)))
                    .setCcList(toMailAddresses(message.getRecipients(jakarta.mail.Message.RecipientType.CC)))
                    .setSubject(message.getSubject() == null ? "" : message.getSubject())
                    .setTextBody(accumulator.textBody)
                    .setHtmlBody(accumulator.htmlBody)
                    .setSentAt(toInstant(message.getSentDate()))
                    .setReceivedAt(toInstant(message.getReceivedDate()))
                    .setAttachmentList(accumulator.attachmentList)
                    .build();
        } catch (Exception e) {
            throw new MailException("Failed to parse mime message", e);
        }
    }

    private static void parsePart(Part part,
                                  boolean includeBody,
                                  boolean includeAttachments,
                                  ContentAccumulator accumulator) throws Exception {
        if (isAttachment(part)) {
            if (includeAttachments) {
                // 当前版本按 byte[] 暴露附件内容, 这里会把附件一次性读入内存.
                accumulator.attachmentList.add(MailAttachment.builder()
                        .setFileName(resolveFileName(part))
                        .setContentType(part.getContentType())
                        .setContent(part.getInputStream().readAllBytes())
                        .build());
            }
            return;
        }

        if (part.isMimeType("multipart/*")) {
            Object content = part.getContent();
            if (content instanceof Multipart multipart) {
                // multipart 只是容器, 真正的正文和附件还在子 part 里, 需要继续递归解析.
                for (int i = 0; i < multipart.getCount(); i++) {
                    parsePart(multipart.getBodyPart(i), includeBody, includeAttachments, accumulator);
                }
            }
            return;
        }

        if (!includeBody) {
            return;
        }

        // 纯文本和 HTML 正文各保留第一份, 避免 multipart 嵌套时被后续重复 part 覆盖.
        if (part.isMimeType("text/plain") && accumulator.textBody == null) {
            accumulator.textBody = (String) part.getContent();
            return;
        }
        if (part.isMimeType("text/html") && accumulator.htmlBody == null) {
            accumulator.htmlBody = (String) part.getContent();
        }
    }

    private static boolean isAttachment(Part part) throws Exception {
        String disposition = part.getDisposition();
        return Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || Part.INLINE.equalsIgnoreCase(disposition) && part.getFileName() != null
                || part.getFileName() != null;
    }

    private static String resolveFileName(Part part) throws Exception {
        String fileName = part.getFileName();
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        return MimeUtility.decodeText(fileName);
    }

    private static List<MailAddress> toMailAddresses(@Nullable Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return List.of();
        }

        List<MailAddress> result = new ArrayList<>(addresses.length);
        for (Address address : addresses) {
            if (address instanceof InternetAddress internetAddress) {
                result.add(MailAddress.builder()
                        .setAddress(internetAddress.getAddress())
                        .setPersonal(internetAddress.getPersonal())
                        .build());
            } else if (address != null) {
                result.add(MailAddress.builder().setAddress(address.toString()).build());
            }
        }
        return List.copyOf(result);
    }

    private static @Nullable Instant toInstant(@Nullable Date date) {
        return date == null ? null : date.toInstant();
    }

    private static final class ContentAccumulator {

        private @Nullable String textBody;

        private @Nullable String htmlBody;

        private final List<MailAttachment> attachmentList = new ArrayList<>();
    }
}
