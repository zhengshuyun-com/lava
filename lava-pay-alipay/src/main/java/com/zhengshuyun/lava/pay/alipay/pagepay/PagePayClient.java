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
 *
 * <p>支付宝当前未提供该页面支付接口的 REST V3 路径，本入口按官方 {@code pageExecute}
 * 语义生成 AOP 自动提交表单。</p>
 */
public final class PagePayClient {
    /** 电脑网站统一收单页面支付的固定 AOP 方法名。 */
    private static final String METHOD = "alipay.trade.page.pay";
    /** 电脑网站即时到账产品码，固定为 {@code FAST_INSTANT_TRADE_PAY}。 */
    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";
    /** 电脑网站支付接入类型，固定为 {@code PCWEB}。 */
    private static final String INTEGRATION_TYPE = "PCWEB";
    /** 页面支付绝对过期时间的固定格式，精确到秒。 */
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss");

    /** 根客户端共享运行时，用于检查关闭状态并取得表单生成器。 */
    private final AlipayRuntime runtime;
    /** 支付结果异步通知地址，随公共参数参与 AOP 签名。 */
    private final URI notifyUrl;
    /** 支付完成后的浏览器同步返回地址，不可作为支付成功依据。 */
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
        // 1. 确认根客户端仍可用，并按支付宝时区校验订单绝对过期时间。
        AlipayTransport transport = runtime.transport();
        ValidationUtils.requireNonNull(request, "request must not be null");
        validateTimeExpire(transport, request.timeExpire());

        // 2. 将公开业务模型转换为固定产品参数和 AOP biz_content 载荷。
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
        // 3. 由独立 AOP 表单生成器完成公共参数注入、RSA2 签名、HTML 转义和表单组装。
        return new PagePayForm(runtime.pagePayForms().create(
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

    /**
     * 校验绝对过期时间位于支付宝允许的时间窗口内。
     *
     * @param transport 共享传输层
     * @param timeExpire 可选过期时间
     */
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

    /**
     * 电脑网站支付 {@code biz_content} 的最终协议载荷。
     *
     * @param outTradeNo        商户订单号，最长 64 字符
     * @param totalAmount       订单金额，单位为元、固定保留两位小数
     * @param subject           订单标题，最长 256 字符
     * @param productCode       产品码，固定为 {@code FAST_INSTANT_TRADE_PAY}
     * @param body              可选订单描述，最长 400 字符
     * @param timeExpire        可选绝对过期时间，格式为 {@code yyyy-MM-dd HH:mm:ss}
     * @param timeoutExpress    可选相对有效期，以整分钟文本表示
     * @param qrPayMode         可选二维码展示模式
     * @param qrcodeWidth       自定义二维码宽度，仅模式 {@code 4} 有效
     * @param goodsDetail       可选商品明细；没有时为 {@code null}
     * @param enablePayChannels 可选允许渠道列表，逗号分隔
     * @param disablePayChannels 可选禁用渠道列表，逗号分隔
     * @param integrationType   页面集成类型，固定为 {@code PCWEB}
     * @param storeId           可选商户门店编号，最长 32 字符
     * @param merchantOrderNo   可选商户原始订单号，最长 32 字符
     * @param passbackParams    可选业务回传参数，已完成一次 URL 编码
     */
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

    /**
     * 页面支付载荷中的单个商品明细。
     *
     * @param goodsId       商户商品编号，最长 64 字符
     * @param goodsName     商品名称，最长 256 字符
     * @param quantity      商品数量，必须大于零
     * @param price         商品单价，单位为元、固定保留两位小数
     * @param alipayGoodsId 可选支付宝商品编号，最长 32 字符
     * @param goodsCategory 可选商品类目，最长 24 字符
     * @param categoriesTree 可选商品类目树，最长 128 字符
     * @param body          可选商品说明，最长 400 字符
     * @param showUrl       可选商品展示绝对地址，最长 400 字符
     */
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
        /**
         * 将已校验的公开商品模型映射为页面支付业务参数载荷。
         *
         * @param value 不可变商品明细，价格单位为分
         * @return 价格已转换为元、展示地址已转换为 ASCII 文本的协议载荷
         */
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
