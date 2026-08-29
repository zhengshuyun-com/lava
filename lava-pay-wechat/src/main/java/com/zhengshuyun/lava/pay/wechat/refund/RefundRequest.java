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

package com.zhengshuyun.lava.pay.wechat.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 普通支付退款申请。
 */
public final class RefundRequest {
    private final @Nullable String transactionId;
    private final @Nullable String outTradeNo;
    private final String outRefundNo;
    private final @Nullable String reason;
    private final @Nullable URI notifyUrl;
    private final @Nullable String fundsAccount;
    private final Amount amount;
    private final @Nullable List<GoodsDetail> goodsDetail;

    private RefundRequest(Builder builder) {
        boolean hasTransactionId = builder.transactionId != null;
        boolean hasOutTradeNo = builder.outTradeNo != null;
        ValidationUtils.requireTrue(hasTransactionId != hasOutTradeNo,
                "exactly one of transactionId and outTradeNo is required");
        transactionId = builder.transactionId;
        outTradeNo = builder.outTradeNo;
        outRefundNo = WechatPayValidationUtils.requireOutRefundNo(
                ValidationUtils.requireNonNull(builder.outRefundNo,
                        "outRefundNo is required"));
        reason = builder.reason;
        notifyUrl = builder.notifyUrl;
        fundsAccount = builder.fundsAccount;

        long refund = WechatPayValidationUtils.requirePositive(
                ValidationUtils.requireNonNull(builder.refund, "refund amount is required"),
                "refund");
        long total = WechatPayValidationUtils.requirePositive(
                ValidationUtils.requireNonNull(builder.total, "total amount is required"),
                "total");
        ValidationUtils.requireTrue(refund <= total,
                "refund amount must not exceed total amount");

        List<AmountFrom> from = builder.amountFrom.isEmpty()
                ? null : List.copyOf(builder.amountFrom);
        amount = new Amount(refund, from, total, "CNY");
        goodsDetail = builder.goodsDetail.isEmpty()
                ? null : List.copyOf(builder.goodsDetail);
    }

    /**
     * 创建退款请求构建器。
     *
     * @return 新构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回微信支付订单号。
     *
     * @return 微信支付订单号；未使用时为 {@code null}
     */
    @JsonProperty("transaction_id")
    public @Nullable String transactionId() {
        return transactionId;
    }

    /**
     * 返回商户订单号。
     *
     * @return 商户订单号；未使用时为 {@code null}
     */
    @JsonProperty("out_trade_no")
    public @Nullable String outTradeNo() {
        return outTradeNo;
    }

    /**
     * 返回商户退款单号。
     *
     * @return 商户退款单号
     */
    @JsonProperty("out_refund_no")
    public String outRefundNo() {
        return outRefundNo;
    }

    /**
     * 返回退款原因。
     *
     * @return 退款原因；未配置时为 {@code null}
     */
    @JsonProperty("reason")
    public @Nullable String reason() {
        return reason;
    }

    /**
     * 返回退款通知地址。
     *
     * @return 退款通知地址；未配置时为 {@code null}
     */
    @JsonProperty("notify_url")
    public @Nullable String notifyUrl() {
        return notifyUrl == null ? null : notifyUrl.toASCIIString();
    }

    /**
     * 返回退款资金来源。
     *
     * @return 退款资金来源；未配置时为 {@code null}
     */
    @JsonProperty("funds_account")
    public @Nullable String fundsAccount() {
        return fundsAccount;
    }

    /**
     * 返回退款金额信息。
     *
     * @return 退款金额信息
     */
    @JsonProperty("amount")
    public Amount amount() {
        return amount;
    }

    /**
     * 返回退款商品列表。
     *
     * @return 退款商品列表；未配置时为 {@code null}
     */
    @JsonProperty("goods_detail")
    public @Nullable List<GoodsDetail> goodsDetail() {
        return goodsDetail;
    }

