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

package com.zhengshuyun.lava.mail.provider;

import com.zhengshuyun.lava.core.lang.Validate;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮件厂商 OAuth2 预置配置
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class MailOAuth2Profile {

    /**
     * token endpoint
     */
    private final String tokenEndpoint;

    /**
     * scopes
     */
    private final List<String> scopes;

    private MailOAuth2Profile(Builder builder) {
        this.tokenEndpoint = Validate.notBlank(builder.tokenEndpoint, "tokenEndpoint must not be blank");
        this.scopes = List.copyOf(builder.scopes);
        Validate.isTrue(!scopes.isEmpty(), "scopes must not be empty");
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
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
     * 创建一个已预填 endpoint 和 scopes 的 OAuth2 凭证构建器
     *
     * @return 已带厂商默认值的凭证构建器
     */
    public OAuth2RefreshTokenCredential.Builder createCredentialBuilder() {
        return OAuth2RefreshTokenCredential.builder()
                .setTokenEndpoint(tokenEndpoint)
                .setScopes(scopes);
    }

    /**
     * OAuth2 预置配置构建器
     */
    public static final class Builder {

        /**
         * token endpoint
         */
        private String tokenEndpoint;

        /**
         * scopes
         */
        private List<String> scopes = new ArrayList<>();

        private Builder() {
        }

        /**
         * 设置 token endpoint
         *
         * @param tokenEndpoint token endpoint
         * @return 构建器
         */
        public Builder setTokenEndpoint(String tokenEndpoint) {
            this.tokenEndpoint = tokenEndpoint;
            return this;
        }

        /**
         * 设置 scopes
         *
         * @param scopes scopes
         * @return 构建器
         */
        public Builder setScopes(Iterable<String> scopes) {
            Validate.notNull(scopes, "scopes must not be null");

            // 预置层需要保证每个 scope 都是可直接下发给 OAuth2 endpoint 的有效值.
            List<String> copiedScopes = new ArrayList<>();
            for (String scope : scopes) {
                copiedScopes.add(Validate.notBlank(scope, "scopes contains blank element"));
            }

            this.scopes = copiedScopes;
            return this;
        }

        /**
         * 追加 scope
         *
         * @param scope 单个 scope
         * @return 构建器
         */
        public Builder addScope(String scope) {
            this.scopes.add(Validate.notBlank(scope, "scope must not be blank"));
            return this;
        }

        /**
         * 构建 OAuth2 预置配置
         *
         * @return OAuth2 预置配置
         */
        public MailOAuth2Profile build() {
            return new MailOAuth2Profile(this);
        }
    }
}
