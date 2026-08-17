/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.jspecify.annotations.Nullable;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

/** 协调 MIME 构造、SMTP 提交和可选 OAuth2 token 生命周期。 */
final class MailSenderEngine implements AutoCloseable {
    private final MailClientOptions options;
    private final @Nullable OAuth2AccessTokenProvider tokenProvider;

    private MailSenderEngine(
            MailClientOptions options, @Nullable OAuth2AccessTokenProvider tokenProvider) {
        this.options = options;
        this.tokenProvider = tokenProvider;
    }

    static MailSenderEngine create(MailCredential credential, MailClientOptions options) {
        OAuth2AccessTokenProvider provider = credential instanceof OAuth2RefreshTokenCredential oauth
                ? new OAuth2AccessTokenProvider(
                        oauth, OAuth2TokenClient.createDefault(),
                        options.clock(), options.tokenRefreshAhead())
                : null;
        return new MailSenderEngine(options, provider);
    }

    MailSendResult send(
            SmtpServerConfig config, MailCredential credential, MailSendRequest request) {
        ValidationUtils.requireNonNull(config, "config");
        Session session = MailSessionFactory.smtp(config, credential);
        // 先完成纯本地的 MIME 构造与大小校验，避免无效请求触发 OAuth2 网络刷新。
        MimeMessage message = MimeMessageFactory.create(
                session, request, options.limits(), options.clock());
        String token = tokenProvider == null ? null : tokenProvider.accessToken();
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(
                    config.host(), config.port(), credential.username(),
                    MailSessionFactory.authenticationSecret(credential, token));
            Address[] recipients = message.getAllRecipients();
            if (recipients == null || recipients.length == 0) {
                throw new MailException(MailFailureKind.CONFIGURATION, "message has no recipients");
            }
            transport.sendMessage(message, recipients);
            String[] messageIds = message.getHeader("Message-ID");
            return new MailSendResult(
                    messageIds == null || messageIds.length == 0 ? null : messageIds[0],
                    options.clock().instant());
        } catch (MailException exception) {
            throw exception;
        } catch (Exception exception) {
            throw MailFailures.wrap("send SMTP message", exception);
        }
    }

    @Override
    public void close() {
        if (tokenProvider != null) {
            tokenProvider.close();
        }
    }
}
