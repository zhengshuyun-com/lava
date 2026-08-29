/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.pagepay;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayMoneyUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 电脑网站支付单笔订单参数。
 *
 * <p>应用 ID、异步通知地址、同步返回地址、产品码和页面集成类型由客户端统一注入。
 * 金额单位为分，构建完成后对象不可变。</p>
 */
public final class PagePayRequest {
    private static final Set<String> QR_MODES = Set.of(
            PagePayQrMode.SIMPLE_FRONT,
            PagePayQrMode.FRONT,
            PagePayQrMode.REDIRECT,
            PagePayQrMode.MINI_FRONT,
            PagePayQrMode.CUSTOM_WIDTH
    );
    private static final Duration MIN_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration MAX_TIMEOUT = Duration.ofDays(15);

    private final String outTradeNo;
    private final long totalAmount;
    private final String subject;
    private final @Nullable String body;
    private final @Nullable LocalDateTime timeExpire;
    private final @Nullable Duration timeout;
    private final @Nullable String qrPayMode;
    private final @Nullable Integer qrcodeWidth;
    private final List<PagePayGoodsDetail> goodsDetail;
    private final Set<String> enablePayChannels;
    private final Set<String> disablePayChannels;
    private final @Nullable String storeId;
    private final @Nullable String merchantOrderNo;
    private final @Nullable String passbackParams;

    private PagePayRequest(Builder builder) {
        outTradeNo = AlipayValidationUtils.requireOutTradeNo(builder.outTradeNo);
        totalAmount = AlipayValidationUtils.requirePositiveAmount(
                ValidationUtils.requireNonNull(builder.totalAmount, "totalAmount is required"),
                AlipayMoneyUtils.MAX_PAYMENT_CENTS, "totalAmount");
        subject = AlipayValidationUtils.requireText(
                builder.subject,
                "subject",
                1,
                256
        );
        ValidationUtils.requireTrue(subject.codePoints().noneMatch(
                        value -> value == '/' || value == '=' || value == '&'),
                "subject must not contain '/', '=', or '&'");
        body = AlipayValidationUtils.requireOptionalText(builder.body, "body", 400);
        ValidationUtils.requireTrue(builder.timeExpire == null || builder.timeout == null,
                "timeExpire and timeout are mutually exclusive");
        timeExpire = builder.timeExpire;
        timeout = builder.timeout;
        if (timeout != null) {
            ValidationUtils.requireTrue(!timeout.isNegative() && !timeout.isZero()
                            && timeout.compareTo(MIN_TIMEOUT) >= 0
                            && timeout.compareTo(MAX_TIMEOUT) <= 0
                            && timeout.toSeconds() % 60 == 0,
                    "timeout must contain whole minutes between 1 minute and 15 days");
        }

        qrPayMode = builder.qrPayMode == null ? null
                : AlipayValidationUtils.requireOneOf(
                builder.qrPayMode, "qrPayMode", QR_MODES);
        qrcodeWidth = builder.qrcodeWidth;
        if (PagePayQrMode.CUSTOM_WIDTH.equals(qrPayMode)) {
            ValidationUtils.requireNonNull(qrcodeWidth,
                    "qrcodeWidth is required when qrPayMode is CUSTOM_WIDTH");
        } else {
            ValidationUtils.requireTrue(qrcodeWidth == null,
                    "qrcodeWidth is only valid when qrPayMode is CUSTOM_WIDTH");
        }
        if (qrcodeWidth != null) {
            ValidationUtils.requireTrue(qrcodeWidth > 0 && qrcodeWidth <= 9999,
                    "qrcodeWidth must be between 1 and 9999");
        }

        goodsDetail = List.copyOf(builder.goodsDetail);
        enablePayChannels = Collections.unmodifiableSet(
                new LinkedHashSet<>(builder.enablePayChannels));
        disablePayChannels = Collections.unmodifiableSet(
                new LinkedHashSet<>(builder.disablePayChannels));
        ValidationUtils.requireTrue(enablePayChannels.isEmpty() || disablePayChannels.isEmpty(),
                "enablePayChannels and disablePayChannels are mutually exclusive");
        storeId = AlipayValidationUtils.requireOptionalText(builder.storeId, "storeId", 32);
        merchantOrderNo = AlipayValidationUtils.requireOptionalText(
                builder.merchantOrderNo, "merchantOrderNo", 32);
        passbackParams = AlipayValidationUtils.requireOptionalText(
                builder.passbackParams, "passbackParams", 512);
    }

    /**
     * 创建电脑网站支付请求构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取商户订单号。
     *
     * @return 商户订单号
     */
    public String outTradeNo() {
        return outTradeNo;
    }

    /**
     * 获取订单金额。
     *
     * @return 订单金额，单位为分
     */
    public long totalAmount() {
        return totalAmount;
    }

    /**
     * 获取订单标题。
     *
     * @return 订单标题
     */
    public String subject() {
        return subject;
    }

    /**
     * 获取订单描述。
     *
     * @return 订单描述；没有时为 {@code null}
     */
    public @Nullable String body() {
        return body;
    }

    /**
     * 获取按 GMT+8 解释的绝对支付截止时间。
     *
     * @return 绝对支付截止时间；没有时为 {@code null}
     */
    public @Nullable LocalDateTime timeExpire() {
        return timeExpire;
    }

    /**
     * 获取相对支付超时。
     *
     * @return 相对支付超时；没有时为 {@code null}
     */
    public @Nullable Duration timeout() {
        return timeout;
    }

    /**
     * 获取二维码展示模式。
     *
     * @return 二维码模式；没有时为 {@code null}
     */
    public @Nullable String qrPayMode() {
        return qrPayMode;
    }

