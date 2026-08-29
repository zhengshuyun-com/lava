/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.transaction;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import org.jspecify.annotations.Nullable;

/**
 * 统一收单交易关闭参数。商户订单号与支付宝交易号至少提供一个；同时提供时支付宝优先使用交易号。
 */
public final class TradeCloseRequest {
    private final @Nullable String outTradeNo;
    private final @Nullable String tradeNo;
    private final @Nullable String operatorId;

    /** 使用构建期参数创建并校验关单请求。 */
    private TradeCloseRequest(Builder builder) {
        ValidationUtils.requireTrue(builder.outTradeNo != null || builder.tradeNo != null,
                "at least one of outTradeNo and tradeNo is required");
        outTradeNo = builder.outTradeNo == null ? null
                : AlipayValidationUtils.requireOutTradeNo(builder.outTradeNo);
        tradeNo = builder.tradeNo == null ? null
                : AlipayValidationUtils.requireTradeNo(builder.tradeNo);
        operatorId = AlipayValidationUtils.requireOptionalText(
                builder.operatorId, "operatorId", 28);
    }

    /**
     * 创建交易关闭请求构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取商户订单号。
     *
     * @return 商户订单号；未使用时为 {@code null}
     */
    public @Nullable String outTradeNo() {
        return outTradeNo;
    }

    /**
     * 获取支付宝交易号。
     *
     * @return 支付宝交易号；未使用时为 {@code null}
     */
    public @Nullable String tradeNo() {
        return tradeNo;
    }

    /**
     * 获取商家操作员编号。
     *
     * @return 商家操作员编号；没有时为 {@code null}
     */
    public @Nullable String operatorId() {
        return operatorId;
    }

    /** 交易关闭 fluent 构建器。 */
    public static final class Builder {
        private @Nullable String outTradeNo;
        private @Nullable String tradeNo;
        private @Nullable String operatorId;

        /** 创建空关单请求构建器。 */
        private Builder() {
        }

        /**
         * 配置商户订单号并选择按商户订单关闭。
         *
         * @param value 商户订单号
         * @return 当前构建器
         */
        public Builder outTradeNo(String value) {
            outTradeNo = value;
            return this;
        }

        /**
         * 配置支付宝交易号并选择按支付宝交易关闭。
         *
         * @param value 支付宝交易号
         * @return 当前构建器
         */
        public Builder tradeNo(String value) {
            tradeNo = value;
            return this;
        }

        /**
         * 配置商家操作员编号。
         *
         * @param value 操作员编号
         * @return 当前构建器
         */
        public Builder operatorId(String value) {
            operatorId = value;
            return this;
        }

        /**
         * 校验参数并构建不可变关闭请求。
         *
         * @return 不可变关闭请求
         */
        public TradeCloseRequest build() {
            return new TradeCloseRequest(this);
        }
    }
}
