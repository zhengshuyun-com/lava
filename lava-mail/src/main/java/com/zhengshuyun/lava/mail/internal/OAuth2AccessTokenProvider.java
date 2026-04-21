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
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;

/**
 * OAuth2 access token 提供器
 *
 * @author Toint
 * @since 2026/4/21
 */
public final class OAuth2AccessTokenProvider {

    /**
     * token 获取客户端
     */
    private final OAuth2TokenClient tokenClient;

    /**
     * 缓存 token
     */
    private OAuth2AccessToken cachedToken;

    /**
     * 使用默认 token 客户端创建提供器
     */
    public OAuth2AccessTokenProvider() {
        this(OAuth2TokenClient.createDefault());
    }

    /**
     * 使用指定 token 客户端创建提供器
     *
     * @param tokenClient token 客户端
     */
    public OAuth2AccessTokenProvider(OAuth2TokenClient tokenClient) {
        this.tokenClient = Validate.notNull(tokenClient, "tokenClient must not be null");
    }

    /**
     * 获取可用 access token
     *
     * @param credential OAuth2 凭证
     * @return 可直接用于 IMAP 或 SMTP XOAUTH2 登录的 access token
     */
    public synchronized String getAccessToken(OAuth2RefreshTokenCredential credential) {
        Validate.notNull(credential, "credential must not be null");

        // 只有缓存为空或已过期时才重新换 token, 避免每次请求都打到 OAuth2 endpoint.
        if (cachedToken == null || cachedToken.isExpired()) {
            cachedToken = tokenClient.fetchAccessToken(credential);
        }
        return cachedToken.getAccessToken();
    }
}
