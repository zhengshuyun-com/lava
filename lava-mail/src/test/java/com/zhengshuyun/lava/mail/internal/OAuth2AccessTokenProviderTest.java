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

import com.zhengshuyun.lava.mail.OAuth2RefreshTokenCredential;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OAuth2AccessTokenProvider 单元测试
 *
 * @author Toint
 * @since 2026/4/21
 */
@DisplayName("OAuth2AccessTokenProvider 单元测试")
class OAuth2AccessTokenProviderTest {

    @Test
    @DisplayName("getAccessToken() - 未过期 token 应命中缓存")
    void testGetAccessTokenFromCache() {
        AtomicInteger requestCount = new AtomicInteger();
        OAuth2TokenClient tokenClient = credential -> {
            requestCount.incrementAndGet();
            return OAuth2AccessToken.builder()
                    .setAccessToken("token-1")
                    .setExpiresAt(Instant.now().plusSeconds(3600))
                    .build();
        };

        OAuth2AccessTokenProvider provider = new OAuth2AccessTokenProvider(tokenClient);
        OAuth2RefreshTokenCredential credential = createCredential();

        assertEquals("token-1", provider.getAccessToken(credential));
        assertEquals("token-1", provider.getAccessToken(credential));
        assertEquals(1, requestCount.get());
    }

    @Test
    @DisplayName("getAccessToken() - 临期 token 应提前刷新")
    void testRefreshExpiredAccessToken() {
        AtomicInteger requestCount = new AtomicInteger();
        OAuth2TokenClient tokenClient = credential -> {
            int index = requestCount.incrementAndGet();
            return OAuth2AccessToken.builder()
                    .setAccessToken("token-" + index)
                    .setExpiresAt(index == 1 ? Instant.now().plusSeconds(30) : Instant.now().plusSeconds(3600))
                    .build();
        };

        OAuth2AccessTokenProvider provider = new OAuth2AccessTokenProvider(tokenClient);
        OAuth2RefreshTokenCredential credential = createCredential();

        assertEquals("token-1", provider.getAccessToken(credential));
        assertEquals("token-2", provider.getAccessToken(credential));
        assertEquals(2, requestCount.get());
    }

    private static OAuth2RefreshTokenCredential createCredential() {
        return OAuth2RefreshTokenCredential.builder()
                .setUsername("test@hotmail.com")
                .setClientId("client-id")
                .setRefreshToken("refresh-token")
                .setTokenEndpoint("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                .addScope("https://outlook.office.com/IMAP.AccessAsUser.All")
                .build();
    }

}
