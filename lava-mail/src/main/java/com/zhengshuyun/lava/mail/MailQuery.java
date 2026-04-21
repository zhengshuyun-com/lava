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
 * 收信查询条件
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailQuery {

    /**
     * 文件夹名称
     */
    private final @Nullable String folder;

    /**
     * 限制条数
     */
    private final int limit;

    /**
     * 是否只查询未读
     */
    private final boolean unreadOnly;

    /**
     * 起始接收时间
     */
    private final @Nullable Instant receivedAfter;

    /**
     * 截止接收时间
     */
    private final @Nullable Instant receivedBefore;

    /**
     * 发件人筛选
     */
    private final @Nullable String from;

    /**
     * 主题包含筛选
     */
    private final @Nullable String subjectContains;

    /**
     * 是否包含正文
     */
    private final boolean includeBody;

    /**
     * 是否包含附件
     */
    private final boolean includeAttachments;

    private MailQuery(Builder builder) {
        this.folder = builder.folder;
        if (folder != null) {
            Validate.notBlank(folder, "folder must not be blank");
        }
        this.limit = builder.limit;
        Validate.isTrue(limit > 0, "limit must be positive");
        this.unreadOnly = builder.unreadOnly;
        this.receivedAfter = builder.receivedAfter;
        this.receivedBefore = builder.receivedBefore;
        this.from = builder.from;
        this.subjectContains = builder.subjectContains;
        this.includeBody = builder.includeBody;
        this.includeAttachments = builder.includeAttachments;
    }

    /**
     * 创建收信查询构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取文件夹名称
     *
     * @return 文件夹名称, 未设置时返回 null
     */
    public @Nullable String getFolder() {
        return folder;
    }

    /**
     * 获取返回条数上限
     *
     * @return 返回条数上限
     */
    public int getLimit() {
        return limit;
    }

    /**
     * 是否只查询未读邮件
     *
     * @return true 表示只查未读
     */
    public boolean isUnreadOnly() {
        return unreadOnly;
    }

    /**
     * 获取起始接收时间
     *
     * @return 起始接收时间, 未设置时返回 null
     */
    public @Nullable Instant getReceivedAfter() {
        return receivedAfter;
    }

    /**
     * 获取截止接收时间
     *
     * @return 截止接收时间, 未设置时返回 null
     */
    public @Nullable Instant getReceivedBefore() {
        return receivedBefore;
    }

    /**
     * 获取发件人筛选条件
     *
     * @return 发件人筛选条件, 未设置时返回 null
     */
    public @Nullable String getFrom() {
        return from;
    }

    /**
     * 获取主题包含筛选条件
     *
     * @return 主题筛选条件, 未设置时返回 null
     */
    public @Nullable String getSubjectContains() {
        return subjectContains;
    }

    /**
     * 是否包含正文
     *
     * @return true 表示解析正文
     */
    public boolean isIncludeBody() {
        return includeBody;
    }

    /**
     * 是否包含附件
     *
     * @return true 表示解析附件
     */
    public boolean isIncludeAttachments() {
        return includeAttachments;
    }

    /**
     * 收信查询构建器
     */
    public static final class Builder {

        /**
         * 文件夹名称
         */
        private @Nullable String folder;

        /**
         * 限制条数
         */
        private int limit = 50;

        /**
         * 是否只查询未读
         */
        private boolean unreadOnly;

        /**
         * 起始接收时间
         */
        private @Nullable Instant receivedAfter;

        /**
         * 截止接收时间
         */
        private @Nullable Instant receivedBefore;

        /**
         * 发件人筛选
         */
        private @Nullable String from;

        /**
         * 主题包含筛选
         */
        private @Nullable String subjectContains;

        /**
         * 是否包含正文
         */
        private boolean includeBody = true;

        /**
         * 是否包含附件
         */
        private boolean includeAttachments;

        private Builder() {
        }

        /**
         * 设置文件夹名称
         *
         * @param folder 文件夹名称, 允许为 null
         * @return this
         */
        public Builder setFolder(@Nullable String folder) {
            this.folder = folder;
            return this;
        }

        /**
         * 设置文件夹枚举
         *
         * @param folder 文件夹枚举, 允许为 null
         * @return this
         */
        public Builder setFolder(@Nullable MailFolder folder) {
            this.folder = folder == null ? null : folder.getValue();
            return this;
        }

        /**
         * 设置返回条数上限
         *
         * @param limit 返回条数上限
         * @return this
         */
        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * 设置是否只查未读
         *
         * @param unreadOnly true 表示只查未读
         * @return this
         */
        public Builder setUnreadOnly(boolean unreadOnly) {
            this.unreadOnly = unreadOnly;
            return this;
        }

        /**
         * 设置起始接收时间
         *
         * @param receivedAfter 起始接收时间
         * @return this
         */
        public Builder setReceivedAfter(@Nullable Instant receivedAfter) {
            this.receivedAfter = receivedAfter;
            return this;
        }

        /**
         * 设置截止接收时间
         *
         * @param receivedBefore 截止接收时间
         * @return this
         */
        public Builder setReceivedBefore(@Nullable Instant receivedBefore) {
            this.receivedBefore = receivedBefore;
            return this;
        }

        /**
         * 设置发件人筛选条件
         *
         * @param from 发件人筛选条件
         * @return this
         */
        public Builder setFrom(@Nullable String from) {
            this.from = from;
            return this;
        }

        /**
         * 设置主题包含筛选条件
         *
         * @param subjectContains 主题筛选条件
         * @return this
         */
        public Builder setSubjectContains(@Nullable String subjectContains) {
            this.subjectContains = subjectContains;
            return this;
        }

        /**
         * 设置是否包含正文
         *
         * @param includeBody true 表示解析正文
         * @return this
         */
        public Builder setIncludeBody(boolean includeBody) {
            this.includeBody = includeBody;
            return this;
        }

        /**
         * 设置是否包含附件
         *
         * @param includeAttachments true 表示解析附件, 默认 false
         * @return this
         */
        public Builder setIncludeAttachments(boolean includeAttachments) {
            this.includeAttachments = includeAttachments;
            return this;
        }

        /**
         * 构建收信查询对象
         *
         * @return 收信查询对象
         */
        public MailQuery build() {
            return new MailQuery(this);
        }
    }
}