    /**
     * 获取自定义二维码宽度。
     *
     * @return 自定义二维码宽度；没有时为 {@code null}
     */
    public @Nullable Integer qrcodeWidth() {
        return qrcodeWidth;
    }

    /**
     * 获取商品明细。
     *
     * @return 不可变商品明细列表
     */
    public List<PagePayGoodsDetail> goodsDetail() {
        return goodsDetail;
    }

    /**
     * 获取指定可用的支付渠道。
     *
     * @return 不可变指定可用支付渠道集合
     */
    public Set<String> enablePayChannels() {
        return enablePayChannels;
    }

    /**
     * 获取禁用的支付渠道。
     *
     * @return 不可变禁用支付渠道集合
     */
    public Set<String> disablePayChannels() {
        return disablePayChannels;
    }

    /**
     * 获取商户门店号。
     *
     * @return 商户门店号；没有时为 {@code null}
     */
    public @Nullable String storeId() {
        return storeId;
    }

    /**
     * 获取商户原始订单号。
     *
     * @return 商户原始订单号；没有时为 {@code null}
     */
    public @Nullable String merchantOrderNo() {
        return merchantOrderNo;
    }

    /**
     * 获取异步通知回传参数。
     *
     * @return 异步通知回传参数；没有时为 {@code null}
     */
    public @Nullable String passbackParams() {
        return passbackParams;
    }

    /**
     * 电脑网站支付请求 fluent 构建器。
     */
    public static final class Builder {
        private @Nullable String outTradeNo;
        private @Nullable Long totalAmount;
        private @Nullable String subject;
        private @Nullable String body;
        private @Nullable LocalDateTime timeExpire;
        private @Nullable Duration timeout;
        private @Nullable String qrPayMode;
        private @Nullable Integer qrcodeWidth;
        private final List<PagePayGoodsDetail> goodsDetail = new ArrayList<>();
        private final Set<String> enablePayChannels = new LinkedHashSet<>();
        private final Set<String> disablePayChannels = new LinkedHashSet<>();
        private @Nullable String storeId;
        private @Nullable String merchantOrderNo;
        private @Nullable String passbackParams;

        private Builder() {
        }

        /**
         * 配置商户订单号。
         *
         * @param value 商户订单号
         * @return 当前构建器
         */
        public Builder outTradeNo(String value) {
            outTradeNo = value;
            return this;
        }

        /**
         * 配置订单金额。
         *
         * @param value 订单金额，单位为分
         * @return 当前构建器
         */
        public Builder totalAmount(long value) {
            totalAmount = value;
            return this;
        }

        /**
         * 配置订单标题。
         *
         * @param value 订单标题
         * @return 当前构建器
         */
        public Builder subject(String value) {
            subject = value;
            return this;
        }

        /**
         * 配置订单描述。
         *
         * @param value 订单描述
         * @return 当前构建器
         */
        public Builder body(String value) {
            body = value;
            return this;
        }

        /**
         * 配置绝对支付截止时间。
         *
         * @param value 绝对支付截止时间，按 GMT+8 解释
         * @return 当前构建器
         */
        public Builder timeExpire(LocalDateTime value) {
            timeExpire = value;
            return this;
        }

        /**
         * 配置相对支付超时。
         *
         * @param value 相对支付超时
         * @return 当前构建器
         */
        public Builder timeout(Duration value) {
            timeout = value;
            return this;
        }

        /**
         * 配置二维码展示模式。
         *
         * @param value {@link PagePayQrMode} 中的二维码模式常量
         * @return 当前构建器
         */
        public Builder qrPayMode(String value) {
            qrPayMode = value;
            return this;
        }

        /**
         * 配置自定义二维码宽度。
         *
         * @param value 自定义二维码宽度
         * @return 当前构建器
         */
        public Builder qrcodeWidth(int value) {
            qrcodeWidth = value;
            return this;
        }

        /**
         * 添加商品明细。
         *
         * @param value 商品明细
         * @return 当前构建器
         */
        public Builder addGoodsDetail(PagePayGoodsDetail value) {
            goodsDetail.add(ValidationUtils.requireNonNull(value, "goodsDetail"));
            return this;
        }

        /**
         * 添加指定可用支付渠道。
         *
         * @param value 指定可用支付渠道
         * @return 当前构建器
         */
        public Builder addEnablePayChannel(String value) {
            enablePayChannels.add(requireChannel(value));
            return this;
        }

        /**
         * 添加禁用支付渠道。
         *
         * @param value 禁用支付渠道
         * @return 当前构建器
         */
        public Builder addDisablePayChannel(String value) {
            disablePayChannels.add(requireChannel(value));
            return this;
        }

        /**
         * 配置商户门店号。
         *
         * @param value 商户门店号
         * @return 当前构建器
         */
        public Builder storeId(String value) {
            storeId = value;
            return this;
        }

        /**
         * 配置商户原始订单号。
         *
         * @param value 商户原始订单号
         * @return 当前构建器
         */
        public Builder merchantOrderNo(String value) {
            merchantOrderNo = value;
            return this;
        }

        /**
         * 配置异步通知回传参数。
         *
         * @param value 异步通知回传参数；生成表单时会按支付宝要求 URL 编码
         * @return 当前构建器
         */
        public Builder passbackParams(String value) {
            passbackParams = value;
            return this;
        }

        /**
         * 校验参数并构建不可变支付请求。
         *
         * @return 不可变支付请求
         */
        public PagePayRequest build() {
            return new PagePayRequest(this);
        }

        private static String requireChannel(String value) {
            value = AlipayValidationUtils.requireText(
                    value,
                    "payChannel",
                    1,
                    64
            );
            ValidationUtils.requireTrue(value.indexOf(',') < 0,
                    "payChannel must not contain commas");
            return value;
        }
    }
}
