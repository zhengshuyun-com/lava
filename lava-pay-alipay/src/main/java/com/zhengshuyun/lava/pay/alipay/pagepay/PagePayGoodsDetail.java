/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.pagepay;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayPayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * 电脑网站支付订单中的单个商品明细。
 */
public final class PagePayGoodsDetail {
    private final String goodsId;
    private final String goodsName;
    private final long quantity;
    private final long price;
    private final @Nullable String alipayGoodsId;
    private final @Nullable String goodsCategory;
    private final @Nullable String categoriesTree;
    private final @Nullable String body;
    private final @Nullable URI showUrl;

    private PagePayGoodsDetail(Builder builder) {
        goodsId = AlipayPayValidationUtils.requireIdentifier(builder.goodsId,
                "goodsId", 64);
        goodsName = AlipayPayValidationUtils.requireText(builder.goodsName,
                "goodsName", 1, 256);
        quantity = ValidationUtils.requireNonNull(builder.quantity, "quantity is required");
        ValidationUtils.requireTrue(quantity > 0, "quantity must be positive");
        price = AlipayPayValidationUtils.requirePositiveAmount(
                ValidationUtils.requireNonNull(builder.price, "price is required"),
                999_999_999L, "price");
        alipayGoodsId = AlipayPayValidationUtils.requireOptionalText(
                builder.alipayGoodsId, "alipayGoodsId", 32);
        goodsCategory = AlipayPayValidationUtils.requireOptionalText(
                builder.goodsCategory, "goodsCategory", 24);
        categoriesTree = AlipayPayValidationUtils.requireOptionalText(
                builder.categoriesTree, "categoriesTree", 128);
        body = AlipayPayValidationUtils.requireOptionalText(builder.body, "body", 400);
        showUrl = builder.showUrl;
        if (showUrl != null) {
            ValidationUtils.requireTrue(showUrl.isAbsolute()
                            && ("http".equalsIgnoreCase(showUrl.getScheme())
                            || "https".equalsIgnoreCase(showUrl.getScheme()))
                            && showUrl.getUserInfo() == null && showUrl.getRawFragment() == null,
                    "showUrl must be an absolute HTTP or HTTPS URI");
            ValidationUtils.requireTrue(showUrl.toASCIIString().length() <= 400,
                    "showUrl must not exceed 400 characters");
        }
    }

    /**
     * 创建商品明细构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取商户商品编号。
     *
     * @return 商户商品编号
     */
    public String goodsId() {
        return goodsId;
    }

    /**
     * 获取商品名称。
     *
     * @return 商品名称
     */
    public String goodsName() {
        return goodsName;
    }

    /**
     * 获取商品数量。
     *
     * @return 商品数量
     */
    public long quantity() {
        return quantity;
    }

    /**
     * 获取商品单价。
     *
     * @return 商品单价，单位为分
     */
    public long price() {
        return price;
    }

    /**
     * 获取支付宝商品编号。
     *
     * @return 支付宝商品编号；没有时为 {@code null}
     */
    public @Nullable String alipayGoodsId() {
        return alipayGoodsId;
    }

    /**
     * 获取商品类目。
     *
     * @return 商品类目；没有时为 {@code null}
     */
    public @Nullable String goodsCategory() {
        return goodsCategory;
    }

    /**
     * 获取商品类目树。
     *
     * @return 商品类目树；没有时为 {@code null}
     */
    public @Nullable String categoriesTree() {
        return categoriesTree;
    }

    /**
     * 获取商品描述。
     *
     * @return 商品描述；没有时为 {@code null}
     */
    public @Nullable String body() {
        return body;
    }

    /**
     * 获取商品展示地址。
     *
     * @return 商品展示地址；没有时为 {@code null}
     */
    public @Nullable URI showUrl() {
        return showUrl;
    }

    /**
     * 商品明细 fluent 构建器。
     */
    public static final class Builder {
        private @Nullable String goodsId;
        private @Nullable String goodsName;
        private @Nullable Long quantity;
        private @Nullable Long price;
        private @Nullable String alipayGoodsId;
        private @Nullable String goodsCategory;
        private @Nullable String categoriesTree;
        private @Nullable String body;
        private @Nullable URI showUrl;

        private Builder() {
        }

        /**
         * 配置商户商品编号。
         *
         * @param value 商户商品编号
         * @return 当前构建器
         */
        public Builder goodsId(String value) {
            goodsId = value;
            return this;
        }

        /**
         * 配置商品名称。
         *
         * @param value 商品名称
         * @return 当前构建器
         */
        public Builder goodsName(String value) {
            goodsName = value;
            return this;
        }

        /**
         * 配置商品数量。
         *
         * @param value 商品数量
         * @return 当前构建器
         */
        public Builder quantity(long value) {
            quantity = value;
            return this;
        }

        /**
         * 配置商品单价。
         *
         * @param value 商品单价，单位为分
         * @return 当前构建器
         */
        public Builder price(long value) {
            price = value;
            return this;
        }

        /**
         * 配置支付宝商品编号。
         *
         * @param value 支付宝商品编号
         * @return 当前构建器
         */
        public Builder alipayGoodsId(String value) {
            alipayGoodsId = value;
            return this;
        }

        /**
         * 配置商品类目。
         *
         * @param value 商品类目
         * @return 当前构建器
         */
        public Builder goodsCategory(String value) {
            goodsCategory = value;
            return this;
        }

        /**
         * 配置以竖线分隔的商品类目树。
         *
         * @param value 以竖线分隔的商品类目树
         * @return 当前构建器
         */
        public Builder categoriesTree(String value) {
            categoriesTree = value;
            return this;
        }

        /**
         * 配置商品描述。
         *
         * @param value 商品描述
         * @return 当前构建器
         */
        public Builder body(String value) {
            body = value;
            return this;
        }

        /**
         * 配置商品展示地址。
         *
         * @param value 商品展示地址
         * @return 当前构建器
         */
        public Builder showUrl(URI value) {
            showUrl = value;
            return this;
        }

        /**
         * 校验参数并构建不可变商品明细。
         *
         * @return 不可变商品明细
         */
        public PagePayGoodsDetail build() {
            return new PagePayGoodsDetail(this);
        }
    }
}
