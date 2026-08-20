/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import java.time.Clock;

/**
 * 由单个发件器或收件器实例独占的 token 传输客户端。
 */
@FunctionalInterface
interface OAuth2TokenClient extends AutoCloseable {
    OAuth2AccessToken fetchAccessToken(OAuth2RefreshTokenCredential credential, Clock clock);

    static OAuth2TokenClient createDefault() {
        return new DefaultOAuth2TokenClient();
    }

    @Override
    default void close() {
    }
}
