/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.internal;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.http.HttpClient;
import com.zhengshuyun.lava.http.HttpException;
import com.zhengshuyun.lava.http.HttpHeaders;
import com.zhengshuyun.lava.http.HttpMethod;
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.HttpResponse;
import com.zhengshuyun.lava.http.HttpUrlBuilder;
import com.zhengshuyun.lava.http.OkHttpInterop;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.json.JsonException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayProtocolException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityException;
import com.zhengshuyun.lava.pay.alipay.exception.AlipaySecurityFailure;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayTransportException;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 支付宝公钥模式 OpenAPI V3 REST 传输层。
 */
public final class AlipayTransport {
    private static final String V3_AUTH_SCHEME = "ALIPAY-SHA256withRSA";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_REQUEST_ID = "alipay-request-id";
    private static final String HEADER_SIGNATURE = "alipay-signature";
    private static final String HEADER_TIMESTAMP = "alipay-timestamp";
    private static final String HEADER_NONCE = "alipay-nonce";
    private static final String HEADER_TRACE_ID = "alipay-trace-id";
    private static final String HEADER_TRACE_ID_COMPATIBLE = "alipay-traceid";
    /** 当前传输层绑定的应用 ID。 */
    private final String appId;
    /** 用于请求签名的应用私钥。 */
    private final PrivateKey appPrivateKey;
    /** 共享且已禁用隐式重试和重定向的 HTTP 客户端。 */
    private final HttpClient httpClient;
    /** 不包含接口路径、查询参数和片段的 OpenAPI 基础地址。 */
    private final URI baseUrl;
    /** 生成协议时间戳所使用的时钟。 */
    private final Clock clock;
    /** 请求与响应共用的 JSON 编解码器。 */
    private final JsonCodec jsonCodec;
    /** 执行 V3 响应头验签并解析业务 JSON 的解析器。 */
    private final AlipayResponseParser responseParser;

    /**
     * 创建内部传输层。调用方负责在构造前完成配置校验。
     *
     * @param appId           应用 ID
     * @param appPrivateKey   应用私钥
     * @param alipayPublicKey 支付宝公钥
     * @param httpClient      HTTP 客户端
     * @param baseUrl         OpenAPI 基础地址
     * @param clock           协议时钟
     * @param jsonCodec       JSON 编解码器
     */
    public AlipayTransport(
            String appId,
            PrivateKey appPrivateKey,
            PublicKey alipayPublicKey,
            HttpClient httpClient,
            URI baseUrl,
            Clock clock,
            JsonCodec jsonCodec
    ) {
        this.appId = AlipayValidationUtils.requireAppId(appId);
        this.appPrivateKey = AlipayKeyUtils.requirePrivateKey(appPrivateKey);
        PublicKey checkedPublicKey = AlipayKeyUtils.requirePublicKey(alipayPublicKey);
        this.httpClient = requireSafeHttpClient(httpClient);
        this.baseUrl = AlipayValidationUtils.requireBaseUrl(baseUrl);
        this.clock = ValidationUtils.requireNonNull(clock, "clock must not be null");
        this.jsonCodec = ValidationUtils.requireNonNull(
                jsonCodec,
                "jsonCodec must not be null"
        );
        responseParser = new AlipayResponseParser(checkedPublicKey, this.jsonCodec);
    }

