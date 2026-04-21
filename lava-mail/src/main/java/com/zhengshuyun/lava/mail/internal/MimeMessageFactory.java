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
import com.zhengshuyun.lava.mail.MailSendRequest;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * MimeMessage 构造器
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MimeMessageFactory {

    private MimeMessageFactory() {
    }

    /**
     * 根据发信请求创建 MimeMessage
     *
     * @param session Jakarta Mail Session
     * @param request 发信请求
     * @return 组装好的 MimeMessage
     */
    public static MimeMessage create(Session session, MailSendRequest request) {
        Validate.notNull(session, "session must not be null");
        Validate.notNull(request, "request must not be null");

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(toInternetAddress(request.getFrom()));
            message.setRecipients(Message.RecipientType.TO, toInternetAddresses(request.getToList()));

            if (!request.getCcList().isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, toInternetAddresses(request.getCcList()));
            }
            if (!request.getBccList().isEmpty()) {
                message.setRecipients(Message.RecipientType.BCC, toInternetAddresses(request.getBccList()));
            }
            if (!request.getReplyToList().isEmpty()) {
                message.setReplyTo(toInternetAddresses(request.getReplyToList()));
            }

            message.setSubject(request.getSubject(), StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            applyContent(message, request);
            message.saveChanges();
            return message;
        } catch (Exception e) {
            throw new MailException("Failed to create mime message", e);
        }
    }

    private static void applyContent(MimeMessage message, MailSendRequest request) throws Exception {
        if (request.getAttachmentList().isEmpty()) {
            applyBodyOnlyContent(message, request.getTextBody(), request.getHtmlBody());
            return;
        }

        // 有附件时最外层必须是 mixed, 第一段放正文, 后续各段放附件.
        MimeMultipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(createBodyPart(request.getTextBody(), request.getHtmlBody()));

        for (MailAttachment attachment : request.getAttachmentList()) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(
                    new ByteArrayDataSource(attachment.getContent(), attachment.getContentType())
            ));
            attachmentPart.setFileName(attachment.getFileName());
            multipart.addBodyPart(attachmentPart);
        }

        message.setContent(multipart);
    }

    private static void applyBodyOnlyContent(MimeMessage message,
                                             @Nullable String textBody,
                                             @Nullable String htmlBody) throws Exception {
        if (textBody != null && htmlBody != null) {
            // 同时存在纯文本和 HTML 时, 使用 alternative 让客户端按能力选择展示版本.
            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(createTextBodyPart(textBody));
            multipart.addBodyPart(createHtmlBodyPart(htmlBody));
            message.setContent(multipart);
            return;
        }

        if (htmlBody != null) {
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            return;
        }

        message.setText(textBody == null ? "" : textBody, StandardCharsets.UTF_8.name());
    }

    private static MimeBodyPart createBodyPart(@Nullable String textBody, @Nullable String htmlBody) throws Exception {
        if (textBody != null && htmlBody != null) {
            // 带附件且同时存在 text/html 时, 正文段本身也要保持 alternative 结构.
            MimeMultipart alternative = new MimeMultipart("alternative");
            alternative.addBodyPart(createTextBodyPart(textBody));
            alternative.addBodyPart(createHtmlBodyPart(htmlBody));

            MimeBodyPart alternativeBody = new MimeBodyPart();
            alternativeBody.setContent(alternative);
            return alternativeBody;
        }

        if (htmlBody != null) {
            return createHtmlBodyPart(htmlBody);
        }

        return createTextBodyPart(textBody == null ? "" : textBody);
    }

    private static MimeBodyPart createTextBodyPart(String textBody) throws Exception {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText(textBody, StandardCharsets.UTF_8.name());
        return bodyPart;
    }

    private static MimeBodyPart createHtmlBodyPart(String htmlBody) throws Exception {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(htmlBody, "text/html; charset=UTF-8");
        return bodyPart;
    }

    private static InternetAddress[] toInternetAddresses(Iterable<MailAddress> addressList) throws Exception {
        Validate.notNull(addressList, "addressList must not be null");

        List<InternetAddress> result = new ArrayList<>();
        for (MailAddress address : addressList) {
            result.add(toInternetAddress(Validate.notNull(address, "addressList contains null element")));
        }
        return result.toArray(InternetAddress[]::new);
    }

    private static InternetAddress toInternetAddress(MailAddress address) throws Exception {
        Validate.notNull(address, "address must not be null");
        InternetAddress internetAddress = new InternetAddress(address.getAddress());
        // personal 对应邮件头里的显示名称, 常见客户端会展示为 "Name <address>".
        if (address.getPersonal() != null) {
            internetAddress.setPersonal(address.getPersonal(), StandardCharsets.UTF_8.name());
        }
        return internetAddress;
    }
}
