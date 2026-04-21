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

import com.zhengshuyun.lava.mail.MailSecurityMode;
import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import com.zhengshuyun.lava.mail.PasswordCredential;
import com.zhengshuyun.lava.mail.SmtpServerConfig;
import jakarta.mail.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MailSessionFactory 单元测试
 *
 * @author Toint
 * @since 2026/4/21
 */
@DisplayName("MailSessionFactory 单元测试")
class MailSessionFactoryTest {

    @Test
    @DisplayName("createSmtpSession() - 密码凭证应生成 SMTP 基础属性")
    void testCreateSmtpSessionWithPasswordCredential() {
        SmtpServerConfig config = SmtpServerConfig.builder()
                .setHost("smtp.qq.com")
                .setPort(465)
                .setSecurityMode(MailSecurityMode.SSL_TLS)
                .setConnectTimeoutMillis(1000)
                .setReadTimeoutMillis(2000)
                .setWriteTimeoutMillis(3000)
                .build();

        Session session = MailSessionFactory.createSmtpSession(
                config,
                PasswordCredential.builder()
                        .setUsername("test@qq.com")
                        .setPassword("auth-code")
                        .build(),
                null
        );

        Properties props = session.getProperties();
        assertEquals("smtp.qq.com", props.getProperty("mail.smtp.host"));
        assertEquals("465", props.getProperty("mail.smtp.port"));
        assertEquals("true", props.getProperty("mail.smtp.auth"));
        assertEquals("true", props.getProperty("mail.smtp.ssl.enable"));
        assertEquals("false", props.getProperty("mail.smtp.starttls.enable"));
        assertEquals("false", props.getProperty("mail.smtp.starttls.required"));
        assertEquals("1000", props.getProperty("mail.smtp.connectiontimeout"));
        assertEquals("2000", props.getProperty("mail.smtp.timeout"));
        assertEquals("3000", props.getProperty("mail.smtp.writetimeout"));
        assertNull(props.getProperty("mail.smtp.auth.mechanisms"));
    }

    @Test
    @DisplayName("createSmtpSession() - STARTTLS 模式应强制升级")
    void testCreateSmtpSessionWithRequiredStartTls() {
        SmtpServerConfig config = SmtpServerConfig.builder()
                .setHost("smtp-mail.outlook.com")
                .setPort(587)
                .setSecurityMode(MailSecurityMode.STARTTLS)
                .build();

        Session session = MailSessionFactory.createSmtpSession(
                config,
                PasswordCredential.builder()
                        .setUsername("test@hotmail.com")
                        .setPassword("auth-code")
                        .build(),
                null
        );

        Properties props = session.getProperties();
        assertEquals("false", props.getProperty("mail.smtp.ssl.enable"));
        assertEquals("true", props.getProperty("mail.smtp.starttls.enable"));
        assertEquals("true", props.getProperty("mail.smtp.starttls.required"));
    }

    @Test
    @DisplayName("createImapSession() - OAuth2 凭证应生成 XOAUTH2 属性")
    void testCreateImapSessionWithOAuth2Credential() {
        ImapServerConfig config = ImapServerConfig.builder()
                .setHost("outlook.office365.com")
                .setPort(993)
                .setSecurityMode(MailSecurityMode.SSL_TLS)
                .build();

        Session session = MailSessionFactory.createImapSession(
                config,
                OAuth2RefreshTokenCredential.builder()
                        .setUsername("test@hotmail.com")
                        .setClientId("client-id")
                        .setRefreshToken("refresh-token")
                        .setTokenEndpoint("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                        .addScope("https://outlook.office.com/IMAP.AccessAsUser.All")
                        .build(),
                "access-token"
        );

        Properties props = session.getProperties();
        assertEquals("outlook.office365.com", props.getProperty("mail.imap.host"));
        assertEquals("993", props.getProperty("mail.imap.port"));
        assertEquals("true", props.getProperty("mail.imap.auth"));
        assertEquals("true", props.getProperty("mail.imap.ssl.enable"));
        assertEquals("XOAUTH2", props.getProperty("mail.imap.auth.mechanisms"));
        assertEquals("true", props.getProperty("mail.imap.auth.login.disable"));
        assertEquals("true", props.getProperty("mail.imap.auth.plain.disable"));
    }
}
