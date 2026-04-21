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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;
import com.zhengshuyun.lava.json.JsonUtil;
import com.zhengshuyun.lava.mail.MailException;
import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.time.Instant;
import java.util.Map;

/**
 * OAuth2 token 获取客户端
 *
 * @author Toint
 * @since 2026/4/21
 */
@FunctionalInterface
public interface OAuth2TokenClient {

    /**
     * 根据 refresh token 获取 access token
     *
     * @param credential OAuth2 凭证
     * @return access token
     */
    OAuth2AccessToken fetchAccessToken(OAuth2RefreshTokenCredential credential);

    /**
     * 创建默认 token 客户端
     *
     * @return 默认 token 客户端
     */
    static OAuth2TokenClient createDefault() {
        return credential -> {
            // 先把标准 refresh_token 请求体准备好, 便于后续直接发到 token endpoint.
            Map<String, String> form = createRequestForm(credential);

            try (HttpResponse response = HttpRequest.post(credential.getTokenEndpoint())
                    .setFormBody(form)
                    .build()
                    .execute()) {
                // token endpoint 返回的是 JSON, 这里只关心 access_token 和 expires_in 两个字段.
                String responseBody = response.getBodyAsString();
                if (!response.isSuccessful()) {
                    throw new MailException("Failed to fetch OAuth2 access token, code=" + response.getCode());
                }

                // 用记录类承接 token 响应, 避免后续代码手工从 JsonNode 里逐个取字段.
                OAuth2TokenResponse tokenResponse = JsonUtil.readValue(responseBody, OAuth2TokenResponse.class);
                String accessToken = tokenResponse.getRequiredAccessToken();
                long expiresIn = tokenResponse.getExpiresInOrDefault();
                return OAuth2AccessToken.builder()
                        .setAccessToken(accessToken)
                        .setExpiresAt(Instant.now().plusSeconds(expiresIn))
                        .build();
            } catch (MailException e) {
                throw e;
            } catch (Exception e) {
                throw new MailException("Failed to fetch OAuth2 access token", e);
            }
        };
    }

    /**
     * 组装 refresh token 请求表单
     *
     * @param credential OAuth2 凭证
     * @return 表单参数
     */
    private static Map<String, String> createRequestForm(OAuth2RefreshTokenCredential credential) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", credential.getClientId());
        form.put("refresh_token", credential.getRefreshToken());
        form.put("scope", String.join(" ", credential.getScopes()));

        if (credential.getClientSecret() != null) {
            form.put("client_secret", credential.getClientSecret());
        }
        return Map.copyOf(form);
    }

    /**
     * OAuth2 token 响应
     * <p>
     * 微软等 token endpoint 通常会返回很多附加字段, 这里仅声明当前链路真正关心的字段.
     */
    record OAuth2TokenResponse(
            @JsonAlias("access_token") @Nullable String accessToken,
            @JsonAlias("expires_in") @Nullable Long expiresIn
    ) {

        /**
         * 读取必填 access token
         *
         * @return access token
         * @throws MailException access token 缺失或为空时抛出
         */
        private String getRequiredAccessToken() {
            if (accessToken == null || accessToken.isBlank()) {
                throw new MailException("OAuth2 token response missing field=access_token");
            }
            return accessToken;
        }

        /**
         * 读取 token 有效期
         *
         * @return 有效期秒数, 响应缺省时默认 3600 秒
         * @throws MailException expiresIn 非正数时抛出
         */
        private long getExpiresInOrDefault() {
            if (expiresIn == null) {
                return 3600L;
            }
            if (expiresIn <= 0) {
                throw new MailException("OAuth2 token response has invalid expires_in=" + expiresIn);
            }
            return expiresIn;
        }
    }
}
