/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import okhttp3.HttpUrl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 内部的凭证安全诊断格式化工具。
 */
final class HttpRedactionUtils {
    private static final String REDACTED = "[REDACTED]";
    private static final String ENCODED_REDACTED = "%5BREDACTED%5D";

    private static final Set<String> SENSITIVE_COMPACT_NAMES = Set.of(
            "authorization", "proxyauthorization", "cookie", "setcookie",
            "token", "accesstoken", "refreshtoken", "idtoken",
            "apikey", "xapikey", "secret", "clientsecret", "password", "passwd",
            "credential", "credentials", "signature", "sig");
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "authorization", "cookie", "token", "secret", "password", "passwd",
            "credential", "credentials", "signature");
    private static final Set<String> URL_VALUE_HEADERS = Set.of(
            "location", "content-location", "referer", "referrer", "destination");

    private HttpRedactionUtils() {
    }

    static boolean isSensitiveName(String name) {
        if (SENSITIVE_COMPACT_NAMES.contains(compactName(name))) {
            return true;
        }

        List<String> words = logicalWords(name);
        for (int index = 0; index < words.size(); index++) {
            String word = words.get(index);
            if (SENSITIVE_WORDS.contains(word)) {
                return true;
            }
            if (word.equals("api") && index + 1 < words.size()
                    && words.get(index + 1).equals("key")) {
                return true;
            }
        }
        return false;
    }

    static String redactHeaderValue(String name, String value) {
        if (isSensitiveName(name)) {
            return REDACTED;
        }

        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals("link")) {
            return redactLinkValue(value);
        }
        if (normalized.equals("refresh")) {
            return redactRefreshValue(value);
        }
        if (isUrlValueHeader(name, normalized)) {
            return redactPossiblyQuotedUrl(value);
        }
        return value;
    }

    static String redactUrl(String url) {
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null) {
            return "[invalid URL]";
        }
        return redactHttpUrl(parsed);
    }

    private static String redactHttpUrl(HttpUrl parsed) {
        HttpUrl.Builder builder = parsed.newBuilder();
        if (!parsed.username().isEmpty()) {
            builder.username(REDACTED);
        }
        if (!parsed.password().isEmpty()) {
            builder.password(REDACTED);
        }
        String encodedQuery = parsed.encodedQuery();
        if (encodedQuery != null) {
            builder.encodedQuery(redactParameterRange(encodedQuery, 0, encodedQuery.length()));
        }
        String encodedFragment = parsed.encodedFragment();
        if (encodedFragment != null) {
            builder.encodedFragment(redactFragmentParameters(encodedFragment));
        }
        return builder.build().toString();
    }

    private static boolean isUrlValueHeader(String name, String normalized) {
        if (URL_VALUE_HEADERS.contains(normalized)) {
            return true;
        }
        List<String> words = logicalWords(name);
        if (words.isEmpty()) {
            return false;
        }
        String last = words.getLast();
        return last.equals("url") || last.equals("uri");
    }

    private static String redactLinkValue(String value) {
        StringBuilder result = new StringBuilder(value.length());
        int copiedThrough = 0;
        // Link 可包含多个尖括号包裹的 URL；只改写 URL 部分，保留 rel 等参数原样输出。
        while (copiedThrough < value.length()) {
            int open = value.indexOf('<', copiedThrough);
            if (open < 0) {
                result.append(redactUrlReference(value.substring(copiedThrough)));
                break;
            }
            int close = value.indexOf('>', open + 1);
            if (close < 0) {
                result.append(redactUrlReference(value.substring(copiedThrough)));
                break;
            }
            result.append(value, copiedThrough, open + 1);
            result.append(redactUrlReference(value.substring(open + 1, close)));
            result.append('>');
            copiedThrough = close + 1;
        }
        return result.toString();
    }

    private static String redactRefreshValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        int searchFrom = 0;
        while (searchFrom < normalized.length()) {
            int url = normalized.indexOf("url", searchFrom);
            if (url < 0) {
                return redactRawUrlParameters(value);
            }
            int equals = url + 3;
            while (equals < value.length() && value.charAt(equals) == ' ') {
                equals++;
            }
            boolean validPrefix = url == 0 || value.charAt(url - 1) == ';'
                    || Character.isWhitespace(value.charAt(url - 1));
            if (validPrefix && equals < value.length() && value.charAt(equals) == '=') {
                return value.substring(0, equals + 1)
                        + redactPossiblyQuotedUrl(value.substring(equals + 1));
            }
            searchFrom = url + 3;
        }
        return redactRawUrlParameters(value);
    }

    private static String redactPossiblyQuotedUrl(String value) {
        int start = 0;
        while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
            start++;
        }
        int end = value.length();
        while (end > start && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }

        boolean quoted = end - start >= 2
                && (value.charAt(start) == '"' || value.charAt(start) == '\'')
                && value.charAt(end - 1) == value.charAt(start);
        int referenceStart = quoted ? start + 1 : start;
        int referenceEnd = quoted ? end - 1 : end;
        return value.substring(0, referenceStart)
                + redactUrlReference(value.substring(referenceStart, referenceEnd))
                + value.substring(referenceEnd);
    }

    private static String redactUrlReference(String reference) {
        HttpUrl parsed = HttpUrl.parse(reference);
        if (parsed != null) {
            return redactHttpUrl(parsed);
        }
        if (reference.startsWith("//")) {
            HttpUrl protocolRelative = HttpUrl.parse("https:" + reference);
            if (protocolRelative != null) {
                return redactHttpUrl(protocolRelative).substring("https:".length());
            }
        }
        return redactRawUrlParameters(redactUserInfo(reference));
    }

    private static String redactRawUrlParameters(String value) {
        int fragment = value.indexOf('#');
        String result = value;
        if (fragment >= 0) {
            result = value.substring(0, fragment + 1)
                    + redactFragmentParameters(value.substring(fragment + 1));
        }

        int question = result.indexOf('?');
        if (question < 0) {
            return result;
        }
        int precedingFragment = result.indexOf('#');
        if (precedingFragment >= 0 && precedingFragment < question) {
            return result;
        }
        int queryEnd = precedingFragment < 0 ? result.length() : precedingFragment;
        return redactParameterRange(result, question + 1, queryEnd);
    }

    private static String redactFragmentParameters(String fragment) {
        int question = fragment.indexOf('?');
        int parametersStart = question < 0 ? 0 : question + 1;
        return redactParameterRange(fragment, parametersStart, fragment.length());
    }

    private static String redactParameterRange(String value, int rangeStart, int rangeEnd) {
        StringBuilder result = new StringBuilder(value.length());
        result.append(value, 0, rangeStart);

        int parameterStart = rangeStart;
        while (parameterStart <= rangeEnd) {
            int separator = nextParameterSeparator(value, parameterStart, rangeEnd);
            int equals = value.indexOf('=', parameterStart);
            if (equals < 0 || equals >= separator) {
                equals = separator;
            }
            String rawName = value.substring(parameterStart, equals);
            result.append(rawName);
            if (isSensitiveName(decodeQueryName(rawName))) {
                result.append('=').append(ENCODED_REDACTED);
            } else {
                result.append(value, equals, separator);
            }
            if (separator == rangeEnd) {
                break;
            }
            result.append(value.charAt(separator));
            parameterStart = separator + 1;
        }
        result.append(value, rangeEnd, value.length());
        return result.toString();
    }

    private static int nextParameterSeparator(String value, int start, int end) {
        for (int index = start; index < end; index++) {
            char character = value.charAt(index);
            if (character == '&' || character == ';') {
                return index;
            }
        }
        return end;
    }

    private static String redactUserInfo(String value) {
        int authorityStart;
        if (value.startsWith("//")) {
            authorityStart = 2;
        } else {
            int scheme = value.indexOf("://");
            if (scheme < 0) {
                return value;
            }
            authorityStart = scheme + 3;
        }

        int authorityEnd = value.length();
        for (int index = authorityStart; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '/' || character == '?' || character == '#') {
                authorityEnd = index;
                break;
            }
        }
        int at = value.lastIndexOf('@', authorityEnd - 1);
        if (at < authorityStart) {
            return value;
        }
        return value.substring(0, authorityStart) + REDACTED + value.substring(at);
    }

    private static String decodeQueryName(String name) {
        try {
            return URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return name;
        }
    }

    private static String compactName(String name) {
        StringBuilder result = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (Character.isLetterOrDigit(character)) {
                result.append(Character.toLowerCase(character));
            }
        }
        return result.toString();
    }

    private static List<String> logicalWords(String name) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (!Character.isLetterOrDigit(character)) {
                addWord(result, current);
                continue;
            }

            if (Character.isUpperCase(character) && !current.isEmpty()) {
                char previous = name.charAt(index - 1);
                boolean followsLowercase = Character.isLowerCase(previous)
                        || Character.isDigit(previous);
                boolean startsFinalCapitalizedWord = Character.isUpperCase(previous)
                        && index + 1 < name.length()
                        && Character.isLowerCase(name.charAt(index + 1));
                if (followsLowercase || startsFinalCapitalizedWord) {
                    addWord(result, current);
                }
            }
            current.append(Character.toLowerCase(character));
        }
        addWord(result, current);
        return result;
    }

    private static void addWord(List<String> words, StringBuilder word) {
        if (!word.isEmpty()) {
            words.add(word.toString());
            word.setLength(0);
        }
    }
}
