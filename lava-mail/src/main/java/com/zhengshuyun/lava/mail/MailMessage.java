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
import java.util.List;

/**
 * 邮件消息
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailMessage {

    /**
     * 邮件消息 ID
     */
    private final @Nullable String messageId;

    /**
     * 发件人列表
     */
    private final List<MailAddress> fromList;

    /**
     * 收件人列表
     */
    private final List<MailAddress> toList;

    /**
     * 抄送列表
     */
    private final List<MailAddress> ccList;

    /**
     * 主题
     */
    private final String subject;

    /**
     * 纯文本正文
     */
    private final @Nullable String textBody;

    /**
     * HTML 正文
     */
    private final @Nullable String htmlBody;

    /**
     * 发送时间
     */
    private final @Nullable Instant sentAt;

    /**
     * 接收时间
     */
    private final @Nullable Instant receivedAt;

    /**
     * 附件列表
     */
    private final List<MailAttachment> attachmentList;

    private MailMessage(Builder builder) {
        this.messageId = builder.messageId;
        this.fromList = List.copyOf(builder.fromList);
        this.toList = List.copyOf(builder.toList);
        this.ccList = List.copyOf(builder.ccList);
        this.subject = Validate.notNull(builder.subject, "subject must not be null");
        this.textBody = builder.textBody;
        this.htmlBody = builder.htmlBody;
        this.sentAt = builder.sentAt;
        this.receivedAt = builder.receivedAt;
        this.attachmentList = List.copyOf(builder.attachmentList);
    }

    /**
     * 创建邮件消息构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取邮件消息 ID
     *
     * @return 邮件消息 ID, 为空时返回 null
     */
    public @Nullable String getMessageId() {
        return messageId;
    }

    /**
     * 获取发件人列表
     *
     * @return 发件人列表
     */
    public List<MailAddress> getFromList() {
        return fromList;
    }

    /**
     * 获取收件人列表
     *
     * @return 收件人列表
     */
    public List<MailAddress> getToList() {
        return toList;
    }

    /**
     * 获取抄送列表
     *
     * @return 抄送列表
     */
    public List<MailAddress> getCcList() {
        return ccList;
    }

    /**
     * 获取邮件主题
     *
     * @return 邮件主题
     */
    public String getSubject() {
        return subject;
    }

    /**
     * 获取纯文本正文
     *
     * @return 纯文本正文, 未解析或不存在时返回 null
     */
    public @Nullable String getTextBody() {
        return textBody;
    }

    /**
     * 获取 HTML 正文
     *
     * @return HTML 正文, 未解析或不存在时返回 null
     */
    public @Nullable String getHtmlBody() {
        return htmlBody;
    }

    /**
     * 获取发送时间
     *
     * @return 发送时间, 不存在时返回 null
     */
    public @Nullable Instant getSentAt() {
        return sentAt;
    }

    /**
     * 获取接收时间
     *
     * @return 接收时间, 不存在时返回 null
     */
    public @Nullable Instant getReceivedAt() {
        return receivedAt;
    }

    /**
     * 获取附件列表
     *
     * @return 附件列表
     */
    public List<MailAttachment> getAttachmentList() {
        return attachmentList;
    }

    /**
     * 邮件消息构建器
     */
    public static final class Builder {

        /**
         * 邮件消息 ID
         */
        private @Nullable String messageId;

        /**
         * 发件人列表
         */
        private List<MailAddress> fromList = List.of();

        /**
         * 收件人列表
         */
        private List<MailAddress> toList = List.of();

        /**
         * 抄送列表
         */
        private List<MailAddress> ccList = List.of();

        /**
         * 主题
         */
        private String subject = "";

        /**
         * 纯文本正文
         */
        private @Nullable String textBody;

        /**
         * HTML 正文
         */
        private @Nullable String htmlBody;

        /**
         * 发送时间
         */
        private @Nullable Instant sentAt;

        /**
         * 接收时间
         */
        private @Nullable Instant receivedAt;

        /**
         * 附件列表
         */
        private List<MailAttachment> attachmentList = List.of();

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
         * 设置发件人列表
         *
         * @param fromList 发件人列表
         * @return this
         */
        public Builder setFromList(Iterable<MailAddress> fromList) {
            this.fromList = copyToList(fromList, "fromList");
            return this;
        }

        /**
         * 设置收件人列表
         *
         * @param toList 收件人列表
         * @return this
         */
        public Builder setToList(Iterable<MailAddress> toList) {
            this.toList = copyToList(toList, "toList");
            return this;
        }

        /**
         * 设置抄送列表
         *
         * @param ccList 抄送列表
         * @return this
         */
        public Builder setCcList(Iterable<MailAddress> ccList) {
            this.ccList = copyToList(ccList, "ccList");
            return this;
        }

        /**
         * 设置邮件主题
         *
         * @param subject 邮件主题
         * @return this
         */
        public Builder setSubject(String subject) {
            this.subject = Validate.notNull(subject, "subject must not be null");
            return this;
        }

        /**
         * 设置纯文本正文
         *
         * @param textBody 纯文本正文
         * @return this
         */
        public Builder setTextBody(@Nullable String textBody) {
            this.textBody = textBody;
            return this;
        }

        /**
         * 设置 HTML 正文
         *
         * @param htmlBody HTML 正文
         * @return this
         */
        public Builder setHtmlBody(@Nullable String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }

        /**
         * 设置发送时间
         *
         * @param sentAt 发送时间
         * @return this
         */
        public Builder setSentAt(@Nullable Instant sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        /**
         * 设置接收时间
         *
         * @param receivedAt 接收时间
         * @return this
         */
        public Builder setReceivedAt(@Nullable Instant receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        /**
         * 设置附件列表
         *
         * @param attachmentList 附件列表
         * @return this
         */
        public Builder setAttachmentList(Iterable<MailAttachment> attachmentList) {
            this.attachmentList = copyToList(attachmentList, "attachmentList");
            return this;
        }

        /**
         * 构建邮件消息
         *
         * @return 邮件消息
         */
        public MailMessage build() {
            return new MailMessage(this);
        }

        /**
         * 复制外部列表并校验空元素
         *
         * @param source        外部集合
         * @param parameterName 参数名
         * @param <T>           元素类型
         * @return 不可变列表
         */
        private static <T> List<T> copyToList(Iterable<T> source, String parameterName) {
            Validate.notNull(source, parameterName + " must not be null");

            java.util.ArrayList<T> result = new java.util.ArrayList<>();
            for (T element : source) {
                result.add(Validate.notNull(element, parameterName + " contains null element"));
            }
            return List.copyOf(result);
        }
    }
}
