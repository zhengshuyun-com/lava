/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/** 使用 refresh token 换取 access token 的 OAuth 2 认证凭证。 */
public final class OAuth2RefreshTokenCredential implements MailCredential {
    private final String username;
    private final String clientId;
    private final String refreshToken;
    private final URI tokenEndpoint;
    private final List<String> scopes;
    private final @Nullable String clientSecret;

    /**
     * 创建 OAuth2 refresh token 凭证。
     *
     * <p>token endpoint 必须是无 user-info 和 fragment 的绝对 HTTPS URI。client ID、refresh
     * token 和 client secret 均按不透明值保留首尾空白。</p>
     *
     * @param username 邮箱登录用户名
     * @param clientId OAuth2 client ID
     * @param refreshToken refresh token
     * @param tokenEndpoint token endpoint
     * @param scopes 请求的 scope 列表，每项只能包含一个 scope token
     * @param clientSecret 可选 client secret
     */
    public OAuth2RefreshTokenCredential(
            String username,
            String clientId,
            String refreshToken,
            URI tokenEndpoint,
            List<String> scopes,
            @Nullable String clientSecret) {
        this.username = PasswordCredential.requireNonBlankWithoutControls(username, "username");
        this.clientId = PasswordCredential.requireNonBlankPreserved(clientId, "clientId");
        this.refreshToken = PasswordCredential.requireNonBlankPreserved(refreshToken, "refreshToken");
        this.tokenEndpoint = requireHttpsEndpoint(tokenEndpoint);
        ValidationUtils.requireNonNull(scopes, "scopes");
        this.scopes = scopes.stream()
                .map(OAuth2RefreshTokenCredential::requireScope)
                .toList();
        if (this.scopes.isEmpty()) {
            throw new IllegalArgumentException("scopes must not be empty");
        }
        this.clientSecret = clientSecret == null
                ? null
                : PasswordCredential.requireNonBlankPreserved(clientSecret, "clientSecret");
    }

    @Override
    public String username() {
        return username;
    }

    /**
     * 返回原始 client ID。
     *
     * @return 原始 client ID
     */
    public String clientId() {
        return clientId;
    }

    /**
     * 返回真实 refresh token，仅可用于换取 token，调用方不得记录。
     *
     * @return 原始 refresh token
     */
    public String refreshToken() {
        return refreshToken;
    }

    /**
     * 返回 token endpoint。
     *
     * @return token endpoint
     */
    public URI tokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * 返回不可变的 scope 列表。
     *
     * @return 不可变的 scope 列表
     */
    public List<String> scopes() {
        return scopes;
    }

    /**
     * 返回真实 client secret，仅可用于换取 token，调用方不得记录。
     *
     * @return client secret；未配置时为 {@code null}
     */
    public @Nullable String clientSecret() {
        return clientSecret;
    }

    @Override
    public String toString() {
        return "OAuth2RefreshTokenCredential[username=" + username
                + ", clientId=<redacted>"
                + ", refreshToken=<redacted>, tokenEndpoint=<redacted>, scopes=" + scopes.size()
                + ", clientSecret=<redacted>]";
    }

    private static URI requireHttpsEndpoint(URI endpoint) {
        ValidationUtils.requireNonNull(endpoint, "tokenEndpoint");
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
                || endpoint.getHost() == null
                || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null) {
            throw new IllegalArgumentException("tokenEndpoint must be an absolute HTTPS URI without user-info or fragment");
        }
        return endpoint;
    }

    private static String requireScope(String scope) {
        String result = PasswordCredential.requireNonBlank(scope, "scope");
        if (result.codePoints().anyMatch(Character::isWhitespace)
                || result.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("scope must be a single OAuth scope token");
        }
        return result;
    }
}
