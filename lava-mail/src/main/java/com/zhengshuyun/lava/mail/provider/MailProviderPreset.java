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

package com.zhengshuyun.lava.mail.provider;

import com.zhengshuyun.lava.core.lang.Validate;
import com.zhengshuyun.lava.mail.ImapServerConfig;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import com.zhengshuyun.lava.mail.SmtpServerConfig;
import org.jspecify.annotations.Nullable;

/**
 * 邮件厂商预置配置
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailProviderPreset {

    /**
     * 厂商名称
     */
    private final String name;

    /**
     * IMAP 配置
     */
    private final ImapServerConfig imapServerConfig;

    /**
     * SMTP 配置
     */
    private final SmtpServerConfig smtpServerConfig;

    /**
     * OAuth2 配置, 可选
     */
    private final @Nullable MailOAuth2Profile oauth2Profile;

    private MailProviderPreset(Builder builder) {
        this.name = Validate.notBlank(builder.name, "name must not be blank");
        this.imapServerConfig = Validate.notNull(builder.imapServerConfig, "imapServerConfig must not be null");
        this.smtpServerConfig = Validate.notNull(builder.smtpServerConfig, "smtpServerConfig must not be null");
        this.oauth2Profile = builder.oauth2Profile;
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取厂商名称
     *
     * @return 厂商名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取 IMAP 配置
     *
     * @return IMAP 配置
     */
    public ImapServerConfig getImapServerConfig() {
        return imapServerConfig;
    }

    /**
     * 获取 SMTP 配置
     *
     * @return SMTP 配置
     */
    public SmtpServerConfig getSmtpServerConfig() {
        return smtpServerConfig;
    }

    /**
     * 获取 OAuth2 配置
     *
     * @return OAuth2 配置, 无则返回 null
     */
    public @Nullable MailOAuth2Profile getOAuth2Profile() {
        return oauth2Profile;
    }

    /**
     * 是否存在 OAuth2 配置
     *
     * @return true 表示已配置 OAuth2
     */
    public boolean hasOAuth2Profile() {
        return oauth2Profile != null;
    }

    /**
     * 创建一个已预填厂商 OAuth2 默认值的凭证构建器
     *
     * @return 凭证构建器
     */
    public OAuth2RefreshTokenCredential.Builder createOAuth2CredentialBuilder() {
        if (oauth2Profile == null) {
            throw new IllegalStateException("oauth2Profile is not configured for provider=" + name);
        }
        // 预置层只负责填默认 endpoint 和 scopes, 账号信息仍然由调用方自行提供.
        return oauth2Profile.createCredentialBuilder();
    }

    /**
     * 邮件厂商预置配置构建器
     */
    public static final class Builder {

        /**
         * 厂商名称
         */
        private String name;

        /**
         * IMAP 配置
         */
        private ImapServerConfig imapServerConfig;

        /**
         * SMTP 配置
         */
        private SmtpServerConfig smtpServerConfig;

        /**
         * OAuth2 配置
         */
        private @Nullable MailOAuth2Profile oauth2Profile;

        private Builder() {
        }

        /**
         * 设置厂商名称
         *
         * @param name 厂商名称
         * @return 构建器
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置 IMAP 配置
         *
         * @param imapServerConfig IMAP 配置
         * @return 构建器
         */
        public Builder setImapServerConfig(ImapServerConfig imapServerConfig) {
            this.imapServerConfig = imapServerConfig;
            return this;
        }

        /**
         * 设置 SMTP 配置
         *
         * @param smtpServerConfig SMTP 配置
         * @return 构建器
         */
        public Builder setSmtpServerConfig(SmtpServerConfig smtpServerConfig) {
            this.smtpServerConfig = smtpServerConfig;
            return this;
        }

        /**
         * 设置 OAuth2 配置
         *
         * @param oauth2Profile OAuth2 配置
         * @return 构建器
         */
        public Builder setOAuth2Profile(@Nullable MailOAuth2Profile oauth2Profile) {
            this.oauth2Profile = oauth2Profile;
            return this;
        }

        /**
         * 构建邮件厂商预置配置
         *
         * @return 邮件厂商预置配置
         */
        public MailProviderPreset build() {
            return new MailProviderPreset(this);
        }
    }
}
