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
import com.zhengshuyun.lava.mail.internal.ImapMailReader;
import com.zhengshuyun.lava.mail.internal.MailSessionFactory;
import com.zhengshuyun.lava.mail.internal.OAuth2AccessTokenProvider;
import jakarta.mail.Session;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 同步邮件收件客户端
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailReader {

    /**
     * IMAP 服务器配置
     */
    private final ImapServerConfig imapServerConfig;

    /**
     * 登录凭证
     */
    private final MailCredential credential;

    /**
     * OAuth2 token 提供器
     */
    private final OAuth2AccessTokenProvider accessTokenProvider;

    /**
     * IMAP 读取器
     */
    private final ImapMailReader imapMailReader;

    private MailReader(Builder builder) {
        this.imapServerConfig = Validate.notNull(builder.imapServerConfig, "imapServerConfig must not be null");
        this.credential = Validate.notNull(builder.credential, "credential must not be null");
        this.accessTokenProvider = new OAuth2AccessTokenProvider();
        this.imapMailReader = new ImapMailReader();
    }

    /**
     * 创建 MailReader 构建器
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 查询邮件列表
     *
     * @param query 查询条件
     * @return 查询结果, 无结果时返回空列表
     * @throws IllegalArgumentException query 为 null 时抛出
     * @throws MailException            底层邮件协议调用失败时抛出
     */
    public List<MailMessage> listMessages(MailQuery query) {
        Validate.notNull(query, "query must not be null");

        // IMAP 侧与 SMTP 一样, 统一在这里处理 OAuth2 access token 的懒获取.
        String accessToken = resolveAccessTokenIfNecessary();
        Session session = MailSessionFactory.createImapSession(imapServerConfig, credential, accessToken);
        return imapMailReader.listMessages(session, imapServerConfig, credential, query, accessToken);
    }

    /**
     * 获取 IMAP 服务器配置
     *
     * @return IMAP 配置
     */
    public ImapServerConfig getImapServerConfig() {
        return imapServerConfig;
    }

    /**
     * 获取当前登录凭证
     *
     * @return 登录凭证
     */
    public MailCredential getCredential() {
        return credential;
    }

    /**
     * 按凭证类型解析 access token
     *
     * @return OAuth2 场景返回 access token, 密码场景返回 null
     */
    private @Nullable String resolveAccessTokenIfNecessary() {
        if (credential instanceof OAuth2RefreshTokenCredential oauth2Credential) {
            return accessTokenProvider.getAccessToken(oauth2Credential);
        }
        return null;
    }

    /**
     * 邮件收件客户端构建器
     */
    public static final class Builder {

        /**
         * IMAP 服务器配置
         */
        private @Nullable ImapServerConfig imapServerConfig;

        /**
         * 登录凭证
         */
        private @Nullable MailCredential credential;

        private Builder() {
        }

        /**
         * 设置 IMAP 服务器配置
         *
         * @param imapServerConfig IMAP 配置
         * @return this
         */
        public Builder setImapServerConfig(ImapServerConfig imapServerConfig) {
            this.imapServerConfig = imapServerConfig;
            return this;
        }

        /**
         * 设置登录凭证
         *
         * @param credential 登录凭证
         * @return this
         */
        public Builder setCredential(MailCredential credential) {
            this.credential = credential;
            return this;
        }

        /**
         * 构建 MailReader
         *
         * @return MailReader 实例
         */
        public MailReader build() {
            return new MailReader(this);
        }
    }
}
