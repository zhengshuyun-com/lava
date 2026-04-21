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

import com.zhengshuyun.lava.mail.MailAddress;
import com.zhengshuyun.lava.mail.MailAttachment;
import com.zhengshuyun.lava.mail.MailSendRequest;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MimeMessageFactory 单元测试
 *
 * @author Toint
 * @since 2026/4/21
 */
@DisplayName("MimeMessageFactory 单元测试")
class MimeMessageFactoryTest {

    @Test
    @DisplayName("create() - 纯文本邮件应正确组装")
    void testCreatePlainTextMessage() throws Exception {
        MailSendRequest request = MailSendRequest.builder()
                .setFrom(MailAddress.builder()
                        .setAddress("sender@qq.com")
                        .setPersonal("Sender")
                        .build())
                .addTo(MailAddress.builder()
                        .setAddress("receiver@qq.com")
                        .build())
                .setSubject("Plain Mail")
                .setTextBody("hello lava")
                .build();

        MimeMessage message = MimeMessageFactory.create(createSession(), request);

        assertEquals("Plain Mail", message.getSubject());
        assertEquals(1, message.getFrom().length);
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals("sender@qq.com", from.getAddress());
        assertEquals("Sender", from.getPersonal());
        assertEquals(1, message.getAllRecipients().length);
        assertEquals("hello lava", message.getContent());
    }

    @Test
    @DisplayName("create() - 文本加 HTML 邮件应生成 alternative multipart")
    void testCreateAlternativeMessage() throws Exception {
        MailSendRequest request = MailSendRequest.builder()
                .setFrom(MailAddress.builder().setAddress("sender@qq.com").build())
                .addTo(MailAddress.builder().setAddress("receiver@qq.com").build())
                .setSubject("HTML Mail")
                .setTextBody("plain body")
                .setHtmlBody("<p>html body</p>")
                .build();

        MimeMessage message = MimeMessageFactory.create(createSession(), request);

        assertTrue(message.isMimeType("multipart/alternative"));
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        assertEquals(2, multipart.getCount());
        assertTrue(multipart.getBodyPart(0).isMimeType("text/plain"));
        assertTrue(multipart.getBodyPart(1).isMimeType("text/html"));
        assertEquals("plain body", multipart.getBodyPart(0).getContent());
        assertEquals("<p>html body</p>", multipart.getBodyPart(1).getContent());
    }

    @Test
    @DisplayName("create() - 带附件邮件应生成 mixed multipart")
    void testCreateMessageWithAttachment() throws Exception {
        MailSendRequest request = MailSendRequest.builder()
                .setFrom(MailAddress.builder().setAddress("sender@qq.com").build())
                .addTo(MailAddress.builder().setAddress("receiver@qq.com").build())
                .setSubject("Attachment Mail")
                .setTextBody("see attachment")
                .addAttachment(MailAttachment.builder()
                        .setFileName("code.txt")
                        .setContentType("text/plain")
                        .setContent("123456".getBytes())
                        .build())
                .build();

        MimeMessage message = MimeMessageFactory.create(createSession(), request);

        assertTrue(message.isMimeType("multipart/mixed"));
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        assertEquals(2, multipart.getCount());

        BodyPart attachmentPart = multipart.getBodyPart(1);
        assertEquals("code.txt", attachmentPart.getFileName());
        assertTrue(attachmentPart.isMimeType("text/plain"));
        assertArrayEquals("123456".getBytes(), attachmentPart.getInputStream().readAllBytes());
    }

    private static Session createSession() {
        return Session.getInstance(new Properties());
    }
}
