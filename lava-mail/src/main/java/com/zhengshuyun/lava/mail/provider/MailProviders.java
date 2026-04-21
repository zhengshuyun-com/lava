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
import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.MailFolder;
import com.zhengshuyun.lava.mail.SmtpServerConfig;

/**
 * 常见邮件厂商预置
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailProviders {

    /**
     * Hotmail 预置
     */
    private static final MailProviderPreset HOTMAIL = MailProviderPreset.builder()
            .setName("hotmail")
            .setImapServerConfig(ImapServerConfig.builder()
                    .setHost("outlook.office365.com")
                    .setPort(993)
                    .setSecurityMode(MailSecurityMode.SSL_TLS)
                    .setDefaultFolder(MailFolder.INBOX)
                    .build())
            .setSmtpServerConfig(SmtpServerConfig.builder()
                    .setHost("smtp-mail.outlook.com")
                    .setPort(587)
                    .setSecurityMode(MailSecurityMode.STARTTLS)
                    .build())
            .setOAuth2Profile(MailOAuth2Profile.builder()
                    .setTokenEndpoint("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                    .addScope("offline_access")
                    .addScope("https://outlook.office.com/IMAP.AccessAsUser.All")
                    .addScope("https://outlook.office.com/SMTP.Send")
                    .build())
            .build();

    /**
     * Outlook.com 预置
     */
    private static final MailProviderPreset OUTLOOK = MailProviderPreset.builder()
            .setName("outlook")
            .setImapServerConfig(HOTMAIL.getImapServerConfig())
            .setSmtpServerConfig(HOTMAIL.getSmtpServerConfig())
            .setOAuth2Profile(HOTMAIL.getOAuth2Profile())
            .build();

    /**
     * QQ 邮箱预置
     */
    private static final MailProviderPreset QQ = MailProviderPreset.builder()
            .setName("qq")
            .setImapServerConfig(ImapServerConfig.builder()
                    .setHost("imap.qq.com")
                    .setPort(993)
                    .setSecurityMode(MailSecurityMode.SSL_TLS)
                    .setDefaultFolder(MailFolder.INBOX)
                    .build())
            .setSmtpServerConfig(SmtpServerConfig.builder()
                    .setHost("smtp.qq.com")
                    .setPort(465)
                    .setSecurityMode(MailSecurityMode.SSL_TLS)
                    .build())
            .build();

    private MailProviders() {
    }

    /**
     * 获取 Hotmail 预置
     *
     * @return Hotmail 预置
     */
    public static MailProviderPreset hotmail() {
        return HOTMAIL;
    }

    /**
     * 获取 Outlook.com 预置
     *
     * @return Outlook.com 预置
     */
    public static MailProviderPreset outlook() {
        return OUTLOOK;
    }

    /**
     * 获取 QQ 邮箱预置
     *
     * @return QQ 邮箱预置
     */
    public static MailProviderPreset qq() {
        return QQ;
    }
}
