/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import org.jspecify.annotations.Nullable;

import java.io.OutputStream;

/** 协调 IMAP 操作、客户端限制和可选 OAuth2 token 生命周期。 */
final class MailReaderEngine implements AutoCloseable {
    private final MailClientOptions options;
    private final ImapMailReader reader = new ImapMailReader();
    private final @Nullable OAuth2AccessTokenProvider tokenProvider;

    private MailReaderEngine(
            MailClientOptions options, @Nullable OAuth2AccessTokenProvider tokenProvider) {
        this.options = options;
        this.tokenProvider = tokenProvider;
    }

    static MailReaderEngine create(MailCredential credential, MailClientOptions options) {
        OAuth2AccessTokenProvider provider = credential instanceof OAuth2RefreshTokenCredential oauth
                ? new OAuth2AccessTokenProvider(
                        oauth, OAuth2TokenClient.createDefault(),
                        options.clock(), options.tokenRefreshAhead())
                : null;
        return new MailReaderEngine(options, provider);
    }

    MailPage<MailMessageSummary> list(
            ImapServerConfig config, MailCredential credential, MailQuery query) {
        return reader.list(config, credential, accessToken(), query, options.limits());
    }

    MailMessage read(ImapServerConfig config, MailCredential credential, MailMessageId id) {
        return reader.read(config, credential, accessToken(), id, options.limits());
    }

    long download(
            ImapServerConfig config,
            MailCredential credential,
            MailMessageId id,
            int attachmentIndex,
            OutputStream destination) {
        return reader.download(
                config, credential, accessToken(), id, attachmentIndex, destination, options.limits());
    }

    private @Nullable String accessToken() {
        return tokenProvider == null ? null : tokenProvider.accessToken();
    }

    @Override
    public void close() {
        if (tokenProvider != null) {
            tokenProvider.close();
        }
    }
}
