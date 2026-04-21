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
 * 基于 refresh token 的 OAuth2 凭证
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class OAuth2RefreshTokenCredential implements MailCredential {

    /**
     * 登录用户名
     */
    private final String username;

    /**
     * OAuth clientId
     */
    private final String clientId;

    /**
     * OAuth refreshToken
     */
    private final String refreshToken;

    /**
     * OAuth token endpoint
     */
    private final String tokenEndpoint;

    /**
     * OAuth scopes
     */
    private final List<String> scopes;

    /**
     * OAuth clientSecret, 可选
     */
    private final @Nullable String clientSecret;

    private OAuth2RefreshTokenCredential(Builder builder) {
        this.username = Validate.notBlank(builder.username, "username must not be blank");
        this.clientId = Validate.notBlank(builder.clientId, "clientId must not be blank");
        this.refreshToken = Validate.notBlank(builder.refreshToken, "refreshToken must not be blank");
        this.tokenEndpoint = Validate.notBlank(builder.tokenEndpoint, "tokenEndpoint must not be blank");
        this.scopes = List.copyOf(builder.scopes);
        Validate.isTrue(!scopes.isEmpty(), "scopes must not be empty");
        this.clientSecret = builder.clientSecret == null
                ? null
                : Validate.notBlank(builder.clientSecret, "clientSecret must not be blank");
    }

    /**
     * 创建 OAuth2 refresh token 凭证构建器
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
     * 获取 OAuth clientId
     *
     * @return clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 获取 refresh token
     *
     * @return refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 获取 token endpoint
     *
     * @return token endpoint
     */
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * 获取 scopes
     *
     * @return scopes
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * 获取 clientSecret
     *
     * @return clientSecret, 未设置时返回 null
     */
    public @Nullable String getClientSecret() {
        return clientSecret;
    }

    /**
     * OAuth2 凭证构建器
     */
    public static final class Builder {

        /**
         * 登录用户名
         */
        private String username;

        /**
         * OAuth clientId
         */
        private String clientId;

        /**
         * OAuth refreshToken
         */
        private String refreshToken;

        /**
         * OAuth token endpoint
         */
        private String tokenEndpoint;

        /**
         * OAuth scopes
         */
        private List<String> scopes = new ArrayList<>();

        /**
         * OAuth clientSecret
         */
        private @Nullable String clientSecret;

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
         * 设置 clientId
         *
         * @param clientId OAuth clientId
         * @return this
         */
        public Builder setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * 设置 refresh token
         *
         * @param refreshToken refresh token
         * @return this
         */
        public Builder setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        /**
         * 设置 token endpoint
         *
         * @param tokenEndpoint token endpoint
         * @return this
         */
        public Builder setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
            return this;
        }

        /**
         * 整体设置 scopes
         *
         * @param scopes scopes
         * @return this
         */
        public Builder setScopes(Iterable<String> scopes) {
            this.scopes = copyToMutableList(scopes, "scopes");
            return this;
        }

        /**
         * 追加一个 scope
         *
         * @param scope 单个 scope
         * @return this
         */
        public Builder addScope(String scope) {
            this.scopes.add(Validate.notBlank(scope, "scope must not be blank"));
            return this;
        }

        /**
         * 设置 clientSecret
         *
         * @param clientSecret clientSecret, 允许为 null
         * @return this
         */
        public Builder setClientSecret(@Nullable String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /**
         * 构建 OAuth2 refresh token 凭证
         *
         * @return OAuth2 refresh token 凭证
         */
        public OAuth2RefreshTokenCredential build() {
            return new OAuth2RefreshTokenCredential(this);
        }

        /**
         * 复制 scopes 并校验空白元素
         *
         * @param source        外部 scopes
         * @param parameterName 参数名
         * @return 可继续追加的可变列表
         */
        private static List<String> copyToMutableList(Iterable<String> source, String parameterName) {
            Validate.notNull(source, parameterName + " must not be null");

            List<String> result = new ArrayList<>();
            for (String element : source) {
                result.add(Validate.notBlank(element, parameterName + " contains blank element"));
            }
            return result;
        }
    }
}
