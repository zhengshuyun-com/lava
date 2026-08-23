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
 * 不可变的 SMTP 连接配置。
 *
 * @param host           服务器主机名
 * @param port           服务器端口，范围为 1 到 65535
 * @param securityMode   传输安全模式
 * @param connectTimeout 建立连接的超时时间
 * @param readTimeout    读取响应的超时时间
 * @param writeTimeout   写入请求的超时时间
 */
public record SmtpServerConfig(
        String host,
        int port,
        MailSecurityMode securityMode,
        Duration connectTimeout,
        Duration readTimeout,
        Duration writeTimeout) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 校验并规范化 SMTP 配置。
     *
     * @param host           服务器主机名
     * @param port           服务器端口
     * @param securityMode   传输安全模式
     * @param connectTimeout 建连超时
     * @param readTimeout    读取超时
     * @param writeTimeout   写入超时
     */
    public SmtpServerConfig {
        host = requireHost(host);
        requirePort(port);
        ValidationUtils.requireNonNull(securityMode, "securityMode");
        connectTimeout = requireTimeout(connectTimeout, "connectTimeout");
        readTimeout = requireTimeout(readTimeout, "readTimeout");
        writeTimeout = requireTimeout(writeTimeout, "writeTimeout");
    }

    /**
     * 使用 30 秒默认超时创建强制 STARTTLS 的配置。
     *
     * @param host 服务器主机名
     * @param port 服务器端口
     * @return SMTP 配置
     */
    public static SmtpServerConfig startTls(String host, int port) {
        return new SmtpServerConfig(
                host, port, MailSecurityMode.STARTTLS,
                DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    /**
     * 使用 30 秒默认超时创建隐式 TLS 配置。
     *
     * @param host 服务器主机名
     * @param port 服务器端口
     * @return SMTP 配置
     */
    public static SmtpServerConfig implicitTls(String host, int port) {
        return new SmtpServerConfig(
                host, port, MailSecurityMode.SSL_TLS,
                DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    static String requireHost(String host) {
        String normalized = ValidationUtils.requireNotBlank(host, "host must not be blank").strip();
        if (containsControlOrWhitespace(normalized)) {
            throw new IllegalArgumentException("host must be a non-blank host name");
        }
        return normalized;
    }

    static void requirePort(int port) {
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    static Duration requireTimeout(Duration timeout, String name) {
        ValidationUtils.requireNonNull(timeout, name);
        long millis = timeout.toMillis();
        if (timeout.isNegative() || millis < 1 || millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " must be positive and at most 2147483647 ms");
        }
        return timeout;
    }

    private static boolean containsControlOrWhitespace(String value) {
        return value.codePoints().anyMatch(codePoint ->
                Character.isISOControl(codePoint) || Character.isWhitespace(codePoint));
    }
}
