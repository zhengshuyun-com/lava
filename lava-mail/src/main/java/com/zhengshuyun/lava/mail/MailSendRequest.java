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

import java.util.ArrayList;
import java.util.List;

/**
 * 发信请求
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailSendRequest {

    /**
     * 发件人
     */
    private final MailAddress from;

    /**
     * 收件人列表
     */
    private final List<MailAddress> toList;

    /**
     * 抄送列表
     */
    private final List<MailAddress> ccList;

    /**
     * 密送列表
     */
    private final List<MailAddress> bccList;

    /**
     * 回复地址列表
     */
    private final List<MailAddress> replyToList;

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
     * 附件列表
     */
    private final List<MailAttachment> attachmentList;

    private MailSendRequest(Builder builder) {
        this.from = Validate.notNull(builder.from, "from must not be null");
        this.toList = List.copyOf(builder.toList);
        Validate.isTrue(!toList.isEmpty(), "toList must not be empty");
        this.ccList = List.copyOf(builder.ccList);
        this.bccList = List.copyOf(builder.bccList);
        this.replyToList = List.copyOf(builder.replyToList);
        this.subject = builder.subject;
        this.textBody = builder.textBody;
        this.htmlBody = builder.htmlBody;
        this.attachmentList = List.copyOf(builder.attachmentList);
    }

    /**
     * 创建发信请求构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取发件人
     *
     * @return 发件人
     */
    public MailAddress getFrom() {
        return from;
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
     * 获取密送列表
     *
     * @return 密送列表
     */
    public List<MailAddress> getBccList() {
        return bccList;
    }

    /**
     * 获取回复地址列表
     *
     * @return 回复地址列表
     */
    public List<MailAddress> getReplyToList() {
        return replyToList;
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
     * @return 纯文本正文, 未设置时返回 null
     */
    public @Nullable String getTextBody() {
        return textBody;
    }

    /**
     * 获取 HTML 正文
     *
     * @return HTML 正文, 未设置时返回 null
     */
    public @Nullable String getHtmlBody() {
        return htmlBody;
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
     * 发信请求构建器
     */
    public static final class Builder {

        /**
         * 发件人
         */
        private MailAddress from;

        /**
         * 收件人列表
         */
        private List<MailAddress> toList = new ArrayList<>();

        /**
         * 抄送列表
         */
        private List<MailAddress> ccList = new ArrayList<>();

        /**
         * 密送列表
         */
        private List<MailAddress> bccList = new ArrayList<>();

        /**
         * 回复地址列表
         */
        private List<MailAddress> replyToList = new ArrayList<>();

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
         * 附件列表
         */
        private List<MailAttachment> attachmentList = new ArrayList<>();

        private Builder() {
        }

        /**
         * 设置发件人
         *
         * @param from 发件人
         * @return this
         */
        public Builder setFrom(MailAddress from) {
            this.from = Validate.notNull(from, "from must not be null");
            return this;
        }

        /**
         * 整体设置收件人列表
         *
         * @param toList 收件人列表
         * @return this
         */
        public Builder setToList(Iterable<MailAddress> toList) {
            this.toList = copyToMutableList(toList, "toList");
            return this;
        }

        /**
         * 追加一个收件人
         *
         * @param to 收件人
         * @return this
         */
        public Builder addTo(MailAddress to) {
            this.toList.add(Validate.notNull(to, "to must not be null"));
            return this;
        }

        /**
         * 整体设置抄送列表
         *
         * @param ccList 抄送列表
         * @return this
         */
        public Builder setCcList(Iterable<MailAddress> ccList) {
            this.ccList = copyToMutableList(ccList, "ccList");
            return this;
        }

        /**
         * 追加一个抄送地址
         *
         * @param cc 抄送地址
         * @return this
         */
        public Builder addCc(MailAddress cc) {
            this.ccList.add(Validate.notNull(cc, "cc must not be null"));
            return this;
        }

        /**
         * 整体设置密送列表
         *
         * @param bccList 密送列表
         * @return this
         */
        public Builder setBccList(Iterable<MailAddress> bccList) {
            this.bccList = copyToMutableList(bccList, "bccList");
            return this;
        }

        /**
         * 追加一个密送地址
         *
         * @param bcc 密送地址
         * @return this
         */
        public Builder addBcc(MailAddress bcc) {
            this.bccList.add(Validate.notNull(bcc, "bcc must not be null"));
            return this;
        }

        /**
         * 整体设置回复地址列表
         *
         * @param replyToList 回复地址列表
         * @return this
         */
        public Builder setReplyToList(Iterable<MailAddress> replyToList) {
            this.replyToList = copyToMutableList(replyToList, "replyToList");
            return this;
        }

        /**
         * 追加一个回复地址
         *
         * @param replyTo 回复地址
         * @return this
         */
        public Builder addReplyTo(MailAddress replyTo) {
            this.replyToList.add(Validate.notNull(replyTo, "replyTo must not be null"));
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
         * 整体设置附件列表
         *
         * @param attachmentList 附件列表
         * @return this
         */
        public Builder setAttachmentList(Iterable<MailAttachment> attachmentList) {
            this.attachmentList = copyToMutableList(attachmentList, "attachmentList");
            return this;
        }

        /**
         * 追加一个附件
         *
         * @param attachment 附件
         * @return this
         */
        public Builder addAttachment(MailAttachment attachment) {
            this.attachmentList.add(Validate.notNull(attachment, "attachment must not be null"));
            return this;
        }

        /**
         * 构建发信请求
         *
         * @return 发信请求
         */
        public MailSendRequest build() {
            return new MailSendRequest(this);
        }

        /**
         * 复制外部列表并做空元素校验
         *
         * @param source        外部集合
         * @param parameterName 参数名
         * @param <T>           元素类型
         * @return 可继续追加的可变列表
         */
        private static <T> List<T> copyToMutableList(Iterable<T> source, String parameterName) {
            Validate.notNull(source, parameterName + " must not be null");

            List<T> result = new ArrayList<>();
            for (T element : source) {
                result.add(Validate.notNull(element, parameterName + " contains null element"));
            }
            return result;
        }
    }
}