    /**
     * 退款请求构建器。
     */
    public static final class Builder {
        private @Nullable String transactionId;
        private @Nullable String outTradeNo;
        private @Nullable String outRefundNo;
        private @Nullable String reason;
        private @Nullable URI notifyUrl;
        private @Nullable String fundsAccount;
        private @Nullable Long refund;
        private @Nullable Long total;
        private final List<AmountFrom> amountFrom = new ArrayList<>();
        private final List<GoodsDetail> goodsDetail = new ArrayList<>();

        private Builder() {
        }

        /**
         * 配置微信支付订单号，并与商户订单号保持二选一。
         *
         * @param value 微信支付订单号
         * @return 当前构建器
         */
        public Builder transactionId(String value) {
            transactionId = WechatPayValidationUtils.requireId(value,
                    "transactionId", 32);
            return this;
        }

        /**
         * 配置商户订单号，并与微信支付订单号保持二选一。
         *
         * @param value 商户订单号
         * @return 当前构建器
         */
        public Builder outTradeNo(String value) {
            outTradeNo = WechatPayValidationUtils.requireOutTradeNo(value);
            return this;
        }

        /**
         * 配置商户退款单号。
         *
         * @param value 商户退款单号
         * @return 当前构建器
         */
        public Builder outRefundNo(String value) {
            outRefundNo = WechatPayValidationUtils.requireOutRefundNo(value);
            return this;
        }

        /**
         * 配置退款原因。
         *
         * @param value 退款原因，最多 80 个 UTF-8 字节
         * @return 当前构建器
         */
        public Builder reason(String value) {
            reason = WechatPayValidationUtils.requireOptionalBytes(value,
                    "reason", 80);
            return this;
        }

        /**
         * 配置退款结果通知地址。
         *
         * @param value 退款结果通知地址
         * @return 当前构建器
         */
        public Builder notifyUrl(URI value) {
            notifyUrl = WechatPayValidationUtils.requireNotifyUrl(value, 256);
            return this;
        }

        /**
         * 使用字符串配置退款结果通知地址。
         *
         * @param value 退款结果通知地址
         * @return 当前构建器
         */
        public Builder notifyUrl(String value) {
            notifyUrl = WechatPayValidationUtils.requireNotifyUrl(value, 256);
            return this;
        }

        /**
         * 配置退款资金来源。
         *
         * @param value 仅支持 {@link RefundFundsAccount#AVAILABLE} 或
         *              {@link RefundFundsAccount#UNSETTLED}
         * @return 当前构建器
         */
        public Builder fundsAccount(String value) {
            ValidationUtils.requireTrue(RefundFundsAccount.AVAILABLE.equals(value)
                            || RefundFundsAccount.UNSETTLED.equals(value),
                    "fundsAccount must be AVAILABLE or UNSETTLED");
            fundsAccount = value;
            return this;
        }

        /**
         * 配置退款金额与原订单总金额，单位均为分。
         *
         * @param refundValue 本次退款金额
         * @param totalValue 原订单金额
         * @return 当前构建器
         */
        public Builder amount(long refundValue, long totalValue) {
            refund = WechatPayValidationUtils.requirePositive(refundValue, "refund");
            total = WechatPayValidationUtils.requirePositive(totalValue, "total");
            return this;
        }

        /**
         * 追加退款出资账户。
         *
         * @param value 待追加退款出资账户
         * @return 当前构建器
         */
        public Builder addAmountFrom(AmountFrom value) {
            amountFrom.add(ValidationUtils.requireNonNull(value,
                    "amountFrom must not be null"));
            return this;
        }

        /**
         * 追加退款商品。
         *
         * @param value 待追加退款商品
         * @return 当前构建器
         */
        public Builder addGoodsDetail(GoodsDetail value) {
            goodsDetail.add(ValidationUtils.requireNonNull(value,
                    "goodsDetail must not be null"));
            return this;
        }

