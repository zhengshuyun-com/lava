/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.bill;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * 普通商户日账单或月账单下载地址查询参数。
 */
public final class BillRequest {
    private static final Set<String> TYPES = Set.of(
            BillType.TRADE,
            BillType.SIGN_CUSTOMER,
            BillType.MERCHANT_ACTIVITY,
            BillType.TRADE_ZFT_MERCHANT,
            BillType.ZFT_ACCOUNT,
            BillType.SETTLEMENT_MERGE
    );

    private final String billType;
    private final @Nullable LocalDate date;
    private final @Nullable YearMonth month;
    private final @Nullable String smid;

    /**
     * 校验构建期参数并创建不可变账单请求。
     *
     * @param builder 构建器
     */
    private BillRequest(Builder builder) {
        billType = AlipayValidationUtils.requireOneOf(
                builder.billType, "billType", TYPES);
        ValidationUtils.requireTrue((builder.date == null) != (builder.month == null),
                "exactly one of date and month is required");
        ValidationUtils.requireTrue(!BillType.SETTLEMENT_MERGE.equals(billType)
                        || builder.date != null,
                "settlementMerge only supports a daily bill date");
        date = builder.date;
        month = builder.month;
        smid = AlipayValidationUtils.requireOptionalText(builder.smid, "smid", 20);
        ValidationUtils.requireTrue(smid == null
                        || BillType.TRADE_ZFT_MERCHANT.equals(billType),
                "smid is only supported for trade_zft_merchant bills");
    }

    /**
     * 创建对账单查询请求构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取账单类型。
     *
     * @return 账单类型
     */
    public String billType() {
        return billType;
    }

    /**
     * 获取日账单日期。
     *
     * @return 日账单日期；查询月账单时为 {@code null}
     */
    public @Nullable LocalDate date() {
        return date;
    }

    /**
     * 获取月账单月份。
     *
     * @return 月账单月份；查询日账单时为 {@code null}
     */
    public @Nullable YearMonth month() {
        return month;
    }

    /**
     * 获取直付通二级商户 SMID。
     *
     * @return 二级商户 SMID；没有时为 {@code null}
     */
    public @Nullable String smid() {
        return smid;
    }

    /** 对账单查询 fluent 构建器。 */
    public static final class Builder {
        private @Nullable String billType;
        private @Nullable LocalDate date;
        private @Nullable YearMonth month;
        private @Nullable String smid;

        /** 创建空账单请求构建器。 */
        private Builder() {
        }

        /**
         * 配置账单类型。
         *
         * @param value {@link BillType} 中的账单类型
         * @return 当前构建器
         */
        public Builder billType(String value) {
            billType = value;
            return this;
        }

        /**
         * 配置日账单日期。
         *
         * @param value 日账单日期
         * @return 当前构建器
         */
        public Builder date(LocalDate value) {
            date = ValidationUtils.requireNonNull(value, "date");
            return this;
        }

        /**
         * 配置月账单月份。
         *
         * @param value 月账单月份
         * @return 当前构建器
         */
        public Builder month(YearMonth value) {
            month = ValidationUtils.requireNonNull(value, "month");
            return this;
        }

        /**
         * 配置直付通二级商户 SMID，仅适用于 {@link BillType#TRADE_ZFT_MERCHANT}。
         *
         * @param value 二级商户 SMID
         * @return 当前构建器
         */
        public Builder smid(String value) {
            smid = value;
            return this;
        }

        /**
         * 校验参数并构建不可变账单查询请求。
         *
         * @return 不可变账单查询请求
         */
        public BillRequest build() {
            return new BillRequest(this);
        }
    }
}
