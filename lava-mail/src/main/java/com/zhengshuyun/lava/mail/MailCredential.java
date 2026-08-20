/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

/**
 * SMTP 和 IMAP 共用的认证凭证。
 */
public sealed interface MailCredential permits PasswordCredential, OAuth2RefreshTokenCredential {
    /**
     * 返回登录用户名。
     *
     * @return 已去除首尾空白的用户名
     */
    String username();
}
