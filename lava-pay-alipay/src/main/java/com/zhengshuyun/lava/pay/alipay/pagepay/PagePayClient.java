/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.pagepay;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayMoneyUtils;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayRuntime;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayTransport;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayValidationUtils;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 固定绑定异步通知与同步返回地址的电脑网站支付入口。
 */
public final class PagePayClient {
    private static final String METHOD = "alipay.trade.page.pay";
    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";
    private static final String INTEGRATION_TYPE = "PCWEB";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss");

    private final AlipayRuntime runtime;
    private final URI notifyUrl;
    private final URI returnUrl;

    /**
     * 由根客户端创建页面支付入口。
     *
     * @param runtime   共享运行时
     * @param notifyUrl 异步通知地址
     * @param returnUrl 同步返回地址
     */
    public PagePayClient(AlipayRuntime runtime, URI notifyUrl, URI returnUrl) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
        this.notifyUrl = AlipayValidationUtils.requireCallbackUrl(notifyUrl, "notifyUrl");
        this.returnUrl = AlipayValidationUtils.requireCallbackUrl(returnUrl, "returnUrl");
    }

    /**
     * 生成已签名的自动提交 POST 表单。
     *
     * @param request 单笔订单业务参数
     * @return 应作为 HTML 响应正文输出的表单
     */
    public PagePayForm createForm(PagePayRequest request) {
        AlipayTransport transport = runtime.transport();
        ValidationUtils.requireNonNull(request, "request must not be null");
        validateTimeExpire(transport, request.timeExpire());

        String timeExpire = request.timeExpire() == null ? null
                : DATE_TIME.format(request.timeExpire());
        String timeoutExpress = request.timeout() == null ? null
                : request.timeout().toMinutes() + "m";
        List<GoodsPayload> goods = request.goodsDetail().isEmpty() ? null
                : request.goodsDetail().stream().map(GoodsPayload::from).toList();
        PagePayPayload payload = new PagePayPayload(
                request.outTradeNo(),
                AlipayMoneyUtils.formatPositive(request.totalAmount()),
                request.subject(),
                PRODUCT_CODE,
                request.body(),
                timeExpire,
                timeoutExpress,
                request.qrPayMode(),
                request.qrcodeWidth(),
                goods,
                request.enablePayChannels().isEmpty() ? null
                        : String.join(",", request.enablePayChannels()),
                request.disablePayChannels().isEmpty() ? null
                        : String.join(",", request.disablePayChannels()),
                INTEGRATION_TYPE,
                request.storeId(),
                request.merchantOrderNo(),
                request.passbackParams() == null ? null
                        : URLEncoder.encode(request.passbackParams(), StandardCharsets.UTF_8)
        );
        return new PagePayForm(transport.pageForm(
                METHOD,
                payload,
                notifyUrl,
                returnUrl
        ));
    }

    /**
     * 获取当前入口绑定的异步通知地址。
     *
     * @return 固定异步通知地址
     */
    public URI notifyUrl() {
        return notifyUrl;
    }

    /**
     * 获取当前入口绑定的同步返回地址。
     *
     * @return 固定同步返回地址
     */
    public URI returnUrl() {
        return returnUrl;
    }

    private static void validateTimeExpire(AlipayTransport transport,
                                           @Nullable LocalDateTime timeExpire) {
        if (timeExpire == null) {
            return;
        }
        Duration remaining = Duration.between(transport.currentDateTime(), timeExpire);
        ValidationUtils.requireTrue(remaining.compareTo(Duration.ofMinutes(1)) >= 0
                        && remaining.compareTo(Duration.ofDays(15)) <= 0,
                "timeExpire must be between 1 minute and 15 days from now");
    }

    private record PagePayPayload(
            @JsonProperty("out_trade_no") String outTradeNo,
            @JsonProperty("total_amount") String totalAmount,
            @JsonProperty("subject") String subject,
            @JsonProperty("product_code") String productCode,
            @JsonProperty("body") @Nullable String body,
            @JsonProperty("time_expire") @Nullable String timeExpire,
            @JsonProperty("timeout_express") @Nullable String timeoutExpress,
            @JsonProperty("qr_pay_mode") @Nullable String qrPayMode,
            @JsonProperty("qrcode_width") @Nullable Integer qrcodeWidth,
            @JsonProperty("goods_detail") @Nullable List<GoodsPayload> goodsDetail,
            @JsonProperty("enable_pay_channels") @Nullable String enablePayChannels,
            @JsonProperty("disable_pay_channels") @Nullable String disablePayChannels,
            @JsonProperty("integration_type") String integrationType,
            @JsonProperty("store_id") @Nullable String storeId,
            @JsonProperty("merchant_order_no") @Nullable String merchantOrderNo,
            @JsonProperty("passback_params") @Nullable String passbackParams
    ) {
    }

    private record GoodsPayload(
            @JsonProperty("goods_id") String goodsId,
            @JsonProperty("goods_name") String goodsName,
            @JsonProperty("quantity") long quantity,
            @JsonProperty("price") String price,
            @JsonProperty("alipay_goods_id") @Nullable String alipayGoodsId,
            @JsonProperty("goods_category") @Nullable String goodsCategory,
            @JsonProperty("categories_tree") @Nullable String categoriesTree,
            @JsonProperty("body") @Nullable String body,
            @JsonProperty("show_url") @Nullable String showUrl
    ) {
        private static GoodsPayload from(PagePayGoodsDetail value) {
            return new GoodsPayload(
                    value.goodsId(),
                    value.goodsName(),
                    value.quantity(),
                    AlipayMoneyUtils.formatPositive(value.price()),
                    value.alipayGoodsId(),
                    value.goodsCategory(),
                    value.categoriesTree(),
                    value.body(),
                    value.showUrl() == null ? null : value.showUrl().toASCIIString()
            );
        }
    }
}
