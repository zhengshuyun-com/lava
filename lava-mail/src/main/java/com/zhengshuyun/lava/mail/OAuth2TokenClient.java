/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhengshuyun.lava.mail;

import java.time.Clock;

/**
 * 由单个发件器或收件器实例独占的 token 传输客户端。
 */
@FunctionalInterface
interface OAuth2TokenClient extends AutoCloseable {
    OAuth2AccessToken fetchAccessToken(OAuth2RefreshTokenCredential credential, Clock clock);

    static OAuth2TokenClient createDefault() {
        return new DefaultOAuth2TokenClient();
    }

    @Override
    default void close() {
    }
}
