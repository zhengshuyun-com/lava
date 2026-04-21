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

import java.util.Arrays;

/**
 * 邮件附件
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailAttachment {

    /**
     * 文件名
     */
    private final String fileName;

    /**
     * 内容类型
     */
    private final String contentType;

    /**
     * 附件内容
     */
    private final byte[] content;

    private MailAttachment(Builder builder) {
        this.fileName = Validate.notBlank(builder.fileName, "fileName must not be blank");
        this.contentType = Validate.notBlank(builder.contentType, "contentType must not be blank");
        this.content = Arrays.copyOf(Validate.notNull(builder.content, "content must not be null"), builder.content.length);
    }

    /**
     * 创建邮件附件构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取附件文件名
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取附件内容类型
     *
     * @return 内容类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 获取附件内容副本
     *
     * @return 附件内容副本
     */
    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    /**
     * 获取附件大小
     *
     * @return 字节大小
     */
    public long getSize() {
        return content.length;
    }

    /**
     * 邮件附件构建器
     */
    public static final class Builder {

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 内容类型
         */
        private String contentType = "application/octet-stream";

        /**
         * 附件内容
         */
        private byte[] content = new byte[0];

        private Builder() {
        }

        /**
         * 设置附件文件名
         *
         * @param fileName 文件名
         * @return this
         */
        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * 设置内容类型
         *
         * @param contentType 内容类型
         * @return this
         */
        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * 设置附件内容
         *
         * @param content 附件字节内容
         * @return this
         */
        public Builder setContent(byte[] content) {
            this.content = Arrays.copyOf(Validate.notNull(content, "content must not be null"), content.length);
            return this;
        }

        /**
         * 构建附件对象
         *
         * @return 附件对象
         */
        public MailAttachment build() {
            return new MailAttachment(this);
        }
    }
}
