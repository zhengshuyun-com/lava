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
import com.zhengshuyun.lava.mail.MailMessage;
import com.zhengshuyun.lava.mail.MailSendRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MailMessageParser 单元测试
 *
 * @author Toint
 * @since 2026/4/21
 */
@DisplayName("MailMessageParser 单元测试")
class MailMessageParserTest {

    @Test
    @DisplayName("parse() - 纯文本邮件应解析基础字段")
    void testParsePlainTextMessage() throws Exception {
        MailSendRequest request = MailSendRequest.builder()
                .setFrom(MailAddress.builder().setAddress("sender@qq.com").build())
                .addTo(MailAddress.builder().setAddress("receiver@qq.com").build())
                .setSubject("Plain Mail")
                .setTextBody("hello lava")
                .build();

        MimeMessage message = MimeMessageFactory.create(Session.getInstance(new Properties()), request);
        MailMessage parsed = MailMessageParser.parse(message, true, true);

        assertEquals("Plain Mail", parsed.getSubject());
        assertEquals(1, parsed.getFromList().size());
        assertEquals("sender@qq.com", parsed.getFromList().get(0).getAddress());
        assertEquals("hello lava", parsed.getTextBody());
        assertNull(parsed.getHtmlBody());
        assertTrue(parsed.getAttachmentList().isEmpty());
    }

    @Test
    @DisplayName("parse() - HTML 和附件邮件应完整解析")
    void testParseHtmlAndAttachmentMessage() throws Exception {
        MailSendRequest request = MailSendRequest.builder()
                .setFrom(MailAddress.builder().setAddress("sender@qq.com").build())
                .addTo(MailAddress.builder().setAddress("receiver@qq.com").build())
                .setSubject("Attachment Mail")
                .setTextBody("plain body")
                .setHtmlBody("<p>html body</p>")
                .addAttachment(MailAttachment.builder()
                        .setFileName("code.txt")
                        .setContentType("text/plain")
                        .setContent("123456".getBytes())
                        .build())
                .build();

        MimeMessage message = MimeMessageFactory.create(Session.getInstance(new Properties()), request);
        MailMessage parsed = MailMessageParser.parse(message, true, true);

        assertEquals("plain body", parsed.getTextBody());
        assertEquals("<p>html body</p>", parsed.getHtmlBody());
        assertEquals(1, parsed.getAttachmentList().size());
        assertEquals("code.txt", parsed.getAttachmentList().get(0).getFileName());
        assertArrayEquals("123456".getBytes(), parsed.getAttachmentList().get(0).getContent());
    }
}