    /**
     * 返回支付宝业务时区下的当前本地时间。
     *
     * @return GMT+8 当前时间
     */
    public LocalDateTime currentDateTime() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.ofHours(8));
    }

    /**
     * 调用支付宝 OpenAPI V3，并在反序列化前验证原始响应签名。
     *
     * @param path         以 {@code /v3/} 开头的 REST 路径
     * @param method       HTTP 方法
     * @param requestBody  可选 JSON 请求对象
     * @param queryParams  查询参数，迭代顺序就是发送与签名顺序
     * @param responseType 业务响应类型
     * @param <T>          业务响应类型
     * @return 已验签并解析的业务响应
     */
    public <T> T execute(
            String path,
            HttpMethod method,
            @Nullable Object requestBody,
            Map<String, String> queryParams,
            Class<T> responseType
    ) {
        // 1. 先构造最终发送 URL 和原始 JSON，签名必须覆盖完全相同的编码结果和字节内容。
        URI endpoint = endpoint(path, queryParams);
        String body = requestBody == null ? "" : encode(requestBody);
        String requestUri = endpoint.getRawPath()
                + (endpoint.getRawQuery() == null ? "" : "?" + endpoint.getRawQuery());

        // 2. 每次请求生成独立时间戳、nonce 和请求 ID，再按 V3 固定换行格式计算 Authorization。
        String nonce = requestId();
        String authString = "app_id=" + appId
                + ",nonce=" + nonce
                + ",timestamp=" + clock.millis();
        String signatureSource = authString + "\n"
                + method.getName() + "\n"
                + requestUri + "\n"
                + body + "\n";
        String authorization = V3_AUTH_SCHEME + " " + authString
                + ",sign=" + AlipayCryptoUtils.sign(signatureSource, appPrivateKey);

        HttpRequest.Builder builder = HttpRequest.builder(endpoint, method)
                .header("Accept", "application/json")
                .header(HEADER_AUTHORIZATION, authorization)
                .header(HEADER_REQUEST_ID, requestId());
        if (requestBody != null) {
            builder.jsonBody(body);
        }

        // 3. 发送失败只暴露脱敏传输元数据；响应正文必须先验签，之后才允许进入错误或业务解析。
        HttpResponse response;
        try {
            response = httpClient.send(builder.build());
        } catch (HttpException exception) {
            throw new AlipayTransportException(
                    exception.getKind(),
                    exception.getMethod(),
                    exception.getUrl(),
                    exception.getTransportCauseType()
            );
        }
        HttpHeaders responseHeaders = response.getHeaders();
        String responseSignature = signatureHeader(responseHeaders, HEADER_SIGNATURE);
        String responseTimestamp = signatureHeader(responseHeaders, HEADER_TIMESTAMP);
        String responseNonce = signatureHeader(responseHeaders, HEADER_NONCE);
        if (response.statusCode() != 200) {
            boolean hasSignatureMetadata = responseParser.hasSignatureMetadata(
                    responseSignature,
                    responseTimestamp,
                    responseNonce
            );
            String traceId = response.getHeader(HEADER_TRACE_ID);
            if (traceId == null) {
                // 部分接口元数据省略了 trace 与 id 之间的连字符，兼容读取但始终优先公共协议名称。
                traceId = response.getHeader(HEADER_TRACE_ID_COMPATIBLE);
            }
            try {
                throw responseParser.parseError(
                        response.statusCode(),
                        hasSignatureMetadata,
                        response.getBodyAsBytes(),
                        responseSignature,
                        responseTimestamp,
                        responseNonce,
                        traceId
                );
            } catch (AlipayProtocolException exception) {
                // 官方 V3 SDK允许错误响应不带签名；无法安全结构化时仅保留 HTTP 状态。
                throw new AlipayTransportException(response.statusCode());
            }
        }
        return responseParser.parseSuccess(
                response.getBodyAsBytes(),
                responseType,
                responseSignature,
                responseTimestamp,
                responseNonce
        );
    }

    /**
     * 使用最终 query 编码结果构造 V3 端点。
     *
     * @param path        V3 路径
     * @param queryParams 查询参数
     * @return 最终发送端点
     */
    private URI endpoint(String path, Map<String, String> queryParams) {
        if (!path.startsWith("/v3/")) {
            throw new IllegalArgumentException("V3 path must start with /v3/");
        }
        HttpUrlBuilder builder = HttpUrlBuilder.from(baseUrl).encodedPath(path);
        queryParams.forEach(builder::queryParam);
        return builder.build();
    }

    /**
     * 将请求模型编码为会直接参与签名和发送的 JSON 原文。
     *
     * @param value 请求模型
     * @return JSON 原文
     */
    private String encode(Object value) {
        try {
            return jsonCodec.write(value);
        } catch (JsonException exception) {
            throw new AlipayProtocolException("无法编码支付宝请求 JSON");
        }
    }

    /**
     * 生成符合支付宝当前 32 字符限制的唯一请求标识或 nonce。
     *
     * @return 去除连字符的 UUID
     */
    private static String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 读取最多出现一次的 V3 签名元数据头。
     *
     * @param headers 原始响应头
     * @param name    响应头名称
     * @return 唯一响应头值；没有时为 {@code null}
     * @throws AlipaySecurityException 同名响应头出现多次
     */
    private static @Nullable String signatureHeader(HttpHeaders headers, String name) {
        List<String> values = headers.values(name);
        if (values.size() > 1) {
            throw new AlipaySecurityException(
                    AlipaySecurityFailure.DUPLICATE_SIGNATURE_HEADER
            );
        }
        return values.isEmpty() ? null : values.getFirst();
    }

    /** 校验借入 HTTP 客户端不会隐式重试或跟随重定向。 */
    private static HttpClient requireSafeHttpClient(HttpClient value) {
        ValidationUtils.requireNonNull(value, "httpClient must not be null");
        ValidationUtils.requireTrue(
                !OkHttpInterop.unwrap(value).retryOnConnectionFailure(),
                "httpClient must disable connection failure retries"
        );
        ValidationUtils.requireTrue(
                !OkHttpInterop.unwrap(value).followRedirects(),
                "httpClient must disable redirects"
        );
        ValidationUtils.requireTrue(
                !OkHttpInterop.unwrap(value).followSslRedirects(),
                "httpClient must disable cross-protocol redirects"
        );
        return value;
    }

}
