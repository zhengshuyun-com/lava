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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.wechat.internal.WechatPayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 微信支付退款单，供退款申请和查询接口复用。
 *
 * @param refundId 微信支付退款单号
 * @param outRefundNo 商户退款单号
 * @param transactionId 微信支付订单号
 * @param outTradeNo 商户订单号
 * @param channel 退款渠道原始值
 * @param userReceivedAccount 退款入账账户
 * @param successTime 退款成功时间
 * @param createTime 退款创建时间
 * @param status 退款状态原始值
 * @param fundsAccount 退款资金账户原始值
 * @param amount 退款金额明细
 * @param promotionDetail 优惠退款详情
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Refund(
        @JsonProperty("refund_id") String refundId,
        @JsonProperty("out_refund_no") String outRefundNo,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("out_trade_no") String outTradeNo,
        @JsonProperty("channel") String channel,
        @JsonProperty("user_received_account") String userReceivedAccount,
        @JsonProperty("success_time") @Nullable OffsetDateTime successTime,
        @JsonProperty("create_time") OffsetDateTime createTime,
        @JsonProperty("status") String status,
        @JsonProperty("funds_account") String fundsAccount,
        @JsonProperty("amount") Amount amount,
        @JsonProperty("promotion_detail") @Nullable List<PromotionDetail> promotionDetail) {

    /**
     * 校验退款单必填字段并复制优惠列表。
     */
    public Refund {
        ValidationUtils.requireNotBlank(refundId, "refundId must not be blank");
        ValidationUtils.requireNotBlank(outRefundNo, "outRefundNo must not be blank");
        ValidationUtils.requireNotBlank(transactionId, "transactionId must not be blank");
        ValidationUtils.requireNotBlank(outTradeNo, "outTradeNo must not be blank");
        ValidationUtils.requireNotBlank(channel, "channel must not be blank");
        ValidationUtils.requireNonNull(userReceivedAccount,
                "userReceivedAccount must not be null");
        ValidationUtils.requireNonNull(createTime, "createTime must not be null");
        ValidationUtils.requireNotBlank(status, "status must not be blank");
        ValidationUtils.requireNotBlank(fundsAccount, "fundsAccount must not be blank");
        ValidationUtils.requireNonNull(amount, "amount must not be null");
        if (promotionDetail != null) {
            promotionDetail = List.copyOf(promotionDetail);
        }
    }

    /**
     * 退款金额明细。
     *
     * @param total 原订单金额
     * @param refund 退款金额
     * @param from 退款出资账户列表
     * @param payerTotal 用户实际支付金额
     * @param payerRefund 用户实际退款金额
     * @param settlementRefund 应结退款金额
     * @param settlementTotal 应结订单金额
     * @param discountRefund 优惠退款金额
     * @param currency 退款币种
     * @param refundFee 手续费退款金额
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(
            @JsonProperty("total") long total,
            @JsonProperty("refund") long refund,
            @JsonProperty("from") @Nullable List<AmountFrom> from,
            @JsonProperty("payer_total") long payerTotal,
            @JsonProperty("payer_refund") long payerRefund,
            @JsonProperty("settlement_refund") long settlementRefund,
            @JsonProperty("settlement_total") long settlementTotal,
            @JsonProperty("discount_refund") long discountRefund,
            @JsonProperty("currency") String currency,
            @JsonProperty("refund_fee") @Nullable Long refundFee) {
        /**
         * 复制退款出资列表。
         */
        public Amount {
            WechatPayValidationUtils.requirePositive(total, "amount.total");
            WechatPayValidationUtils.requirePositive(refund, "amount.refund");
            ValidationUtils.requireTrue(refund <= total,
                    "amount.refund must not exceed amount.total");
            WechatPayValidationUtils.requireNonNegative(payerTotal,
                    "amount.payerTotal");
            WechatPayValidationUtils.requireNonNegative(payerRefund,
                    "amount.payerRefund");
            WechatPayValidationUtils.requireNonNegative(settlementRefund,
                    "amount.settlementRefund");
            WechatPayValidationUtils.requireNonNegative(settlementTotal,
                    "amount.settlementTotal");
            WechatPayValidationUtils.requireNonNegative(discountRefund,
                    "amount.discountRefund");
            ValidationUtils.requireTrue("CNY".equals(currency),
                    "amount.currency must be CNY");
            if (refundFee != null) {
                WechatPayValidationUtils.requireNonNegative(refundFee,
                        "amount.refundFee");
            }
            if (from != null) {
                from = List.copyOf(from);
            }
        }
    }

    /**
     * 退款出资账户。
     *
     * @param account 账户类型原始值
     * @param amount 出资金额
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AmountFrom(
            @JsonProperty("account") String account,
            @JsonProperty("amount") long amount) {
        /**
         * 校验退款出资账户。
         */
        public AmountFrom {
            ValidationUtils.requireNotBlank(account,
                    "amountFrom.account must not be blank");
            WechatPayValidationUtils.requirePositive(amount, "amountFrom.amount");
        }
    }

    /**
     * 优惠退款详情。
     *
     * @param promotionId 优惠 ID
     * @param scope 优惠范围原始值
     * @param type 优惠类型原始值
     * @param amount 优惠金额
     * @param refundAmount 优惠退款金额
     * @param goodsDetail 优惠商品退款详情
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromotionDetail(
            @JsonProperty("promotion_id") String promotionId,
            @JsonProperty("scope") String scope,
            @JsonProperty("type") String type,
            @JsonProperty("amount") long amount,
            @JsonProperty("refund_amount") long refundAmount,
            @JsonProperty("goods_detail") @Nullable List<GoodsDetail> goodsDetail) {
        /**
         * 复制优惠退款商品列表。
         */
        public PromotionDetail {
            ValidationUtils.requireNotBlank(promotionId,
                    "promotionDetail.promotionId must not be blank");
            ValidationUtils.requireNotBlank(scope,
                    "promotionDetail.scope must not be blank");
            ValidationUtils.requireNotBlank(type,
                    "promotionDetail.type must not be blank");
            WechatPayValidationUtils.requireNonNegative(amount,
                    "promotionDetail.amount");
            WechatPayValidationUtils.requireNonNegative(refundAmount,
                    "promotionDetail.refundAmount");
            if (goodsDetail != null) {
                goodsDetail = List.copyOf(goodsDetail);
            }
        }
    }

    /**
     * 优惠退款商品详情。
     *
     * @param merchantGoodsId 商户侧商品编码
     * @param wechatpayGoodsId 微信支付商品编码
     * @param goodsName 商品名称
     * @param unitPrice 商品单价
     * @param refundAmount 商品退款金额
     * @param refundQuantity 商品退货数量
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoodsDetail(
            @JsonProperty("merchant_goods_id") String merchantGoodsId,
            @JsonProperty("wechatpay_goods_id") @Nullable String wechatpayGoodsId,
            @JsonProperty("goods_name") @Nullable String goodsName,
            @JsonProperty("unit_price") long unitPrice,
            @JsonProperty("refund_amount") long refundAmount,
            @JsonProperty("refund_quantity") long refundQuantity) {
        /**
         * 校验优惠退款商品详情。
         */
        public GoodsDetail {
            WechatPayValidationUtils.requireMerchantGoodsId(merchantGoodsId);
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
