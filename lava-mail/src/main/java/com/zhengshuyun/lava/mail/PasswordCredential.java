/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

/** 用户名和密码认证凭证；诊断文本始终隐藏密码。 */
public final class PasswordCredential implements MailCredential {
    private final String username;
    private final String password;

    /**
     * 创建密码凭证。密码属于不透明值，不会去除首尾空白。
     *
     * @param username 登录用户名
     * @param password 登录密码
     */
    public PasswordCredential(String username, String password) {
        this.username = requireNonBlankWithoutControls(username, "username");
        this.password = ValidationUtils.requireNonNull(password, "password");
    }

    @Override
    public String username() {
        return username;
    }

    /**
     * 返回真实密码，仅可用于认证，调用方不得记录。
     *
     * @return 原始密码
     */
    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return "PasswordCredential[username=" + username + ", password=<redacted>]";
    }

    static String requireNonBlank(String value, String name) {
        return ValidationUtils.requireNotBlank(value, name + " must not be blank").strip();
    }

    static String requireNonBlankPreserved(String value, String name) {
        return ValidationUtils.requireNotBlank(value, name + " must not be blank");
    }

    static String requireNonBlankWithoutControls(String value, String name) {
        String normalized = requireNonBlank(value, name);
        if (normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " must not contain control characters");
        }
        return normalized;
    }
}
