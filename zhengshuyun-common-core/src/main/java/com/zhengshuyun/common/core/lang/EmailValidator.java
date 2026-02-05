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

import org.jspecify.annotations.Nullable;

import java.net.IDN;
import java.util.regex.Pattern;

/**
 * 邮箱校验工具类, 基于 RFC 5321/5322 规范
 *
 * <p>支持的特性:
 * <ul>
 *   <li>本地部分最大 64 字符</li>
 *   <li>域名部分最大 255 字符</li>
 *   <li>禁止首尾点和连续点</li>
 *   <li>禁止首尾空格</li>
 *   <li>支持国际化域名 (IDN)</li>
 *   <li>支持特殊字符 (!, #, $, %, &amp; 等)</li>
 * </ul>
 *
 * @author Toint
 * @since 2026/1/1
 */
public final class EmailValidator {

    /** 邮箱本地部分最大长度 (RFC 5321) */
    private static final int MAX_EMAIL_LOCAL_PART_LENGTH = 64;

    /** 邮箱域名部分最大长度 (RFC 5321) */
    private static final int MAX_EMAIL_DOMAIN_PART_LENGTH = 255;

    /**
     * 邮箱本地部分原子字符集
     * <p>包括: a-z, 0-9, 特殊字符 (!, #, $, %, &, ', *, +, /, =, ?, ^, _, `, {, |, }, ~, -) 和 Unicode 字符
     */
    private static final String EMAIL_LOCAL_PART_ATOM = "[a-z0-9!#$%&'*+/=?^_`{|}~\\-\\u0080-\\uFFFF]";

    /**
     * 邮箱本地部分引号内字符集
     * <p>在引号内可以包含更多字符, 包括空格、括号、方括号等
     */
    private static final String EMAIL_LOCAL_PART_INSIDE_QUOTES_ATOM =
            "(?:[a-z0-9!#$%&'*.(),<>\\[\\]:;  @+/=?^_`{|}~\\-\\u0080-\\uFFFF]|\\\\\\\\|\\\\\")";

    /**
     * 邮箱本地部分正则表达式
     * <p>支持普通原子字符和引号字符串, 以及点分隔
     */
    private static final Pattern EMAIL_LOCAL_PART_PATTERN = Pattern.compile(
            "(?:" + EMAIL_LOCAL_PART_ATOM + "+|\"" + EMAIL_LOCAL_PART_INSIDE_QUOTES_ATOM + "+\")" +
                    "(?:\\." + "(?:" + EMAIL_LOCAL_PART_ATOM + "+|\"" + EMAIL_LOCAL_PART_INSIDE_QUOTES_ATOM + "+\")" + ")*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 邮箱域名标签正则表达式
     * <p>支持字母、数字、连字符, 不能以连字符开头或结尾
     */
    private static final String EMAIL_DOMAIN_LABEL = "\\p{Alnum}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    /**
     * 邮箱顶级域名正则表达式
     * <p>必须以字母开头, 支持字母、数字、连字符
     */
    private static final String EMAIL_DOMAIN_TLD = "\\p{Alpha}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    /**
     * 邮箱域名正则表达式
     * <p>支持多级域名和顶级域名
     */
    private static final Pattern EMAIL_DOMAIN_PATTERN = Pattern.compile(
            "^(?:" + EMAIL_DOMAIN_LABEL + "\\.)+" + EMAIL_DOMAIN_TLD + "$",
            Pattern.CASE_INSENSITIVE
    );

    private EmailValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 校验邮箱格式是否有效
     *
     * @param email 待校验的邮箱地址
     * @return 如果邮箱格式有效返回 true, 否则返回 false
     */
    public static boolean isValid(@Nullable String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        // 首尾空格检查(RFC 5321 不允许首尾空格)
        if (!email.equals(email.strip())) {
            return false;
        }

        int splitPosition = email.lastIndexOf('@');
        if (splitPosition <= 0 || splitPosition == email.length() - 1) {
            return false;
        }

        String localPart = email.substring(0, splitPosition);
        String domainPart = email.substring(splitPosition + 1);

        return isValidLocalPart(localPart) && isValidDomain(domainPart);
    }

    /**
     * 校验邮箱本地部分是否有效
     *
     * @param localPart 邮箱本地部分(@ 符号之前)
     * @return 如果本地部分有效返回 true, 否则返回 false
     */
    private static boolean isValidLocalPart(String localPart) {
        if (localPart.length() > MAX_EMAIL_LOCAL_PART_LENGTH) {
            return false;
        }

        // 首尾点检查(RFC 5321 不允许本地部分以点开头或结尾)
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            return false;
        }

        // 连续点检查(RFC 5321 不允许本地部分包含连续点)
        if (localPart.contains("..")) {
            return false;
        }

        return EMAIL_LOCAL_PART_PATTERN.matcher(localPart).matches();
    }

    /**
     * 校验邮箱域名部分是否有效
     *
     * @param domainPart 邮箱域名部分(@ 符号之后)
     * @return 如果域名部分有效返回 true, 否则返回 false
     */
    private static boolean isValidDomain(String domainPart) {
        if (domainPart.isEmpty() || domainPart.endsWith(".")) {
            return false;
        }

        String asciiDomain;
        try {
            asciiDomain = IDN.toASCII(domainPart);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (asciiDomain.length() > MAX_EMAIL_DOMAIN_PART_LENGTH) {
            return false;
        }

        return EMAIL_DOMAIN_PATTERN.matcher(asciiDomain).matches();
    }
}
