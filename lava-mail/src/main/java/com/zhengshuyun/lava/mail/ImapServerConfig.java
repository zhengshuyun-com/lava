/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.core.lang.Validate;

/**
 * IMAP 服务器配置
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class ImapServerConfig {

    /**
     * 主机名
     */
    private final String host;

    /**
     * 端口
     */
    private final int port;

    /**
     * 连接安全模式
     */
    private final MailSecurityMode securityMode;

    /**
     * 默认文件夹
     */
    private final String defaultFolder;

    /**
     * 连接超时时间, 毫秒
     */
    private final int connectTimeoutMillis;

    /**
     * 读取超时时间, 毫秒
     */
    private final int readTimeoutMillis;

    /**
     * 写入超时时间, 毫秒
     */
    private final int writeTimeoutMillis;

    private ImapServerConfig(Builder builder) {
        this.host = Validate.notBlank(builder.host, "host must not be blank");
        this.port = builder.port;
        Validate.isTrue(port > 0, "port must be positive");
        this.securityMode = Validate.notNull(builder.securityMode, "securityMode must not be null");
        this.defaultFolder = Validate.notBlank(builder.defaultFolder, "defaultFolder must not be blank");
        this.connectTimeoutMillis = builder.connectTimeoutMillis;
        this.readTimeoutMillis = builder.readTimeoutMillis;
        this.writeTimeoutMillis = builder.writeTimeoutMillis;
        Validate.isTrue(connectTimeoutMillis >= 0, "connectTimeoutMillis must be >= 0");
        Validate.isTrue(readTimeoutMillis >= 0, "readTimeoutMillis must be >= 0");
        Validate.isTrue(writeTimeoutMillis >= 0, "writeTimeoutMillis must be >= 0");
    }

    /**
     * 创建 IMAP 配置构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取 IMAP 主机名
     *
     * @return 主机名
     */
    public String getHost() {
        return host;
    }

    /**
     * 获取 IMAP 端口
     *
     * @return 端口
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取安全模式
     *
     * @return 安全模式
     */
    public MailSecurityMode getSecurityMode() {
        return securityMode;
    }

    /**
     * 获取默认文件夹
     *
     * @return 默认文件夹
     */
    public String getDefaultFolder() {
        return defaultFolder;
    }

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间, 单位毫秒
     */
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /**
     * 获取读取超时时间
     *
     * @return 读取超时时间, 单位毫秒
     */
    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    /**
     * 获取写入超时时间
     *
     * @return 写入超时时间, 单位毫秒
     */
    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    /**
     * IMAP 服务器配置构建器
     */
    public static final class Builder {

        /**
         * 主机名
         */
        private String host;

        /**
         * 端口
         */
        private int port;

        /**
         * 连接安全模式
         */
        private MailSecurityMode securityMode = MailSecurityMode.SSL_TLS;

        /**
         * 默认文件夹
         */
        private String defaultFolder = MailFolder.INBOX.getValue();

        /**
         * 连接超时时间, 毫秒
         */
        private int connectTimeoutMillis = 30000;

        /**
         * 读取超时时间, 毫秒
         */
        private int readTimeoutMillis = 30000;

        /**
         * 写入超时时间, 毫秒
         */
        private int writeTimeoutMillis = 30000;

        private Builder() {
        }

        /**
         * 设置 IMAP 主机名
         *
         * @param host 主机名
         * @return this
         */
        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        /**
         * 设置 IMAP 端口
         *
         * @param port 端口
         * @return this
         */
        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * 设置安全模式
         *
         * @param securityMode 安全模式
         * @return this
         */
        public Builder setSecurityMode(MailSecurityMode securityMode) {
            this.securityMode = securityMode;
            return this;
        }

        /**
         * 设置默认文件夹
         *
         * @param defaultFolder 默认文件夹
         * @return this
         */
        public Builder setDefaultFolder(String defaultFolder) {
            this.defaultFolder = defaultFolder;
            return this;
        }

        /**
         * 设置默认文件夹枚举
         *
         * @param defaultFolder 默认文件夹枚举
         * @return this
         */
        public Builder setDefaultFolder(MailFolder defaultFolder) {
            this.defaultFolder = Validate.notNull(defaultFolder, "defaultFolder must not be null").getValue();
            return this;
        }

        /**
         * 设置连接超时时间
         *
         * @param connectTimeoutMillis 连接超时时间, 单位毫秒
         * @return this
         */
        public Builder setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        /**
         * 设置读取超时时间
         *
         * @param readTimeoutMillis 读取超时时间, 单位毫秒
         * @return this
         */
        public Builder setReadTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
            return this;
        }

        /**
         * 设置写入超时时间
         *
         * @param writeTimeoutMillis 写入超时时间, 单位毫秒
         * @return this
         */
        public Builder setWriteTimeoutMillis(int writeTimeoutMillis) {
            this.writeTimeoutMillis = writeTimeoutMillis;
            return this;
        }

        /**
         * 构建 IMAP 配置
         *
         * @return IMAP 配置
         */
        public ImapServerConfig build() {
            return new ImapServerConfig(this);
        }
    }
}
