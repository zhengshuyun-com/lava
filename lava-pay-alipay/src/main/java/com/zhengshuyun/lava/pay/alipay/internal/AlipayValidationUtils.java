/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayProtocolException;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 支付宝公共配置与业务字段校验工具。
 */
public final class AlipayValidationUtils {
    /** 允许承载生产签名请求的支付宝正式和沙箱域名。 */
    private static final Set<String> OFFICIAL_OPENAPI_HOSTS = Set.of(
            "openapi.alipay.com",
            "openapi-sandbox.dl.alipaydev.com"
    );
    /** 应用 ID 格式：1 至 32 个 ASCII 字母或数字。 */
    private static final Pattern APP_ID = Pattern.compile("[A-Za-z0-9]{1,32}");
    /** 卖家用户 ID 格式：以 2088 开头的 16 位数字。 */
    private static final Pattern SELLER_ID = Pattern.compile("2088[0-9]{12}");
    /** 商户订单号格式：1 至 64 个 ASCII 字母、数字或下划线。 */
    private static final Pattern OUT_TRADE_NO = Pattern.compile("[A-Za-z0-9_]{1,64}");

    /** 禁止实例化支付宝校验工具。 */
    private AlipayValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 校验支付宝应用 ID 格式。
     *
     * @param value 应用 ID
     * @return 已校验应用 ID
     */
    public static String requireAppId(String value) {
        ValidationUtils.requireNonNull(value, "appId is required");
        ValidationUtils.requireTrue(APP_ID.matcher(value).matches(), "appId format is invalid");
        return value;
    }

    /**
     * 校验卖家支付宝用户 ID 格式。
     *
     * @param value 卖家支付宝用户 ID
     * @return 已校验卖家支付宝用户 ID
     */
    public static String requireSellerId(String value) {
        ValidationUtils.requireNonNull(value, "sellerId is required");
        ValidationUtils.requireTrue(SELLER_ID.matcher(value).matches(),
                "sellerId must start with 2088 and contain exactly 16 digits");
        return value;
    }

    /**
     * 校验商户订单号格式。
     *
     * @param value 商户订单号
     * @return 已校验商户订单号
     */
    public static String requireOutTradeNo(String value) {
        ValidationUtils.requireNonNull(value, "outTradeNo is required");
        ValidationUtils.requireTrue(OUT_TRADE_NO.matcher(value).matches(),
                "outTradeNo must contain 1 to 64 letters, digits, or underscores");
        return value;
    }

    /**
     * 校验退款请求号格式。
     *
     * @param value 退款请求号
     * @return 已校验退款请求号
     */
    public static String requireOutRequestNo(String value) {
        return requireIdentifier(value, "outRequestNo", 64);
    }

    /**
     * 校验支付宝交易号格式。
     *
     * @param value 支付宝交易号
     * @return 已校验支付宝交易号
     */
    public static String requireTradeNo(String value) {
        return requireIdentifier(value, "tradeNo", 64);
    }

    /**
     * 校验通用标识符。
     *
     * @param value   标识符
     * @param name    字段名
     * @param maximum 最大字符数
     * @return 原值
     */
    public static String requireIdentifier(String value, String name, int maximum) {
        return requireText(
                value,
                name,
                1,
                maximum
        );
    }

    /**
     * 校验文本字符数与控制字符。
     *
     * @param value   文本
     * @param name    字段名
     * @param minimum 最少 Unicode 字符数
     * @param maximum 最多 Unicode 字符数
     * @return 原值
     */
    public static String requireText(
            String value,
            String name,
            int minimum,
            int maximum
    ) {
        ValidationUtils.requireNonNull(value, name + " is required");
        int length = value.codePointCount(0, value.length());
        ValidationUtils.requireTrue(length >= minimum && length <= maximum,
                name + " length is out of range");
        ValidationUtils.requireTrue(value.codePoints().noneMatch(Character::isISOControl),
                name + " must not contain control characters");
        return value;
    }

