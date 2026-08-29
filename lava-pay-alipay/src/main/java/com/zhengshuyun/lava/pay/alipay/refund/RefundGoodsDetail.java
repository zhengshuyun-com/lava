/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个退款商品明细。
 */
public final class RefundGoodsDetail {
    private final String goodsId;
    private final long refundAmount;
    private final @Nullable String outItemId;
    private final @Nullable String outSkuId;
    private final List<String> outCertificateNos;

    private RefundGoodsDetail(Builder builder) {
        goodsId = AlipayValidationUtils.requireIdentifier(builder.goodsId, "goodsId", 32);
        refundAmount = AlipayValidationUtils.requirePositiveAmount(
                ValidationUtils.requireNonNull(builder.refundAmount,
                        "refundAmount is required"), 999_999_999L, "refundAmount");
        outItemId = AlipayValidationUtils.requireOptionalText(
                builder.outItemId, "outItemId", 64);
        outSkuId = AlipayValidationUtils.requireOptionalText(
                builder.outSkuId, "outSkuId", 64);
        outCertificateNos = List.copyOf(builder.outCertificateNos);
    }

    /**
     * 创建退款商品明细构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取商品编号。
     *
     * @return 商品编号
     */
    public String goodsId() {
        return goodsId;
    }

    /**
     * 获取商品退款金额。
     *
     * @return 商品退款金额，单位为分
     */
    public long refundAmount() {
        return refundAmount;
    }

    /**
     * 获取商家小程序商品 ID。
     *
     * @return 商家小程序商品 ID；没有时为 {@code null}
     */
    public @Nullable String outItemId() {
        return outItemId;
    }

    /**
     * 获取商家小程序 SKU ID。
     *
     * @return 商家小程序 SKU ID；没有时为 {@code null}
     */
    public @Nullable String outSkuId() {
        return outSkuId;
    }

    /**
     * 获取外部凭证编号。
     *
     * @return 不可变外部凭证编号列表
     */
    public List<String> outCertificateNos() {
        return outCertificateNos;
    }

    /** 退款商品 fluent 构建器。 */
    public static final class Builder {
        private @Nullable String goodsId;
        private @Nullable Long refundAmount;
        private @Nullable String outItemId;
        private @Nullable String outSkuId;
        private final List<String> outCertificateNos = new ArrayList<>();

        private Builder() {
        }

        /**
         * 配置商品编号。
         *
         * @param value 商品编号
         * @return 当前构建器
         */
        public Builder goodsId(String value) {
            goodsId = value;
            return this;
        }

        /**
         * 配置商品退款金额。
         *
         * @param value 商品退款金额，单位为分
         * @return 当前构建器
         */
        public Builder refundAmount(long value) {
            refundAmount = value;
            return this;
        }

        /**
         * 配置商家小程序商品 ID。
         *
         * @param value 商家小程序商品 ID
         * @return 当前构建器
         */
        public Builder outItemId(String value) {
            outItemId = value;
            return this;
        }

        /**
         * 配置商家小程序 SKU ID。
         *
         * @param value 商家小程序 SKU ID
         * @return 当前构建器
         */
        public Builder outSkuId(String value) {
            outSkuId = value;
            return this;
        }

        /**
         * 添加外部凭证编号。
         *
         * @param value 外部凭证编号
         * @return 当前构建器
         */
        public Builder addOutCertificateNo(String value) {
            outCertificateNos.add(AlipayValidationUtils.requireIdentifier(
                    value, "outCertificateNo", 128));
            return this;
        }

        /**
         * 校验参数并构建不可变退款商品明细。
         *
         * @return 不可变退款商品明细
         */
        public RefundGoodsDetail build() {
            return new RefundGoodsDetail(this);
        }
    }
}
