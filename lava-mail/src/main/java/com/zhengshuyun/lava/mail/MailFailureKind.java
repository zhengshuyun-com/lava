/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

/**
 * 稳定且可供程序判断的邮件操作失败类别。
 */
public enum MailFailureKind {
    /**
     * 客户端参数、文件夹或游标配置错误。
     */
    CONFIGURATION,
    /**
     * 用户名、密码或 OAuth2 认证失败。
     */
    AUTHENTICATION,
    /**
     * TLS 握手或证书验证失败。
     */
    TLS,
    /**
     * DNS、建连、断线或套接字错误。
     */
    CONNECTION,
    /**
     * 连接、读取或写入超时。
     */
    TIMEOUT,
    /**
     * SMTP、IMAP 或 OAuth2 端点违反协议预期。
     */
    PROTOCOL,
    /**
     * 地址、消息、字符集或响应内容无法解析。
     */
    PARSING,
    /**
     * 正文、附件、响应或 MIME 深度超过限制。
     */
    SIZE_LIMIT
}
