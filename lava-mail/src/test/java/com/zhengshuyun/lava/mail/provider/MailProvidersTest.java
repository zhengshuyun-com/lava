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

package com.zhengshuyun.lava.mail.provider;

import com.zhengshuyun.lava.mail.MailSecurityMode;
import com.zhengshuyun.lava.mail.MailFolder;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮件厂商预置单元测试
 *
 * @author Toint
 * @since 2026/4/21
 */
@DisplayName("邮件厂商预置单元测试")
class MailProvidersTest {

    @Test
    @DisplayName("hotmail() - 应返回 Outlook.com 默认预置")
    void testHotmailPreset() {
        MailProviderPreset preset = MailProviders.hotmail();

        assertEquals("hotmail", preset.getName());
        assertEquals("outlook.office365.com", preset.getImapServerConfig().getHost());
        assertEquals(993, preset.getImapServerConfig().getPort());
        assertEquals(MailSecurityMode.SSL_TLS, preset.getImapServerConfig().getSecurityMode());
        assertEquals(MailFolder.INBOX.getValue(), preset.getImapServerConfig().getDefaultFolder());
        assertEquals("smtp-mail.outlook.com", preset.getSmtpServerConfig().getHost());
        assertEquals(587, preset.getSmtpServerConfig().getPort());
        assertEquals(MailSecurityMode.STARTTLS, preset.getSmtpServerConfig().getSecurityMode());
        assertTrue(preset.hasOAuth2Profile());
        assertNotNull(preset.getOAuth2Profile());
        assertEquals(
                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                preset.getOAuth2Profile().getTokenEndpoint()
        );
        assertEquals(
                List.of(
                        "offline_access",
                        "https://outlook.office.com/IMAP.AccessAsUser.All",
                        "https://outlook.office.com/SMTP.Send"
                ),
                preset.getOAuth2Profile().getScopes()
        );

        OAuth2RefreshTokenCredential credential = MailProviders.hotmail()
                .createOAuth2CredentialBuilder()
                .setUsername("user@hotmail.com")
                .setClientId("client-id")
                .setRefreshToken("refresh-token")
                .build();

        assertEquals("user@hotmail.com", credential.getUsername());
        assertEquals("client-id", credential.getClientId());
        assertEquals("refresh-token", credential.getRefreshToken());
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/token", credential.getTokenEndpoint());
        assertEquals(
                List.of(
                        "offline_access",
                        "https://outlook.office.com/IMAP.AccessAsUser.All",
                        "https://outlook.office.com/SMTP.Send"
                ),
                credential.getScopes()
        );
    }

    @Test
    @DisplayName("qq() - 应返回 QQ 邮箱默认预置")
    void testQqPreset() {
        MailProviderPreset preset = MailProviders.qq();

        assertEquals("qq", preset.getName());
        assertEquals("imap.qq.com", preset.getImapServerConfig().getHost());
        assertEquals(993, preset.getImapServerConfig().getPort());
        assertEquals(MailSecurityMode.SSL_TLS, preset.getImapServerConfig().getSecurityMode());
        assertEquals("smtp.qq.com", preset.getSmtpServerConfig().getHost());
        assertEquals(465, preset.getSmtpServerConfig().getPort());
        assertEquals(MailSecurityMode.SSL_TLS, preset.getSmtpServerConfig().getSecurityMode());
        assertFalse(preset.hasOAuth2Profile());
        assertNull(preset.getOAuth2Profile());
    }

    @Test
    @DisplayName("createOAuth2CredentialBuilder() - 无 OAuth2 预置时应抛出异常")
    void testCreateOAuth2CredentialBuilderWithoutOAuth2Profile() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> MailProviders.qq().createOAuth2CredentialBuilder()
        );

        assertTrue(exception.getMessage().contains("oauth2Profile is not configured"));
    }
}
