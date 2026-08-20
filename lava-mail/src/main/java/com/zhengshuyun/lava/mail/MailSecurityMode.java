/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

/**
 * SMTP 或 IMAP 的传输安全模式；明文模式必须显式选择。
 */
public enum MailSecurityMode {
    /**
     * 从建连开始即使用 TLS。
     */
    SSL_TLS,
    /**
     * 先建立普通连接，再强制升级为 TLS。
     */
    STARTTLS,
    /**
     * 不使用 TLS，只适合受控测试环境。
     */
    PLAINTEXT
}
