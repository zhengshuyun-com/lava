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

import com.zhengshuyun.lava.core.lang.Validate;
import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.MailCredential;
import com.zhengshuyun.lava.mail.MailSecurityMode;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import com.zhengshuyun.lava.mail.PasswordCredential;
import com.zhengshuyun.lava.mail.SmtpServerConfig;
import jakarta.mail.Session;
import org.jspecify.annotations.Nullable;

import java.util.Properties;

/**
 * Mail Session 工厂
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailSessionFactory {

    private MailSessionFactory() {
    }

    /**
     * 创建 SMTP Session
     */
    public static Session createSmtpSession(SmtpServerConfig config,
                                            MailCredential credential,
                                            @Nullable String accessToken) {
        Validate.notNull(config, "config must not be null");
        Validate.notNull(credential, "credential must not be null");
        validateOAuthAccessToken(credential, accessToken);

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", config.getHost());
        props.setProperty("mail.smtp.port", String.valueOf(config.getPort()));
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.connectiontimeout", String.valueOf(config.getConnectTimeoutMillis()));
        props.setProperty("mail.smtp.timeout", String.valueOf(config.getReadTimeoutMillis()));
        props.setProperty("mail.smtp.writetimeout", String.valueOf(config.getWriteTimeoutMillis()));
        applySecurityMode(props, "mail.smtp", config.getSecurityMode());

        if (credential instanceof OAuth2RefreshTokenCredential) {
            props.setProperty("mail.smtp.auth.mechanisms", "XOAUTH2");
            props.setProperty("mail.smtp.auth.login.disable", "true");
            props.setProperty("mail.smtp.auth.plain.disable", "true");
        }
        return Session.getInstance(props);
    }

    /**
     * 创建 IMAP Session
     */
    public static Session createImapSession(ImapServerConfig config,
                                            MailCredential credential,
                                            @Nullable String accessToken) {
        Validate.notNull(config, "config must not be null");
        Validate.notNull(credential, "credential must not be null");
        validateOAuthAccessToken(credential, accessToken);

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.host", config.getHost());
        props.setProperty("mail.imap.port", String.valueOf(config.getPort()));
        props.setProperty("mail.imap.auth", "true");
        props.setProperty("mail.imap.connectiontimeout", String.valueOf(config.getConnectTimeoutMillis()));
        props.setProperty("mail.imap.timeout", String.valueOf(config.getReadTimeoutMillis()));
        props.setProperty("mail.imap.writetimeout", String.valueOf(config.getWriteTimeoutMillis()));
        applySecurityMode(props, "mail.imap", config.getSecurityMode());

        if (credential instanceof OAuth2RefreshTokenCredential) {
            props.setProperty("mail.imap.auth.mechanisms", "XOAUTH2");
            props.setProperty("mail.imap.auth.login.disable", "true");
            props.setProperty("mail.imap.auth.plain.disable", "true");
        }
        return Session.getInstance(props);
    }

    /**
     * 根据凭证解析登录密钥
     */
    public static String resolvePassword(MailCredential credential, @Nullable String accessToken) {
        Validate.notNull(credential, "credential must not be null");
        if (credential instanceof PasswordCredential passwordCredential) {
            return passwordCredential.getPassword();
        }
        Validate.notBlank(accessToken, "accessToken must not be blank when using OAuth2 credential");
        return accessToken;
    }

    private static void applySecurityMode(Properties props, String prefix, MailSecurityMode securityMode) {
        switch (Validate.notNull(securityMode, "securityMode must not be null")) {
            case SSL_TLS -> {
                props.setProperty(prefix + ".ssl.enable", "true");
                props.setProperty(prefix + ".starttls.enable", "false");
                props.setProperty(prefix + ".starttls.required", "false");
            }
            case STARTTLS -> {
                props.setProperty(prefix + ".ssl.enable", "false");
                props.setProperty(prefix + ".starttls.enable", "true");
                // 显式要求升级到 TLS, 避免 STARTTLS 模式被静默降级为明文连接.
                props.setProperty(prefix + ".starttls.required", "true");
            }
            case NONE -> {
                props.setProperty(prefix + ".ssl.enable", "false");
                props.setProperty(prefix + ".starttls.enable", "false");
                props.setProperty(prefix + ".starttls.required", "false");
            }
        }
    }

    private static void validateOAuthAccessToken(MailCredential credential, @Nullable String accessToken) {
        if (credential instanceof OAuth2RefreshTokenCredential) {
            Validate.notBlank(accessToken, "accessToken must not be blank when using OAuth2 credential");
        }
    }
}
