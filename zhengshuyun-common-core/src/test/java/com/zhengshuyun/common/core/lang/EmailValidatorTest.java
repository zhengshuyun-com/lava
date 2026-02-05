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

package com.zhengshuyun.common.core.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {

    // 基本有效邮箱

    @Test
    void isValid_shouldPass_basicEmail() {
        assertTrue(EmailValidator.isValid("test@example.com"));
        assertTrue(EmailValidator.isValid("user+tag@domain.co"));
        assertTrue(EmailValidator.isValid("user.tag+tag@sub.domain.co"));
    }

    // 基本无效邮箱

    @Test
    void isValid_shouldFail_nullOrEmpty() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
    }

    @Test
    void isValid_shouldFail_invalidFormat() {
        assertFalse(EmailValidator.isValid("invalid"));
        assertFalse(EmailValidator.isValid("@example.com"));
        assertFalse(EmailValidator.isValid("test@"));
        assertFalse(EmailValidator.isValid("test@domain"));
        assertFalse(EmailValidator.isValid("test@-domain.com"));
        assertFalse(EmailValidator.isValid("test@domain..com"));
        assertFalse(EmailValidator.isValid("test..user@example.com"));
        assertFalse(EmailValidator.isValid("test@example.com."));
    }

    // 本地部分首尾点

    @Test
    void isValid_shouldFail_localPartStartsWithDot() {
        assertFalse(EmailValidator.isValid(".user@example.com"));
    }

    @Test
    void isValid_shouldFail_localPartEndsWithDot() {
        assertFalse(EmailValidator.isValid("user.@example.com"));
    }

    @Test
    void isValid_shouldFail_localPartConsecutiveDots() {
        assertFalse(EmailValidator.isValid("test..user@example.com"));
    }

    // 首尾空格

    @Test
    void isValid_shouldFail_leadingWhitespace() {
        assertFalse(EmailValidator.isValid(" test@example.com"));
    }

    @Test
    void isValid_shouldFail_trailingWhitespace() {
        assertFalse(EmailValidator.isValid("test@example.com "));
    }

    @Test
    void isValid_shouldFail_bothWhitespace() {
        assertFalse(EmailValidator.isValid(" test@example.com "));
    }

    // 长度边界

    @Test
    void isValid_shouldPass_localPartMaxLength() {
        String localPart64 = "a".repeat(64);
        assertTrue(EmailValidator.isValid(localPart64 + "@example.com"));
    }

    @Test
    void isValid_shouldFail_localPartTooLong() {
        String localPart65 = "a".repeat(65);
        assertFalse(EmailValidator.isValid(localPart65 + "@example.com"));
    }

    @Test
    void isValid_shouldPass_domainMaxLength() {
        // 255 字符的域名（包括点分隔符）
        // 例如: a.a.a....a.com（标签长度为 1，加点后为 2 字符）
        StringBuilder domain = new StringBuilder();
        // 125 个 "a." = 250 字符，再加 ".com" = 254 字符
        for (int i = 0; i < 125; i++) {
            domain.append("a.");
        }
        domain.append("com");
        String email = "test@" + domain;
        assertTrue(EmailValidator.isValid(email));
    }

    @Test
    void isValid_shouldFail_domainTooLong() {
        // 超过 255 字符的域名
        StringBuilder domain = new StringBuilder();
        for (int i = 0; i < 130; i++) {
            domain.append("a.");
        }
        domain.append("com");
        assertFalse(EmailValidator.isValid("test@" + domain));
    }

    // 国际化域名

    @Test
    void isValid_shouldPass_internationalDomain() {
        assertTrue(EmailValidator.isValid("user@münchen.de"));
    }

    @Test
    void isValid_shouldPass_chineseDomain() {
        assertTrue(EmailValidator.isValid("user@中国.cn"));
    }

    // 特殊字符

    @Test
    void isValid_shouldPass_specialCharacters() {
        assertTrue(EmailValidator.isValid("test!user@example.com"));
        assertTrue(EmailValidator.isValid("test#user@example.com"));
        assertTrue(EmailValidator.isValid("test$user@example.com"));
        assertTrue(EmailValidator.isValid("test%user@example.com"));
        assertTrue(EmailValidator.isValid("test&user@example.com"));
    }

    @Test
    void isValid_shouldFail_withSpace() {
        assertFalse(EmailValidator.isValid("test user@example.com"));
    }

    // 边界情况

    @Test
    void isValid_shouldFail_multipleAtSigns() {
        assertFalse(EmailValidator.isValid("test@user@example.com"));
    }

    @Test
    void isValid_shouldFail_noAtSign() {
        assertFalse(EmailValidator.isValid("testexample.com"));
    }

    @Test
    void isValid_shouldFail_domainStartsWithDot() {
        assertFalse(EmailValidator.isValid("test@.example.com"));
    }

    @Test
    void isValid_shouldFail_domainEndsWithDot() {
        assertFalse(EmailValidator.isValid("test@example.com."));
    }
}
