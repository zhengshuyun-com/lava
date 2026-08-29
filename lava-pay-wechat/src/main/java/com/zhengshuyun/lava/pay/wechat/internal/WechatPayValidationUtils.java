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

package com.zhengshuyun.lava.pay.wechat.internal;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 微信支付公开模型共用的字段校验工具。
 */
public final class WechatPayValidationUtils {
    private static final Pattern OUT_TRADE_NO = Pattern.compile("[0-9A-Za-z_\\-|*]{6,32}");
    private static final Pattern OUT_REFUND_NO = Pattern.compile("[0-9A-Za-z_\\-|*@]{1,64}");
    private static final Pattern MERCHANT_GOODS_ID = Pattern.compile("[0-9A-Za-z_-]{1,32}");

    private WechatPayValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 校验商户号。
     *
     * @param value 商户号
     * @return 原值
     */
    public static String requireMchid(String value) {
        return requireText(value, "mchid", 1, 32);
    }

    /**
     * 校验应用 ID。
     *
     * @param value 应用 ID
     * @return 原值
     */
    public static String requireAppid(String value) {
        return requireText(value, "appid", 1, 32);
    }

    /**
     * 校验商户订单号。
     *
     * @param value 商户订单号
     * @return 原值
     */
    public static String requireOutTradeNo(String value) {
        ValidationUtils.requireNotBlank(value, "outTradeNo must not be blank");
        ValidationUtils.requireTrue(OUT_TRADE_NO.matcher(value).matches(),
                "outTradeNo must contain 6-32 allowed characters");
        return value;
    }

    /**
     * 校验商户退款单号。
     *
     * @param value 商户退款单号
     * @return 原值
     */
    public static String requireOutRefundNo(String value) {
        ValidationUtils.requireNotBlank(value, "outRefundNo must not be blank");
        ValidationUtils.requireTrue(OUT_REFUND_NO.matcher(value).matches()
                        && utf8Length(value) <= 64,
                "outRefundNo must contain at most 64 bytes of allowed characters");
        return value;
    }

    /**
     * 校验商户侧商品编码。
     *
     * @param value 商品编码
     * @return 原值
     */
    public static String requireMerchantGoodsId(String value) {
        ValidationUtils.requireNotBlank(value, "merchantGoodsId must not be blank");
        ValidationUtils.requireTrue(MERCHANT_GOODS_ID.matcher(value).matches(),
                "merchantGoodsId must contain 1-32 letters, digits, hyphens, or underscores");
        return value;
    }

    /**
     * 校验不触发 DNS 查询的 IPv4 或 IPv6 字面量。
     *
     * @param value IP 地址文本
     * @param name 参数名
     * @return 原值
     */
    public static String requireIpAddress(String value, String name) {
        ValidationUtils.requireNotBlank(value, name + " must not be blank");
        try {
            InetAddress.ofLiteral(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be an IPv4 or IPv6 address");
        }
        return value;
    }

    /**
     * 校验微信支付订单号或退款单号等非空标识。
     *
     * @param value 标识值
     * @param name 参数名
     * @param maximum 最大字符数
     * @return 原值
     */
    public static String requireId(String value, String name, int maximum) {
        return requireText(value, name, 1, maximum);
    }

    /**
     * 校验普通文本和微信支付支持的 UTF-8 字符范围。
     *
     * @param value 文本
     * @param name 参数名
     * @param minimum 最少字符数
     * @param maximum 最大字符数
     * @return 原值
     */
    public static String requireText(String value, String name, int minimum, int maximum) {
        if (minimum > 0) {
            ValidationUtils.requireNotBlank(value, name + " must not be blank");
        } else {
            ValidationUtils.requireNonNull(value, name + " must not be null");
        }
        int characters = value.codePointCount(0, value.length());
        ValidationUtils.requireTrue(characters >= minimum && characters <= maximum,
                name + " length must be between " + minimum + " and " + maximum);
        ValidationUtils.requireTrue(value.codePoints().noneMatch(codePoint -> codePoint > 0xFFFF),
                name + " contains a character unsupported by WeChat Pay UTF-8 rules");
        return value;
    }

    /**
     * 校验按 UTF-8 字节数限制的可选文本。
     *
     * @param value 可选文本
     * @param name 参数名
     * @param maximumBytes 最大字节数
     * @return 原值
     */
    public static @Nullable String requireOptionalBytes(@Nullable String value, String name,
                                                        int maximumBytes) {
        if (value == null) {
            return null;
        }
        ValidationUtils.requireTrue(utf8Length(value) <= maximumBytes,
                name + " must not exceed " + maximumBytes + " UTF-8 bytes");
        ValidationUtils.requireTrue(value.codePoints().noneMatch(codePoint -> codePoint > 0xFFFF),
                name + " contains a character unsupported by WeChat Pay UTF-8 rules");
        return value;
    }

    /**
     * 校验正金额。
     *
     * @param value 金额，单位为分
     * @param name 参数名
     * @return 原值
     */
    public static long requirePositive(long value, String name) {
        ValidationUtils.requireTrue(value > 0, name + " must be positive");
        return value;
    }

    /**
     * 校验非负金额或数量。
     *
     * @param value 数值
     * @param name 参数名
     * @return 原值
     */
    public static long requireNonNegative(long value, String name) {
        ValidationUtils.requireTrue(value >= 0, name + " must not be negative");
        return value;
    }

    /**
     * 校验微信支付通知地址。
     *
     * @param value 通知地址
     * @param maximum 最大字符数
     * @return 原值
     */
    public static URI requireNotifyUrl(URI value, int maximum) {
        ValidationUtils.requireNonNull(value, "notifyUrl must not be null");
        ValidationUtils.requireTrue(value.isAbsolute() && "https".equalsIgnoreCase(value.getScheme()),
                "notifyUrl must be an absolute HTTPS URL");
        ValidationUtils.requireTrue(value.getHost() != null && !value.getHost().isBlank(),
                "notifyUrl must contain a host");
        ValidationUtils.requireTrue(value.getRawPath() != null
                        && !value.getRawPath().isBlank()
                        && !"/".equals(value.getRawPath()),
                "notifyUrl must contain a complete path");
        ValidationUtils.requireTrue(value.getRawQuery() == null && value.getRawFragment() == null,
                "notifyUrl must not contain query parameters or a fragment");
        ValidationUtils.requireTrue(value.getUserInfo() == null,
                "notifyUrl must not contain user information");
        ValidationUtils.requireTrue(value.toASCIIString().length() <= maximum,
                "notifyUrl must not exceed " + maximum + " characters");
        return value;
    }

    /**
     * 返回 UTF-8 字节长度。
     *
     * @param value 文本
     * @return UTF-8 字节数
     */
    public static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
