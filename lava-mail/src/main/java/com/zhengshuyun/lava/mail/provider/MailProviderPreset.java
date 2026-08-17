/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail.provider;

import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import com.zhengshuyun.lava.mail.SmtpServerConfig;
import org.jspecify.annotations.Nullable;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

/**
 * 邮件服务商的已知连接默认值。
 *
 * @param name 服务商名称
 * @param imap IMAP 默认配置
 * @param smtp SMTP 默认配置
 * @param oauth2 可选 OAuth2 配置
 */
public record MailProviderPreset(
        String name,
        ImapServerConfig imap,
        SmtpServerConfig smtp,
        @Nullable MailOAuth2Profile oauth2) {
    /**
     * 校验服务商预设。
     *
     * @param name 服务商名称
     * @param imap IMAP 默认配置
     * @param smtp SMTP 默认配置
     * @param oauth2 可选 OAuth2 配置
     */
    public MailProviderPreset {
        name = ValidationUtils.requireNotBlank(name, "name must not be blank").strip();
        ValidationUtils.requireNonNull(imap, "imap");
        ValidationUtils.requireNonNull(smtp, "smtp");
    }

    /**
     * 使用服务商 OAuth2 配置创建凭证。
     *
     * @param username 邮箱登录用户名
     * @param clientId OAuth2 client ID
     * @param refreshToken refresh token
     * @param clientSecret 可选 client secret
     * @return OAuth2 凭证
     * @throws IllegalStateException 此服务商未配置 OAuth2 时抛出
     */
    public OAuth2RefreshTokenCredential oauthCredential(
            String username, String clientId, String refreshToken, @Nullable String clientSecret) {
        if (oauth2 == null) {
            throw new IllegalStateException("OAuth2 is not configured for provider=" + name);
        }
        return oauth2.credential(username, clientId, refreshToken, clientSecret);
    }
}
