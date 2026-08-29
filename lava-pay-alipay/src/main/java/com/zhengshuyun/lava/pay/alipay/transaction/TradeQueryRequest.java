/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.transaction;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 支付宝交易查询参数，商户订单号与支付宝交易号严格二选一。
 */
public final class TradeQueryRequest {
    private static final Set<String> OPTIONS = Set.of(
            TradeQueryOption.TRADE_SETTLE_INFO,
            TradeQueryOption.FUND_BILL_LIST,
            TradeQueryOption.VOUCHER_DETAIL_LIST,
            TradeQueryOption.DISCOUNT_GOODS_DETAIL,
            TradeQueryOption.MDISCOUNT_AMOUNT,
            TradeQueryOption.MEDICAL_INSURANCE_INFO
    );

    private final @Nullable String outTradeNo;
    private final @Nullable String tradeNo;
    private final List<String> queryOptions;

    private TradeQueryRequest(Builder builder) {
        ValidationUtils.requireTrue((builder.outTradeNo == null) != (builder.tradeNo == null),
                "exactly one of outTradeNo and tradeNo is required");
        outTradeNo = builder.outTradeNo == null ? null
                : AlipayValidationUtils.requireOutTradeNo(builder.outTradeNo);
        tradeNo = builder.tradeNo == null ? null
                : AlipayValidationUtils.requireTradeNo(builder.tradeNo);
        queryOptions = List.copyOf(builder.queryOptions);
    }

    /**
     * 创建交易查询请求构建器。
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
     * 获取查询选项。
     *
     * @return 不可变查询选项列表
     */
    public List<String> queryOptions() {
        return queryOptions;
    }

    /** 交易查询 fluent 构建器。 */
    public static final class Builder {
        private @Nullable String outTradeNo;
        private @Nullable String tradeNo;
        private final Set<String> queryOptions = new LinkedHashSet<>();

        private Builder() {
        }

        /**
         * 配置商户订单号并选择按商户订单查询。
         *
         * @param value 商户订单号
         * @return 当前构建器
         */
        public Builder outTradeNo(String value) {
            outTradeNo = value;
            return this;
        }

        /**
         * 配置支付宝交易号并选择按支付宝交易查询。
         *
         * @param value 支付宝交易号
         * @return 当前构建器
         */
        public Builder tradeNo(String value) {
            tradeNo = value;
            return this;
        }

        /**
         * 添加交易查询选项。
         *
         * @param value {@link TradeQueryOption} 中的查询选项
         * @return 当前构建器
         */
        public Builder addQueryOption(String value) {
            queryOptions.add(AlipayValidationUtils.requireOneOf(
                    value, "queryOption", OPTIONS));
            return this;
        }

        /**
         * 校验参数并构建不可变查询请求。
         *
         * @return 不可变查询请求
         */
        public TradeQueryRequest build() {
            return new TradeQueryRequest(this);
        }
    }
}
