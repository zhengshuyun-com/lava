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
import com.zhengshuyun.lava.mail.internal.MailSessionFactory;
import com.zhengshuyun.lava.mail.internal.MimeMessageFactory;
import com.zhengshuyun.lava.mail.internal.OAuth2AccessTokenProvider;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 同步邮件发件客户端
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailSender {

    /**
     * SMTP 服务器配置
     */
    private final SmtpServerConfig smtpServerConfig;

    /**
     * 登录凭证
     */
    private final MailCredential credential;

    /**
     * OAuth2 token 提供器
     */
    private final OAuth2AccessTokenProvider accessTokenProvider;

    private MailSender(Builder builder) {
        this.smtpServerConfig = Validate.notNull(builder.smtpServerConfig, "smtpServerConfig must not be null");
        this.credential = Validate.notNull(builder.credential, "credential must not be null");
        this.accessTokenProvider = new OAuth2AccessTokenProvider();
    }

    /**
     * 创建 MailSender 构建器
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 发送邮件
     * <p>
     * 当前实现会根据凭证类型自动选择密码登录或 OAuth2 access token 登录.
     * 整个发信链路分成 4 步:
     * <p>
     * - 先按凭证类型解析出密码或 access token
     * - 再构造 SMTP Session 和 MimeMessage
     * - 然后连接 SMTP 服务端并发送消息
     * - 最后把发送结果收口成 Lava 自己的返回模型
     *
     * @param request 发信请求
     * @return 发信结果
     * @throws IllegalArgumentException request 为 null 时抛出
     * @throws MailException            底层邮件协议调用失败时抛出
     */
    public MailSendResult send(MailSendRequest request) {
        Validate.notNull(request, "request must not be null");

        // OAuth2 凭证需要先换取 access token, 密码型凭证则直接返回 null.
        String accessToken = resolveAccessTokenIfNecessary();

        // Session 里已经带好了 SSL, STARTTLS, XOAUTH2 等协议层配置.
        Session session = MailSessionFactory.createSmtpSession(smtpServerConfig, credential, accessToken);

        // MimeMessageFactory 负责把 Lava 自己的发信请求转成标准 MIME 消息.
        MimeMessage message = MimeMessageFactory.create(session, request);

        Transport transport = null;
        try {
            // Transport 连接阶段才会真正和 SMTP 服务端握手并完成鉴权.
            transport = session.getTransport("smtp");
            // 这里统一通过 MailSessionFactory.resolvePassword 屏蔽密码登录和 XOAUTH2 登录差异.
            transport.connect(
                    smtpServerConfig.getHost(),
                    smtpServerConfig.getPort(),
                    credential.getUsername(),
                    MailSessionFactory.resolvePassword(credential, accessToken)
            );

            // 真正发送时使用消息内已经组装好的所有收件人地址.
            transport.sendMessage(message, message.getAllRecipients());

            // 返回结果只保留调用方最常关心的几个字段, 不泄漏 Jakarta Mail 原生对象.
            return MailSendResult.builder()
                    .setMessageId(message.getMessageID())
                    .setSentAt(Instant.now())
                    .setResponseSummary("sent via host=" + smtpServerConfig.getHost())
                    .build();
        } catch (Exception e) {
            throw new MailException("Failed to send mail message", e);
        } finally {
            // Transport 是短连接资源, 每次发信结束后都立即关闭.
            closeTransport(transport);
        }
    }

    /**
     * 获取 SMTP 服务器配置
     *
     * @return SMTP 配置
     */
    public SmtpServerConfig getSmtpServerConfig() {
        return smtpServerConfig;
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
     * <p>
     * OAuth2 场景下这里会通过 refresh token 换取 access token.
     * 密码型凭证不需要额外换 token, 因此直接返回 null.
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
     * 关闭 Transport
     *
     * @param transport SMTP Transport
     */
    private static void closeTransport(@Nullable Transport transport) {
        if (transport != null && transport.isConnected()) {
            try {
                transport.close();
            } catch (Exception ignored) {
                // 关闭阶段没有补救动作, 这里忽略异常避免覆盖主异常.
            }
        }
    }

    /**
     * 邮件发件客户端构建器
     */
    public static final class Builder {

        /**
         * SMTP 服务器配置
         */
        private @Nullable SmtpServerConfig smtpServerConfig;

        /**
         * 登录凭证
         */
        private @Nullable MailCredential credential;

        private Builder() {
        }

        /**
         * 设置 SMTP 服务器配置
         *
         * @param smtpServerConfig SMTP 配置
         * @return this
         */
        public Builder setSmtpServerConfig(SmtpServerConfig smtpServerConfig) {
            this.smtpServerConfig = smtpServerConfig;
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
         * 构建 MailSender
         *
         * @return MailSender 实例
         */
        public MailSender build() {
            return new MailSender(this);
        }
    }
}
