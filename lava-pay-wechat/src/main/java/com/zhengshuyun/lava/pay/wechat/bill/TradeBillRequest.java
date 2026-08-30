/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.bill;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * 申请交易账单的不可变查询参数。账单日期由客户端在发送前校验，
 * 必须早于当日且不早于最近三个月。
 */
public final class TradeBillRequest {
    /** 账单日期，按微信支付中国标准时区计算，不得为当日或更晚日期。 */
    private final LocalDate billDate;

    /** 交易账单类型；未配置时由微信支付按 {@link TradeBillType#ALL} 处理。 */
    private final @Nullable TradeBillType billType;

    /** 账单压缩类型；未配置时返回未压缩的原始文本。 */
    private final @Nullable BillTarType tarType;

    /**
     * 使用构建期参数创建不可变交易账单请求。
     *
     * @param builder 已收集查询参数的构建器
     * @throws IllegalArgumentException 未配置账单日期时抛出
     */
    private TradeBillRequest(Builder builder) {
        billDate = ValidationUtils.requireNonNull(builder.billDate, "billDate is required");
        billType = builder.billType;
        tarType = builder.tarType;
    }

    /**
     * 创建交易账单请求构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回账单日期。
     *
     * @return 账单日期
     */
    public LocalDate billDate() {
        return billDate;
    }

    /**
     * 返回交易账单类型。
     *
     * @return 账单类型；未配置时由微信支付按 ALL 处理
     */
    public @Nullable TradeBillType billType() {
        return billType;
    }

    /**
     * 返回账单压缩类型。
     *
     * @return 压缩类型；未配置时下载原始文本
     */
    public @Nullable BillTarType tarType() {
        return tarType;
    }

    /**
     * 交易账单请求构建器。
     */
    public static final class Builder {
        /** 待申请的账单日期；必填，初始为 {@code null}。 */
        private @Nullable LocalDate billDate;

        /** 待申请的交易账单类型；可选，初始为 {@code null}。 */
        private @Nullable TradeBillType billType;

        /** 待申请的账单压缩类型；可选，初始为 {@code null}。 */
        private @Nullable BillTarType tarType;

        /** 创建空交易账单请求构建器。 */
        private Builder() {
        }

        /**
         * 配置账单日期。
         *
         * @param value 账单日期
         * @return 当前构建器
         */
        public Builder billDate(LocalDate value) {
            billDate = ValidationUtils.requireNonNull(value, "billDate must not be null");
            return this;
        }

        /**
         * 配置交易账单类型。
         *
         * @param value 账单类型
         * @return 当前构建器
         */
        public Builder billType(TradeBillType value) {
            billType = ValidationUtils.requireNonNull(value, "billType must not be null");
            return this;
        }

        /**
         * 配置账单压缩类型。
         *
         * @param value 压缩类型
         * @return 当前构建器
         */
        public Builder tarType(BillTarType value) {
            tarType = ValidationUtils.requireNonNull(value, "tarType must not be null");
            return this;
        }

        /**
         * 校验并创建交易账单请求。
         *
         * @return 不可变交易账单请求
         * @throws IllegalArgumentException 未配置账单日期时抛出
         */
        public TradeBillRequest build() {
            return new TradeBillRequest(this);
        }
    }
}
