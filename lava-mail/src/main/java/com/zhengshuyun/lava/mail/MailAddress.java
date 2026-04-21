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

/**
 * 邮件地址
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailAddress {

    /**
     * 邮箱地址
     */
    private final String address;

    /**
     * 显示名称
     * <p>
     * 发信时可用于自定义发件人昵称, 例如 `DMIT Inc. <system@notice.dmit.io>`.
     */
    private final @Nullable String personal;

    private MailAddress(Builder builder) {
        this.address = Validate.notBlank(builder.address, "address must not be blank");
        this.personal = builder.personal;
    }

    /**
     * 创建邮件地址构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取邮箱地址
     *
     * @return 邮箱地址
     */
    public String getAddress() {
        return address;
    }

    /**
     * 获取显示名称
     *
     * @return 显示名称, 未设置时返回 null
     */
    public @Nullable String getPersonal() {
        return personal;
    }

    /**
     * 邮件地址构建器
     */
    public static final class Builder {

        /**
         * 邮箱地址
         */
        private String address;

        /**
         * 显示名称
         * <p>
         * 例如发件时可传 `DMIT Inc.` 这类名称, 与 address 一起组成展示头.
         */
        private @Nullable String personal;

        private Builder() {
        }

        /**
         * 设置邮箱地址
         *
         * @param address 邮箱地址
         * @return this
         */
        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        /**
         * 设置显示名称
         *
         * @param personal 显示名称, 允许为 null
         * @return this
         */
        public Builder setPersonal(@Nullable String personal) {
            this.personal = personal;
            return this;
        }

        /**
         * 构建邮件地址
         *
         * @return 邮件地址
         */
        public MailAddress build() {
            return new MailAddress(this);
        }
    }
}
