/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.refund;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.HttpMethod;
import com.zhengshuyun.lava.pay.alipay.internal.*;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 支付宝 OpenAPI V3 统一收单退款申请与退款查询客户端。
 */
public final class RefundClient {
    private static final String APPLY_PATH = "/v3/alipay/trade/refund";
    private static final String QUERY_PATH = "/v3/alipay/trade/fastpay/refund/query";

    private final AlipayRuntime runtime;

    /**
     * 由根客户端创建退款入口。
     *
     * @param runtime 共享运行时
     */
    public RefundClient(AlipayRuntime runtime) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
    }

    /**
     * 发起全部或部分退款。
     *
     * @param request 退款参数
     * @return 已验签退款结果；应通过 {@link RefundResult#succeeded()} 判断是否明确成功
     */
    public RefundResult apply(RefundRequest request) {
        // 1. 将稳定退款请求号、金额和可选商品明细组装为官方 V3 退款载荷。
        AlipayTransport transport = runtime.transport();
        ValidationUtils.requireNonNull(request, "request must not be null");
        List<GoodsPayload> goods = request.goodsDetail().isEmpty() ? null
                : request.goodsDetail().stream().map(GoodsPayload::from).toList();
        ApplyPayload response = transport.execute(
                APPLY_PATH,
                HttpMethod.POST,
                new ApplyRequestPayload(
                        request.outTradeNo(),
                        request.tradeNo(),
                        AlipayMoneyUtils.formatPositive(request.refundAmount()),
                        request.reason(),
                        request.outRequestNo(),
                        goods,
                        request.queryOptions()
                ),
                Map.of(),
                ApplyPayload.class
        );

        // 2. 响应验签完成后，将支付宝交易号和商户订单号绑定到本次退款请求。
        String tradeNo = AlipayValidationUtils.requireResponseText(
                response.tradeNo, "trade_no");
        String outTradeNo = AlipayValidationUtils.requireResponseText(
                response.outTradeNo, "out_trade_no");
        requireRequestedTrade(
                request.outTradeNo(),
                request.tradeNo(),
                outTradeNo,
                tradeNo
        );
        // 3. 严格解析资金变化标志和金额明细；调用方只能通过 succeeded 判断明确成功。
        return new RefundResult(
                tradeNo,
                outTradeNo,
                response.fundChange,
                AlipayMoneyUtils.parse(response.refundFee, "refund_fee"),
                optionalMoney(response.sendBackFee, "send_back_fee"),
                response.buyerOpenId,
                response.buyerLogonId,
                toFundBills(response.fundBills)
        );
    }

    /**
     * 查询指定退款请求。
     *
     * @param request 退款查询参数
     * @return 已验签退款查询结果
     */
    public RefundQueryResult query(RefundQueryRequest request) {
        // 1. 使用原交易标识和稳定退款请求号构造 V3 退款查询载荷。
        AlipayTransport transport = runtime.transport();
        ValidationUtils.requireNonNull(request, "request must not be null");
        QueryPayload response = transport.execute(
                QUERY_PATH,
                HttpMethod.POST,
                new QueryRequestPayload(
                        request.outTradeNo(),
                        request.tradeNo(),
                        request.outRequestNo(),
                        request.queryOptions()
                ),
                Map.of(),
                QueryPayload.class
        );

        // 2. 将响应中的订单号和退款请求号与本次查询条件逐项绑定。
        if (response.outTradeNo != null && request.outTradeNo() != null) {
            AlipayValidationUtils.requireSame(request.outTradeNo(), response.outTradeNo);
        }
        if (response.tradeNo != null && request.tradeNo() != null) {
            AlipayValidationUtils.requireSame(request.tradeNo(), response.tradeNo);
        }
        if (response.outRequestNo != null) {
            AlipayValidationUtils.requireSame(request.outRequestNo(), response.outRequestNo);
        }
        // 3. 严格解析退款状态、金额、时间和冲退信息，再映射为公开不可变结果。
        return new RefundQueryResult(
                response.tradeNo,
                response.outTradeNo,
                response.outRequestNo,
                optionalMoney(response.totalAmount, "total_amount"),
                optionalMoney(response.refundAmount, "refund_amount"),
                response.refundStatus,
                AlipayDateTimeUtils.parseOptional(response.refundTime, "gmt_refund_pay"),
                optionalMoney(response.sendBackFee, "send_back_fee"),
                toDepositBackInfo(response.depositBackInfo),
                toFundBills(response.fundBills)
        );
    }

    /** 将退款响应中的双订单标识绑定到原请求。 */
    private static void requireRequestedTrade(
            @Nullable String requestedOutTradeNo,
            @Nullable String requestedTradeNo,
            String actualOutTradeNo,
            String actualTradeNo
    ) {
        if (requestedTradeNo != null) {
            AlipayValidationUtils.requireSame(requestedTradeNo, actualTradeNo);
        }
        if (requestedOutTradeNo != null) {
            AlipayValidationUtils.requireSame(requestedOutTradeNo, actualOutTradeNo);
        }
    }

    /** 将可选资金渠道载荷转换为不可变公开模型。 */
    private static List<RefundFundBill> toFundBills(
            @Nullable List<FundBillPayload> values) {
        return values == null ? List.of() : values.stream().map(value ->
                new RefundFundBill(
                        AlipayValidationUtils.requireResponseText(
                                value.fundChannel, "fund_channel"),
                        AlipayMoneyUtils.parse(value.amount, "fund_bill.amount"),
                        optionalMoney(value.realAmount, "fund_bill.real_amount"),
                        value.fundType
                )).toList();
    }

    /** 严格转换可选银行卡冲退信息。 */
    private static @Nullable DepositBackInfo toDepositBackInfo(
            @Nullable DepositBackPayload value) {
        if (value == null) {
            return null;
        }
        if (value.hasDepositBack != null
                && !"true".equalsIgnoreCase(value.hasDepositBack)
                && !"false".equalsIgnoreCase(value.hasDepositBack)) {
            throw new com.zhengshuyun.lava.pay.alipay.exception.AlipayProtocolException(
                    "支付宝银行卡冲退标识无效");
        }
        return new DepositBackInfo(
                Boolean.parseBoolean(value.hasDepositBack),
                value.status,
                optionalMoney(value.amount, "deposit_back_info.dback_amount"),
                AlipayDateTimeUtils.parseOptional(
                        value.bankAckTime, "deposit_back_info.bank_ack_time"),
                AlipayDateTimeUtils.parseOptional(
                        value.estimatedReceiptTime,
                        "deposit_back_info.est_bank_receipt_time")
        );
    }

    /** 解析可选支付宝金额。 */
    private static @Nullable Long optionalMoney(@Nullable String value, String name) {
        return value == null ? null : AlipayMoneyUtils.parse(value, name);
    }

    private record ApplyRequestPayload(
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("refund_amount") String refundAmount,
            @JsonProperty("refund_reason") @Nullable String reason,
            @JsonProperty("out_request_no") String outRequestNo,
            @JsonProperty("refund_goods_detail") @Nullable List<GoodsPayload> goodsDetail,
            @JsonProperty("query_options") List<String> queryOptions
    ) {
    }

    private record QueryRequestPayload(
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("out_request_no") String outRequestNo,
            @JsonProperty("query_options") List<String> queryOptions
    ) {
    }

    private record GoodsPayload(
            @JsonProperty("goods_id") String goodsId,
            @JsonProperty("refund_amount") String refundAmount,
            @JsonProperty("out_item_id") @Nullable String outItemId,
            @JsonProperty("out_sku_id") @Nullable String outSkuId,
            @JsonProperty("out_certificate_no_list") @Nullable List<String> certificateNos
    ) {
        /** 将公开退款商品明细映射为协议载荷。 */
        private static GoodsPayload from(RefundGoodsDetail value) {
            return new GoodsPayload(
                    value.goodsId(),
                    AlipayMoneyUtils.formatPositive(value.refundAmount()),
                    value.outItemId(),
                    value.outSkuId(),
                    value.outCertificateNos().isEmpty() ? null : value.outCertificateNos()
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ApplyPayload(
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("fund_change") @Nullable String fundChange,
            @JsonProperty("refund_fee") @Nullable String refundFee,
            @JsonProperty("send_back_fee") @Nullable String sendBackFee,
            @JsonProperty("buyer_open_id") @Nullable String buyerOpenId,
            @JsonProperty("buyer_logon_id") @Nullable String buyerLogonId,
            @JsonProperty("refund_detail_item_list") @Nullable List<FundBillPayload> fundBills
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QueryPayload(
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("out_request_no") @Nullable String outRequestNo,
            @JsonProperty("total_amount") @Nullable String totalAmount,
            @JsonProperty("refund_amount") @Nullable String refundAmount,
            @JsonProperty("refund_status") @Nullable String refundStatus,
            @JsonProperty("gmt_refund_pay") @Nullable String refundTime,
            @JsonProperty("send_back_fee") @Nullable String sendBackFee,
            @JsonProperty("deposit_back_info") @Nullable DepositBackPayload depositBackInfo,
            @JsonProperty("refund_detail_item_list") @Nullable List<FundBillPayload> fundBills
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FundBillPayload(
            @JsonProperty("fund_channel") @Nullable String fundChannel,
            @JsonProperty("amount") @Nullable String amount,
            @JsonProperty("real_amount") @Nullable String realAmount,
            @JsonProperty("fund_type") @Nullable String fundType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DepositBackPayload(
            @JsonProperty("has_deposit_back") @Nullable String hasDepositBack,
            @JsonProperty("dback_status") @Nullable String status,
            @JsonProperty("dback_amount") @Nullable String amount,
            @JsonProperty("bank_ack_time") @Nullable String bankAckTime,
            @JsonProperty("est_bank_receipt_time") @Nullable String estimatedReceiptTime
    ) {
    }
}
