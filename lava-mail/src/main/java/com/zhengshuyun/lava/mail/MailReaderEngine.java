/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhengshuyun.lava.mail;

import org.jspecify.annotations.Nullable;

import java.io.OutputStream;

/**
 * 协调 IMAP 操作、客户端限制和可选 OAuth2 token 生命周期。
 */
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