        /**
         * 校验并创建退款请求。
         *
         * @return 不可变退款请求
         */
        public RefundRequest build() {
            return new RefundRequest(this);
        }
    }

    /**
     * 退款金额信息。
     *
     * @param refund 退款金额
     * @param from 退款出资账户
     * @param total 原订单金额
     * @param currency 固定为 CNY
     */
    public record Amount(
            @JsonProperty("refund") long refund,
            @JsonProperty("from") @Nullable List<AmountFrom> from,
            @JsonProperty("total") long total,
            @JsonProperty("currency") String currency) {
        /**
         * 校验退款金额、出资账户和币种。
         */
        public Amount {
            WechatPayValidationUtils.requirePositive(refund, "amount.refund");
            WechatPayValidationUtils.requirePositive(total, "amount.total");
            ValidationUtils.requireTrue(refund <= total,
                    "refund amount must not exceed total amount");
            ValidationUtils.requireTrue("CNY".equals(currency),
                    "amount.currency must be CNY");
            if (from != null) {
                ValidationUtils.requireNotEmpty(from,
                        "amount.from must contain at least one item");
                from = List.copyOf(from);
                Set<String> accounts = new HashSet<>();
                long sum = 0;
                for (AmountFrom item : from) {
                    ValidationUtils.requireTrue(accounts.add(item.account()),
                            "amountFrom account must not be repeated");
                    ValidationUtils.requireTrue(item.amount() <= refund - sum,
                            "amountFrom amounts must not exceed refund amount");
                    sum += item.amount();
                }
                ValidationUtils.requireTrue(sum == refund,
                        "amountFrom amounts must equal refund amount");
            }
        }
    }

    /**
     * 退款出资账户及金额。
     *
     * @param account 仅支持 AVAILABLE 或 UNAVAILABLE
     * @param amount 出资金额，单位为分
     */
    public record AmountFrom(
            @JsonProperty("account") String account,
            @JsonProperty("amount") long amount) {
        /**
         * 校验退款出资信息。
         */
        public AmountFrom {
            ValidationUtils.requireTrue(RefundFundsAccount.AVAILABLE.equals(account)
                            || RefundFundsAccount.UNAVAILABLE.equals(account),
                    "amountFrom.account must be AVAILABLE or UNAVAILABLE");
            WechatPayValidationUtils.requirePositive(amount, "amountFrom.amount");
        }
    }

    /**
     * 指定商品退款信息。
     *
     * @param merchantGoodsId 商户侧商品编码
     * @param wechatpayGoodsId 微信支付商品编码
     * @param goodsName 商品名称
     * @param unitPrice 商品单价
     * @param refundAmount 商品退款金额
     * @param refundQuantity 商品退货数量
     */
    public record GoodsDetail(
            @JsonProperty("merchant_goods_id") String merchantGoodsId,
            @JsonProperty("wechatpay_goods_id") @Nullable String wechatpayGoodsId,
            @JsonProperty("goods_name") @Nullable String goodsName,
            @JsonProperty("unit_price") long unitPrice,
            @JsonProperty("refund_amount") long refundAmount,
            @JsonProperty("refund_quantity") long refundQuantity) {

        /**
         * 校验指定商品退款信息。
         */
        public GoodsDetail {
            merchantGoodsId = WechatPayValidationUtils.requireMerchantGoodsId(
                    merchantGoodsId);
            if (wechatpayGoodsId != null) {
                WechatPayValidationUtils.requireText(wechatpayGoodsId,
                        "wechatpayGoodsId", 1, 32);
            }
            if (goodsName != null) {
                WechatPayValidationUtils.requireText(goodsName, "goodsName", 1, 256);
            }
            WechatPayValidationUtils.requirePositive(unitPrice, "unitPrice");
            WechatPayValidationUtils.requirePositive(refundAmount, "refundAmount");
            WechatPayValidationUtils.requirePositive(refundQuantity, "refundQuantity");
        }
    }
}
