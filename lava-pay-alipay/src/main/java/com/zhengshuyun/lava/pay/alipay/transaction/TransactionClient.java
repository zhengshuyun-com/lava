/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 支付宝统一收单交易查询与关闭客户端。
 */
public final class TransactionClient {
    private static final String QUERY_METHOD = "alipay.trade.query";
    private static final String CLOSE_METHOD = "alipay.trade.close";

    private final AlipayPayRuntime runtime;

    /**
     * 由根客户端创建交易入口。
     *
     * @param runtime 共享运行时
     */
    public TransactionClient(AlipayPayRuntime runtime) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
    }

    /**
     * 按商户订单号查询交易。
     *
     * @param outTradeNo 商户订单号
     * @return 已验签交易状态
     */
    public Trade queryByOutTradeNo(String outTradeNo) {
        return query(TradeQueryRequest.builder().outTradeNo(outTradeNo).build());
    }

    /**
     * 按支付宝交易号查询交易。
     *
     * @param tradeNo 支付宝交易号
     * @return 已验签交易状态
     */
    public Trade queryByTradeNo(String tradeNo) {
        return query(TradeQueryRequest.builder().tradeNo(tradeNo).build());
    }

    /**
     * 使用完整参数查询交易。
     *
     * @param request 查询参数
     * @return 已验签交易状态
     */
    public Trade query(TradeQueryRequest request) {
        AlipayPayTransport transport = runtime.transport();
        ValidationUtils.requireNonNull(request, "request must not be null");
        TradePayload response = transport.execute(QUERY_METHOD,
                new QueryPayload(request.outTradeNo(), request.tradeNo(),
                        request.queryOptions().isEmpty() ? null : request.queryOptions()),
                TradePayload.class);

        String outTradeNo = AlipayPayValidationUtils.requireResponseText(
                response.outTradeNo, "out_trade_no");
        String tradeState = AlipayPayValidationUtils.requireResponseText(
                response.tradeState, "trade_status");
        if (request.outTradeNo() != null) {
            AlipayPayValidationUtils.requireSame(request.outTradeNo(), outTradeNo);
        } else {
            AlipayPayValidationUtils.requireSame(request.tradeNo(), response.tradeNo);
        }
        List<Trade.FundBill> fundBills = response.fundBills == null ? List.of()
                : response.fundBills.stream().map(TransactionClient::toFundBill).toList();
        return new Trade(response.tradeNo, outTradeNo, tradeState,
                AlipayPayMoneyUtils.parse(response.totalAmount, "total_amount"),
                response.buyerOpenId, response.buyerUserId, response.buyerLogonId,
                AlipayPayDateTimeUtils.parseOptional(response.sendPayDate, "send_pay_date"),
                optionalMoney(response.buyerPayAmount, "buyer_pay_amount"),
                optionalMoney(response.receiptAmount, "receipt_amount"),
                optionalMoney(response.invoiceAmount, "invoice_amount"),
                optionalMoney(response.pointAmount, "point_amount"),
                response.storeId, fundBills);
    }

    /**
     * 按商户订单号关闭待支付交易。
     *
     * @param outTradeNo 商户订单号
     * @return 已验签关闭结果
     */
    public TradeCloseResult closeByOutTradeNo(String outTradeNo) {
        return close(TradeCloseRequest.builder().outTradeNo(outTradeNo).build());
    }

    /**
     * 按支付宝交易号关闭待支付交易。
     *
     * @param tradeNo 支付宝交易号
     * @return 已验签关闭结果
     */
    public TradeCloseResult closeByTradeNo(String tradeNo) {
        return close(TradeCloseRequest.builder().tradeNo(tradeNo).build());
    }

    /**
     * 使用完整参数关闭待支付交易。
     *
     * @param request 关闭参数
     * @return 已验签关闭结果
     */
    public TradeCloseResult close(TradeCloseRequest request) {
        AlipayPayTransport transport = runtime.transport();
        ValidationUtils.requireNonNull(request, "request must not be null");
        ClosePayload response = transport.execute(CLOSE_METHOD,
                new CloseRequestPayload(request.outTradeNo(), request.tradeNo(),
                        request.operatorId()), ClosePayload.class);
        if (request.outTradeNo() != null) {
            AlipayPayValidationUtils.requireSame(request.outTradeNo(), response.outTradeNo);
        } else {
            AlipayPayValidationUtils.requireSame(request.tradeNo(), response.tradeNo);
        }
        return new TradeCloseResult(response.tradeNo, response.outTradeNo);
    }

    private static Trade.FundBill toFundBill(FundBillPayload value) {
        return new Trade.FundBill(
                AlipayPayValidationUtils.requireResponseText(
                        value.fundChannel, "fund_channel"),
                AlipayPayMoneyUtils.parse(value.amount, "fund_bill_list.amount"),
                optionalMoney(value.realAmount, "fund_bill_list.real_amount"));
    }

    private static @Nullable Long optionalMoney(@Nullable String value, String name) {
        return value == null ? null : AlipayPayMoneyUtils.parse(value, name);
    }

    private record QueryPayload(
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("query_options") @Nullable List<String> queryOptions) {
    }

    private record CloseRequestPayload(
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("operator_id") @Nullable String operatorId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TradePayload(
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("out_trade_no") @Nullable String outTradeNo,
            @JsonProperty("trade_status") @Nullable String tradeState,
            @JsonProperty("total_amount") @Nullable String totalAmount,
            @JsonProperty("buyer_open_id") @Nullable String buyerOpenId,
            @JsonProperty("buyer_user_id") @Nullable String buyerUserId,
            @JsonProperty("buyer_logon_id") @Nullable String buyerLogonId,
            @JsonProperty("send_pay_date") @Nullable String sendPayDate,
            @JsonProperty("buyer_pay_amount") @Nullable String buyerPayAmount,
            @JsonProperty("receipt_amount") @Nullable String receiptAmount,
            @JsonProperty("invoice_amount") @Nullable String invoiceAmount,
            @JsonProperty("point_amount") @Nullable String pointAmount,
            @JsonProperty("store_id") @Nullable String storeId,
            @JsonProperty("fund_bill_list") @Nullable List<FundBillPayload> fundBills) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FundBillPayload(
            @JsonProperty("fund_channel") @Nullable String fundChannel,
            @JsonProperty("amount") @Nullable String amount,
            @JsonProperty("real_amount") @Nullable String realAmount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClosePayload(
            @JsonProperty("trade_no") @Nullable String tradeNo,
            @JsonProperty("out_trade_no") @Nullable String outTradeNo) {
    }
}
