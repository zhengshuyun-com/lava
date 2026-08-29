/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.zhengshuyun.lava.http.*;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.json.JsonException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayProtocolException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayTransportException;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝 OpenAPI 公钥模式的统一参数签名、表单生成、发送和响应验签层。
 */
public final class AlipayTransport {
    private static final String VERSION = "1.0";
    private static final String FORMAT = "json";
    private static final String CHARSET = "UTF-8";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final String appId;
    private final PrivateKey appPrivateKey;
    private final HttpClient httpClient;
    private final URI gatewayUrl;
    private final Clock clock;
    private final JsonCodec jsonCodec;
    private final AlipayResponseParser responseParser;

    /**
     * 创建内部传输层。调用方负责在构造前完成配置校验。
     *
     * @param appId          应用 ID
     * @param appPrivateKey  应用私钥
     * @param alipayPublicKey 支付宝公钥
     * @param httpClient     HTTP 客户端
     * @param gatewayUrl     网关地址
     * @param clock          协议时钟
     * @param jsonCodec      JSON 编解码器
     */
    public AlipayTransport(
            String appId,
            PrivateKey appPrivateKey,
            PublicKey alipayPublicKey,
            HttpClient httpClient,
            URI gatewayUrl,
            Clock clock,
            JsonCodec jsonCodec
    ) {
        this.appId = appId;
        this.appPrivateKey = appPrivateKey;
        this.httpClient = httpClient;
        this.gatewayUrl = gatewayUrl;
        this.clock = clock;
        this.jsonCodec = jsonCodec;
        responseParser = new AlipayResponseParser(alipayPublicKey, jsonCodec);
    }

    /**
     * 获取当前传输层绑定的应用 ID。
     *
     * @return 当前客户端应用 ID
     */
    public String appId() {
        return appId;
    }

    /**
     * 返回支付宝协议时区下的当前本地时间。
     *
     * @return GMT+8 当前时间
     */
    public LocalDateTime currentDateTime() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.ofHours(8));
    }

    /**
     * 生成页面跳转类 API 的签名 POST 表单，不向支付宝发送 HTTP 请求。
     *
     * @param method     接口名称
     * @param bizRequest 业务参数
     * @param notifyUrl  异步通知地址
     * @param returnUrl  同步返回地址
     * @return 可直接作为 HTML 响应输出的自动提交表单
     */
    public String pageForm(
            String method,
            Object bizRequest,
            URI notifyUrl,
            URI returnUrl
    ) {
        String bizContent = encode(bizRequest);
        Map<String, String> signed = signedParameters(
                method,
                bizContent,
                notifyUrl,
                returnUrl
        );
        Map<String, String> query = new LinkedHashMap<>(signed);
        query.remove("biz_content");

        HttpUrlBuilder url = HttpUrlBuilder.from(gatewayUrl);
        query.forEach(url::queryParam);
        String action = htmlEscape(url.build().toASCIIString());
        String hiddenValue = htmlEscape(bizContent);
        return "<form name=\"punchout_form\" method=\"post\" action=\"" + action + "\">\n"
                + "<input type=\"hidden\" name=\"biz_content\" value=\"" + hiddenValue
                + "\">\n<input type=\"submit\" value=\"立即支付\" style=\"display:none\">\n"
                + "</form>\n<script>document.forms[0].submit();</script>";
    }

    /**
     * 调用服务端 OpenAPI，并在解析前验证响应业务节点签名。
     *
     * @param method       接口名称
     * @param bizRequest   业务参数
     * @param responseType 响应模型
     * @param <T>          响应模型类型
     * @return 已验签业务响应
     */
    public <T> T execute(String method, Object bizRequest, Class<T> responseType) {
        String bizContent = encode(bizRequest);
        Map<String, String> signed = signedParameters(
                method,
                bizContent,
                null,
                null
        );
        Map<String, String> query = new LinkedHashMap<>(signed);
        query.remove("biz_content");

        HttpRequest request = HttpRequest.builder(gatewayUrl.toASCIIString(), HttpMethod.POST)
                .addQueryParams(query)
                .header(HttpHeaderNames.ACCEPT, HttpMediaTypes.APPLICATION_JSON)
                .userAgent("lava-pay-alipay")
                .formBody(Map.of("biz_content", bizContent))
                .build();
        HttpResponse response;
        try {
            response = httpClient.send(request);
        } catch (HttpException exception) {
            throw new AlipayTransportException(
                    exception.getKind(),
                    exception.getMethod(),
                    exception.getUrl(),
                    exception.getTransportCauseType()
            );
        }
        if (!response.isSuccessful()) {
            throw new AlipayTransportException(response.statusCode());
        }
        return responseParser.parse(
                method,
                response.getBodyAsBytes(),
                responseType,
                response.getHeaders().get("trace_id")
        );
    }

    private Map<String, String> signedParameters(
            String method,
            String bizContent,
            @Nullable URI notifyUrl,
            @Nullable URI returnUrl
    ) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", appId);
        params.put("biz_content", bizContent);
        params.put("charset", CHARSET);
        params.put("format", FORMAT);
        params.put("method", method);
        if (notifyUrl != null) {
            params.put("notify_url", notifyUrl.toASCIIString());
        }
        if (returnUrl != null) {
            params.put("return_url", returnUrl.toASCIIString());
        }
        params.put("sign_type", AlipayCryptoUtils.SIGN_TYPE);
        params.put("timestamp", TIMESTAMP.format(
                clock.instant().atZone(ZoneOffset.ofHours(8))));
        params.put("version", VERSION);
        params.put("sign", AlipayCryptoUtils.sign(params, appPrivateKey));
        return params;
    }

    private String encode(Object value) {
        try {
            return jsonCodec.write(value);
        } catch (JsonException exception) {
            throw new AlipayProtocolException("无法编码支付宝请求 JSON");
        }
    }

    private static String htmlEscape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
