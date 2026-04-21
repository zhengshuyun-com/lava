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

package com.zhengshuyun.lava.mail.internal;

import com.zhengshuyun.lava.core.lang.Validate;

import java.time.Instant;

/**
 * OAuth2 access token
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class OAuth2AccessToken {

    /**
     * 过期前预留的安全时间, 秒
     */
    private static final long EXPIRATION_SKEW_SECONDS = 60L;

    /**
     * access token
     */
    private final String accessToken;

    /**
     * 过期时间
     */
    private final Instant expiresAt;

    private OAuth2AccessToken(Builder builder) {
        this.accessToken = Validate.notBlank(builder.accessToken, "accessToken must not be blank");
        this.expiresAt = Validate.notNull(builder.expiresAt, "expiresAt must not be null");
    }

    /**
     * 创建 access token 构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取 access token
     *
     * @return access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * 获取过期时间
     *
     * @return 过期时间
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * 判断 token 是否已过期
     * <p>
     * 这里会提前一小段时间视为过期, 避免拿着临期 token 去做 SMTP 或 IMAP 登录.
     *
     * @return 已过期或即将过期返回 true
     */
    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now().plusSeconds(EXPIRATION_SKEW_SECONDS));
    }

    /**
     * OAuth2 access token 构建器
     */
    public static final class Builder {

        /**
         * access token
         */
        private String accessToken;

        /**
         * 过期时间
         */
        private Instant expiresAt;

        private Builder() {
        }

        /**
         * 设置 access token
         *
         * @param accessToken access token
         * @return this
         */
        public Builder setAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * 设置过期时间
         *
         * @param expiresAt 过期时间
         * @return this
         */
        public Builder setExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * 构建 access token 对象
         *
         * @return access token 对象
         */
        public OAuth2AccessToken build() {
            return new OAuth2AccessToken(this);
        }
    }
}
