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

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 包内使用的 bearer token 及服务端提供的可选到期时间。
 */
final class OAuth2AccessToken {
    private final String value;
    private final @Nullable Instant expiresAt;

    OAuth2AccessToken(String value, @Nullable Instant expiresAt) {
        value = ValidationUtils.requireNotBlank(value, "access token must not be blank");
        if (value.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("access token must not contain whitespace");
        }
        this.value = value;
        this.expiresAt = expiresAt;
    }

    String value() {
        return value;
    }

    @Nullable Instant expiresAt() {
        return expiresAt;
    }

    boolean reusable(Clock clock, Duration refreshAhead) {
        if (expiresAt == null) {
            return false;
        }
        try {
            return clock.instant().plus(refreshAhead).isBefore(expiresAt);
        } catch (ArithmeticException | java.time.DateTimeException exception) {
            return false;
        }
    }
}
