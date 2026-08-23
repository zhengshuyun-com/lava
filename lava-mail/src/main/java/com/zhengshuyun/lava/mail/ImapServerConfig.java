/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import java.time.Duration;

/**
 * 不可变的 IMAP 连接配置。
 *
 * @param host           服务器主机名
 * @param port           服务器端口，范围为 1 到 65535
 * @param securityMode   传输安全模式
 * @param defaultFolder  查询未指定文件夹时使用的默认文件夹
 * @param connectTimeout 建立连接的超时时间
 * @param readTimeout    读取响应的超时时间
 * @param writeTimeout   写入请求的超时时间
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
     * @param host           服务器主机名
     * @param port           服务器端口
     * @param securityMode   传输安全模式
     * @param defaultFolder  默认文件夹
     * @param connectTimeout 建连超时
     * @param readTimeout    读取超时
     * @param writeTimeout   写入超时
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
