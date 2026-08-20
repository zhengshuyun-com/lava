/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail.provider;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;

/**
 * 邮件服务商维护的 OAuth2 token endpoint 与 scope 组合。
 *
 * @param tokenEndpoint token endpoint
 * @param scopes        不可变的 scope 列表
 */
public record MailOAuth2Profile(URI tokenEndpoint, List<String> scopes) {
    /**
     * 校验并复制服务商 OAuth2 配置。
     *
     * @param tokenEndpoint token endpoint
     * @param scopes        scope 列表
     */
    public MailOAuth2Profile {
        ValidationUtils.requireNonNull(tokenEndpoint, "tokenEndpoint");
        scopes = List.copyOf(ValidationUtils.requireNonNull(scopes, "scopes"));
        // 复用凭证构造器执行 HTTPS endpoint 和 scope 的严格校验，避免两套规则产生偏差。
        new OAuth2RefreshTokenCredential(
                "validation@example.invalid", "validation", "validation",
                tokenEndpoint, scopes, null);
    }

    /**
     * 使用此服务商的 endpoint 和 scope 创建 OAuth2 凭证。
     *
     * @param username     邮箱登录用户名
     * @param clientId     OAuth2 client ID
     * @param refreshToken refresh token
     * @param clientSecret 可选 client secret
     * @return OAuth2 凭证
     */
    public OAuth2RefreshTokenCredential credential(
            String username,
            String clientId,
            String refreshToken,
            @Nullable String clientSecret) {
        return new OAuth2RefreshTokenCredential(
                username, clientId, refreshToken, tokenEndpoint, scopes, clientSecret);
    }
}
