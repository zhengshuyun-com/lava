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
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 发信结果
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailSendResult {

    /**
     * 邮件消息 ID
     */
    private final @Nullable String messageId;

    /**
     * 发送完成时间
     */
    private final Instant sentAt;

    /**
     * 服务端摘要
     */
    private final @Nullable String responseSummary;

    private MailSendResult(Builder builder) {
        this.messageId = builder.messageId;
        this.sentAt = Validate.notNull(builder.sentAt, "sentAt must not be null");
        this.responseSummary = builder.responseSummary;
    }

    /**
     * 创建发信结果构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取消息 ID
     *
     * @return 消息 ID, 服务端未返回时为 null
     */
    public @Nullable String getMessageId() {
        return messageId;
    }

    /**
     * 获取发送完成时间
     *
     * @return 发送完成时间
     */
    public Instant getSentAt() {
        return sentAt;
    }

    /**
     * 获取服务端返回摘要
     *
     * @return 服务端摘要, 无摘要时为 null
     */
    public @Nullable String getResponseSummary() {
        return responseSummary;
    }

    /**
     * 发信结果构建器
     */
    public static final class Builder {

        /**
         * 邮件消息 ID
         */
        private @Nullable String messageId;

        /**
         * 发送完成时间
         */
        private Instant sentAt;

        /**
         * 服务端摘要
         */
        private @Nullable String responseSummary;

        private Builder() {
        }

        /**
         * 设置消息 ID
         *
         * @param messageId 消息 ID
         * @return this
         */
        public Builder setMessageId(@Nullable String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * 设置发送完成时间
         *
         * @param sentAt 发送完成时间
         * @return this
         */
        public Builder setSentAt(Instant sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        /**
         * 设置服务端摘要
         *
         * @param responseSummary 服务端摘要
         * @return this
         */
        public Builder setResponseSummary(@Nullable String responseSummary) {
            this.responseSummary = responseSummary;
            return this;
        }

        /**
         * 构建发信结果
         *
         * @return 发信结果
         */
        public MailSendResult build() {
            return new MailSendResult(this);
        }
    }
}
