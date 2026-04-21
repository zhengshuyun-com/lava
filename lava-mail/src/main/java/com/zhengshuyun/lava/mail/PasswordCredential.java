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
 * 密码型凭证
 * <p>
 * 可用于普通密码或邮箱授权码.
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class PasswordCredential implements MailCredential {

    /**
     * 登录用户名
     */
    private final String username;

    /**
     * 密码或授权码
     */
    private final String password;

    private PasswordCredential(Builder builder) {
        this.username = Validate.notBlank(builder.username, "username must not be blank");
        this.password = Validate.notBlank(builder.password, "password must not be blank");
    }

    /**
     * 创建密码型凭证构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取登录用户名
     *
     * @return 登录用户名
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 获取密码或授权码
     *
     * @return 密码或授权码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 密码型凭证构建器
     */
    public static final class Builder {

        /**
         * 登录用户名
         */
        private String username;

        /**
         * 密码或授权码
         */
        private String password;

        private Builder() {
        }

        /**
         * 设置登录用户名
         *
         * @param username 登录用户名
         * @return this
         */
        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        /**
         * 设置密码或授权码
         *
         * @param password 密码或授权码
         * @return this
         */
        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * 构建密码型凭证
         *
         * @return 密码型凭证
         */
        public PasswordCredential build() {
            return new PasswordCredential(this);
        }
    }
}
