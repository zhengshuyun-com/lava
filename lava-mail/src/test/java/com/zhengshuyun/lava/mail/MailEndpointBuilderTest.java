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

package com.zhengshuyun.lava.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮件端点 Builder 单元测试
 *
 * @author Toint
 * @since 2026/4/21
 */
@DisplayName("邮件端点 Builder 单元测试")
class MailEndpointBuilderTest {

    @Test
    @DisplayName("MailSender.build() - 未配置 SMTP 应抛出异常")
    void testBuildMailSenderWithoutSmtpConfig() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MailSender.builder()
                        .setCredential(PasswordCredential.builder()
                                .setUsername("test@qq.com")
                                .setPassword("auth-code")
                                .build())
                        .build()
        );

        assertTrue(exception.getMessage().contains("smtpServerConfig must not be null"));
    }

    @Test
    @DisplayName("MailReader.build() - 未配置 IMAP 应抛出异常")
    void testBuildMailReaderWithoutImapConfig() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MailReader.builder()
                        .setCredential(PasswordCredential.builder()
                                .setUsername("test@qq.com")
                                .setPassword("auth-code")
                                .build())
                        .build()
        );

        assertTrue(exception.getMessage().contains("imapServerConfig must not be null"));
    }

    @Test
    @DisplayName("build() - 发件端与收件端应构建成功")
    void testBuildMailSenderAndMailReader() {
        MailReader reader = MailReader.builder()
                .setImapServerConfig(ImapServerConfig.builder()
                        .setHost("imap.qq.com")
                        .setPort(993)
                        .setSecurityMode(MailSecurityMode.SSL_TLS)
                        .build())
                .setCredential(PasswordCredential.builder()
                        .setUsername("test@qq.com")
                        .setPassword("auth-code")
                        .build())
                .build();

        MailSender sender = MailSender.builder()
                .setSmtpServerConfig(SmtpServerConfig.builder()
                        .setHost("smtp.qq.com")
                        .setPort(465)
                        .setSecurityMode(MailSecurityMode.SSL_TLS)
                        .build())
                .setCredential(PasswordCredential.builder()
                        .setUsername("test@qq.com")
                        .setPassword("auth-code")
                        .build())
                .build();

        assertNotNull(reader);
        assertNotNull(sender);
    }

    @Test
    @DisplayName("默认集合字段 - 应返回空集合而不是 null")
    void testDefaultEmptyCollections() {
        MailSendRequest request = MailSendRequest.builder()
                .setFrom(MailAddress.builder().setAddress("sender@qq.com").build())
                .addTo(MailAddress.builder().setAddress("receiver@qq.com").build())
                .setSubject("test")
                .build();

        assertNotNull(request.getCcList());
        assertNotNull(request.getBccList());
        assertNotNull(request.getReplyToList());
        assertNotNull(request.getAttachmentList());
        assertTrue(request.getCcList().isEmpty());
        assertTrue(request.getBccList().isEmpty());
        assertTrue(request.getReplyToList().isEmpty());
        assertTrue(request.getAttachmentList().isEmpty());
    }
}
