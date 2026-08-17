/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import java.time.Duration;
import com.zhengshuyun.lava.core.lang.ValidationUtils;

/**
 * 不可变的 IMAP 连接配置。
 *
 * @param host 服务器主机名
 * @param port 服务器端口，范围为 1 到 65535
 * @param securityMode 传输安全模式
 * @param defaultFolder 查询未指定文件夹时使用的默认文件夹
 * @param connectTimeout 建立连接的超时时间
 * @param readTimeout 读取响应的超时时间
 * @param writeTimeout 写入请求的超时时间
 */
public record ImapServerConfig(
        String host,
        int port,
        MailSecurityMode securityMode,
        String defaultFolder,
        Duration connectTimeout,
        Duration readTimeout,
        Duration writeTimeout) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 校验并规范化 IMAP 配置。
     *
     * @param host 服务器主机名
     * @param port 服务器端口
     * @param securityMode 传输安全模式
     * @param defaultFolder 默认文件夹
     * @param connectTimeout 建连超时
     * @param readTimeout 读取超时
     * @param writeTimeout 写入超时
     */
    public ImapServerConfig {
        host = SmtpServerConfig.requireHost(host);
        SmtpServerConfig.requirePort(port);
        ValidationUtils.requireNonNull(securityMode, "securityMode");
        defaultFolder = ValidationUtils.requireNotBlank(
                defaultFolder, "defaultFolder must not be blank").strip();
        if (defaultFolder.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("defaultFolder must not contain control characters");
        }
        connectTimeout = SmtpServerConfig.requireTimeout(connectTimeout, "connectTimeout");
        readTimeout = SmtpServerConfig.requireTimeout(readTimeout, "readTimeout");
        writeTimeout = SmtpServerConfig.requireTimeout(writeTimeout, "writeTimeout");
    }

    /**
     * 使用 {@code INBOX} 和 30 秒默认超时创建隐式 TLS 配置。
     *
     * @param host 服务器主机名
     * @param port 服务器端口
     * @return IMAP 配置
     */
    public static ImapServerConfig implicitTls(String host, int port) {
        return new ImapServerConfig(
                host, port, MailSecurityMode.SSL_TLS, "INBOX",
                DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }
}
