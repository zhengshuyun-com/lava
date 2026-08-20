/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 为单个邮件客户端提供带提前刷新窗口的线程安全 token 缓存。
 */
final class OAuth2AccessTokenProvider implements AutoCloseable {
    private final OAuth2RefreshTokenCredential credential;
    private final OAuth2TokenClient tokenClient;
    private final Clock clock;
    private final Duration refreshAhead;
    private final AtomicBoolean closed = new AtomicBoolean();
    private @Nullable OAuth2AccessToken cached;

    OAuth2AccessTokenProvider(
            OAuth2RefreshTokenCredential credential,
            OAuth2TokenClient tokenClient,
            Clock clock,
            Duration refreshAhead) {
        this.credential = ValidationUtils.requireNonNull(credential, "credential");
        this.tokenClient = ValidationUtils.requireNonNull(tokenClient, "tokenClient");
        this.clock = ValidationUtils.requireNonNull(clock, "clock");
        this.refreshAhead = ValidationUtils.requireNonNull(refreshAhead, "refreshAhead");
        if (refreshAhead.isNegative()) {
            throw new IllegalArgumentException("refreshAhead must not be negative");
        }
    }

    synchronized String accessToken() {
        if (closed.get()) {
            throw new IllegalStateException("OAuth2 token provider is closed");
        }
        if (cached == null || !cached.reusable(clock, refreshAhead)) {
            // 同步方法把并发刷新合并为一次网络请求，其他调用等待该请求完成。
            cached = tokenClient.fetchAccessToken(credential, clock);
        }
        return cached.value();
    }

    @Override
    public synchronized void close() {
        if (closed.compareAndSet(false, true)) {
            cached = null;
            tokenClient.close();
        }
    }
}