    /**
     * 校验可选文本。
     *
     * @param value   可选文本
     * @param name    字段名
     * @param maximum 最大字符数
     * @return 原值
     */
    public static @Nullable String requireOptionalText(@Nullable String value,
                                                       String name, int maximum) {
        return value == null ? null : requireText(
                value,
                name,
                1,
                maximum
        );
    }

    /**
     * 校验金额位于允许的正数范围内。
     *
     * @param value   金额，单位为分
     * @param maximum 最大金额，单位为分
     * @param name    字段名
     * @return 已校验正数金额
     */
    public static long requirePositiveAmount(long value, long maximum, String name) {
        ValidationUtils.requireTrue(value > 0 && value <= maximum,
                name + " must be between 1 and " + maximum + " cents");
        return value;
    }

    /**
     * 校验支付通知或同步响应地址。
     *
     * @param value 地址
     * @param name  字段名
     * @return 原地址
     */
    public static URI requireCallbackUrl(URI value, String name) {
        ValidationUtils.requireNonNull(value, name + " is required");
        String scheme = value.getScheme();
        ValidationUtils.requireTrue(value.isAbsolute() && value.getHost() != null
                        && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)),
                name + " must be an absolute HTTP or HTTPS URI");
        ValidationUtils.requireTrue(value.getUserInfo() == null && value.getRawFragment() == null,
                name + " must not contain user information or a fragment");
        ValidationUtils.requireTrue(value.toASCIIString().length() <= 256,
                name + " must not exceed 256 characters");
        return value;
    }

    /**
     * 校验支付宝 OpenAPI 基础地址。生产地址仅允许官方正式或沙箱域名，本地协议测试允许环回地址。
     *
     * @param value OpenAPI 基础地址
     * @return 原地址
     */
    public static URI requireBaseUrl(URI value) {
        ValidationUtils.requireNonNull(value, "baseUrl is required");
        ValidationUtils.requireTrue(value.isAbsolute() && value.getHost() != null,
                "baseUrl must be absolute");
        ValidationUtils.requireTrue(value.getUserInfo() == null && value.getRawQuery() == null
                        && value.getRawFragment() == null,
                "baseUrl must not contain user information, query, or fragment");
        ValidationUtils.requireTrue(value.getRawPath() == null || value.getRawPath().isEmpty()
                        || "/".equals(value.getRawPath()),
                "baseUrl must not contain an API path");
        String host = value.getHost().toLowerCase(Locale.ROOT);
        boolean loopback = "localhost".equals(host) || "127.0.0.1".equals(host)
                || "::1".equals(host);
        boolean official = "https".equalsIgnoreCase(value.getScheme())
                && OFFICIAL_OPENAPI_HOSTS.contains(host)
                && (value.getPort() == -1 || value.getPort() == 443);
        boolean localTest = loopback
                && ("http".equalsIgnoreCase(value.getScheme())
                || "https".equalsIgnoreCase(value.getScheme()));
        ValidationUtils.requireTrue(
                official || localTest,
                "baseUrl must use an official Alipay HTTPS host or a loopback test host"
        );
        return value;
    }

    /**
     * 校验值属于允许集合。
     *
     * @param value   值
     * @param name    字段名
     * @param allowed 允许集合
     * @return 原值
     */
    public static String requireOneOf(String value, String name, Set<String> allowed) {
        ValidationUtils.requireNonNull(value, name + " is required");
        ValidationUtils.requireTrue(allowed.contains(value), name + " is unsupported");
        return value;
    }

    /**
     * 要求响应标识与请求一致。
     *
     * @param expected 期望值
     * @param actual   实际值
     */
    public static void requireSame(String expected, @Nullable String actual) {
        if (!expected.equals(actual)) {
            throw new com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityException(
                    com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityFailure.RESPONSE_MISMATCH);
        }
    }

    /**
     * 从协议响应取得必填文本。
     *
     * @param value 字段值
     * @param name  字段名
     * @return 非空值
     */
    public static String requireResponseText(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            throw new AlipayProtocolException("支付宝响应缺少字段 " + name);
        }
        return value;
    }
}
