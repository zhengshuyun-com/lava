/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;
import jakarta.mail.Session;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import java.util.Properties;

/** 集中创建不携带凭证值的 Jakarta Mail Session。 */
final class MailSessionFactory {
    private MailSessionFactory() {
    }

    static Session smtp(SmtpServerConfig config, MailCredential credential) {
        ValidationUtils.requireNonNull(config, "config");
        Properties properties = common(
                "mail.smtp", config.host(), config.port(), config.securityMode(),
                config.connectTimeout(), config.readTimeout(), config.writeTimeout());
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.auth", "true");
        configureOAuth(properties, "mail.smtp", credential);
        return Session.getInstance(properties);
    }

    static Session imap(ImapServerConfig config, MailCredential credential) {
        ValidationUtils.requireNonNull(config, "config");
        Properties properties = common(
                "mail.imap", config.host(), config.port(), config.securityMode(),
                config.connectTimeout(), config.readTimeout(), config.writeTimeout());
        properties.setProperty("mail.store.protocol", "imap");
        properties.setProperty("mail.imap.auth", "true");
        configureOAuth(properties, "mail.imap", credential);
        return Session.getInstance(properties);
    }

    static String authenticationSecret(MailCredential credential, @Nullable String accessToken) {
        ValidationUtils.requireNonNull(credential, "credential");
        if (credential instanceof PasswordCredential passwordCredential) {
            return passwordCredential.password();
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new MailException(
                    MailFailureKind.AUTHENTICATION,
                    "OAuth2 authentication requires a valid access token");
        }
        return accessToken;
    }

    private static Properties common(
            String prefix,
            String host,
            int port,
            MailSecurityMode mode,
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout) {
        Properties properties = new Properties();
        properties.setProperty(prefix + ".host", host);
        properties.setProperty(prefix + ".port", Integer.toString(port));
        properties.setProperty(prefix + ".connectiontimeout", millis(connectTimeout));
        properties.setProperty(prefix + ".timeout", millis(readTimeout));
        properties.setProperty(prefix + ".writetimeout", millis(writeTimeout));
        switch (mode) {
            case SSL_TLS -> {
                properties.setProperty(prefix + ".ssl.enable", "true");
                properties.setProperty(prefix + ".ssl.checkserveridentity", "true");
                properties.setProperty(prefix + ".starttls.enable", "false");
                properties.setProperty(prefix + ".starttls.required", "false");
            }
            case STARTTLS -> {
                // required=true 禁止服务端不支持 STARTTLS 时静默降级为明文认证。
                properties.setProperty(prefix + ".ssl.enable", "false");
                properties.setProperty(prefix + ".ssl.checkserveridentity", "true");
                properties.setProperty(prefix + ".starttls.enable", "true");
                properties.setProperty(prefix + ".starttls.required", "true");
            }
            case PLAINTEXT -> {
                properties.setProperty(prefix + ".ssl.enable", "false");
                properties.setProperty(prefix + ".starttls.enable", "false");
                properties.setProperty(prefix + ".starttls.required", "false");
            }
        }
        return properties;
    }

    private static void configureOAuth(Properties properties, String prefix, MailCredential credential) {
        ValidationUtils.requireNonNull(credential, "credential");
        if (credential instanceof OAuth2RefreshTokenCredential) {
            // 明确只允许 XOAUTH2，避免 token 被 LOGIN 或 PLAIN 机制当作普通密码发送。
            properties.setProperty(prefix + ".auth.mechanisms", "XOAUTH2");
            properties.setProperty(prefix + ".auth.login.disable", "true");
            properties.setProperty(prefix + ".auth.plain.disable", "true");
        }
    }

    private static String millis(Duration duration) {
        return Long.toString(duration.toMillis());
    }
}
