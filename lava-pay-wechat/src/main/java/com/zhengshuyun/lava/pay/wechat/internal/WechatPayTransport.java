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

package com.zhengshuyun.lava.pay.wechat.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhengshuyun.lava.http.*;
import com.zhengshuyun.lava.json.JsonCodec;
import com.zhengshuyun.lava.json.JsonException;
import com.zhengshuyun.lava.pay.wechat.*;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 微信支付 APIv3 的统一签名、发送、验签和错误解析传输层。
 */
public final class WechatPayTransport {
    private static final byte[] EMPTY_BODY = new byte[0];
    private static final int MAX_DOWNLOAD_ERROR_BYTES = 64 * 1024;
    private static final Set<String> OFFICIAL_API_HOSTS = Set.of(
            "api.mch.weixin.qq.com", "api2.mch.weixin.qq.com");

    private final String mchid;
    private final String merchantSerialNo;
    private final PrivateKey merchantPrivateKey;
    private final String wechatPayPublicKeyId;
    private final PublicKey wechatPayPublicKey;
    private final byte[] apiV3Key;
    private final HttpClient httpClient;
    private final URI apiBaseUrl;
    private final Clock clock;
    private final Supplier<String> nonceSupplier;
    private final JsonCodec jsonCodec;

    /**
     * 创建内部传输层。调用方负责保证配置已完成校验。
     *
     * @param mchid 商户号
     * @param merchantSerialNo 商户 API 证书序列号
     * @param merchantPrivateKey 商户私钥
     * @param wechatPayPublicKeyId 微信支付公钥 ID
     * @param wechatPayPublicKey 微信支付公钥
     * @param apiV3Key APIv3 密钥
     * @param httpClient HTTP 客户端
     * @param apiBaseUrl API 根地址
     * @param clock 签名和验签时钟
     * @param nonceSupplier 请求随机串生成器
     * @param jsonCodec JSON 编解码器
     */
    public WechatPayTransport(String mchid, String merchantSerialNo,
                              PrivateKey merchantPrivateKey,
                              String wechatPayPublicKeyId,
                              PublicKey wechatPayPublicKey,
                              byte[] apiV3Key,
                              HttpClient httpClient,
                              URI apiBaseUrl,
                              Clock clock,
                              Supplier<String> nonceSupplier,
                              JsonCodec jsonCodec) {
        this.mchid = mchid;
        this.merchantSerialNo = merchantSerialNo;
        this.merchantPrivateKey = merchantPrivateKey;
        this.wechatPayPublicKeyId = wechatPayPublicKeyId;
        this.wechatPayPublicKey = wechatPayPublicKey;
        this.apiV3Key = apiV3Key.clone();
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
        this.clock = clock;
        this.nonceSupplier = nonceSupplier;
        this.jsonCodec = jsonCodec;
    }

    /**
     * 返回配置的商户号。
     *
     * @return 商户号
     */
    public String mchid() {
        return mchid;
    }

    /**
     * 从配置的 API 根地址创建固定路径端点。
     *
     * @param path API 绝对路径
     * @return 完整 URI
     */
    public URI endpoint(String path) {
        return HttpUrlBuilder.from(apiBaseUrl).path(path).build();
    }

    /**
     * 创建包含一个经过编码的动态路径段的 API 端点。
     *
     * @param prefix 动态段之前的固定路径
     * @param segment 动态路径段
     * @param suffix 动态段之后的固定路径；没有时传空字符串
     * @return 完整 URI
     */
    public URI endpoint(String prefix, String segment, String suffix) {
        HttpUrlBuilder builder = HttpUrlBuilder.from(apiBaseUrl)
                .path(prefix)
                .appendPathSegment(segment);
        if (!suffix.isEmpty()) {
            builder.appendPath(suffix);
        }
        return builder.build();
    }

    /**
     * 设置或替换 URI 的单个查询参数。
     *
     * @param uri 原 URI
     * @param name 参数名
     * @param value 参数值
     * @return 新 URI
     */
    public URI query(URI uri, String name, @Nullable String value) {
        return HttpUrlBuilder.from(uri).queryParam(name, value).build();
    }

    /**
     * 发送签名 GET 请求，验签成功后解析响应。
     *
     * @param uri 请求 URI
     * @param responseType 响应模型
     * @param <T> 响应类型
     * @return 已验签响应模型
     */
    public <T> T get(URI uri, Class<T> responseType) {
        return send(HttpMethod.GET, uri, null, responseType);
    }

    /**
     * 发送签名 POST 请求，验签成功后解析响应。
     *
     * @param uri 请求 URI
     * @param requestBody 请求模型
     * @param responseType 响应模型
     * @param <T> 响应类型
     * @return 已验签响应模型
     */
    public <T> T post(URI uri, Object requestBody, Class<T> responseType) {
        return send(HttpMethod.POST, uri, requestBody, responseType);
    }

    /**
     * 发送期望 204 空响应的签名 POST 请求。
     *
     * @param uri 请求 URI
     * @param requestBody 请求模型
     */
    public void postNoContent(URI uri, Object requestBody) {
        byte[] body = encode(requestBody);
        HttpResponse response = execute(HttpMethod.POST, uri, body);
        verify(response.getHeaders(), response.getBodyAsBytes());
        if (!response.isSuccessful()) {
            throw apiException(response);
        }
        if (response.statusCode() != 204 || response.getContentLength() != 0) {
            throw new WechatPayProtocolException("微信支付关单响应必须为 204 空正文");
        }
    }

    /**
     * 打开账单文件下载流。账单文件响应按官方规则不执行响应验签。
     *
     * @param uri 已由申请账单接口返回且验签通过的下载地址
     * @return 调用方负责关闭的下载流
     */
    public HttpStream openDownload(URI uri) {
        requireTrustedDownloadUrl(uri);
        HttpRequest request = signedRequest(HttpMethod.GET, uri, EMPTY_BODY);
        HttpStream stream;
        try {
            stream = httpClient.openStream(request);
        } catch (HttpException exception) {
            throw transportException(exception);
        }
        if (stream.isSuccessful()) {
            return stream;
        }

        try (stream) {
            byte[] body;
            try {
                body = stream.body().readNBytes(MAX_DOWNLOAD_ERROR_BYTES + 1);
            } catch (IOException exception) {
                throw new WechatPayFileException(WechatPayFileFailure.IO,
                        exception.getClass().getName());
            }
            if (body.length > MAX_DOWNLOAD_ERROR_BYTES) {
                throw new WechatPayProtocolException("微信支付账单下载错误响应超过大小限制");
            }
            throw apiException(stream.statusCode(), stream.headers(), body);
        }
    }

    /**
     * 验证原始微信支付消息签名。
     *
     * @param headers 签名请求头
     * @param body 原始正文
     */
    public void verify(HttpHeaders headers, byte[] body) {
        WechatPayCryptoUtils.verifyMessage(headers, body, wechatPayPublicKeyId,
                wechatPayPublicKey, clock);
    }

    /**
     * 解密回调资源。
     *
     * @param algorithm 加密算法
     * @param nonce 随机串
     * @param associatedData 附加数据
     * @param ciphertext 密文
     * @return 明文 JSON 字节
     */
    public byte[] decrypt(String algorithm, String nonce, @Nullable String associatedData,
                          String ciphertext) {
        return WechatPayCryptoUtils.decrypt(apiV3Key, algorithm, nonce,
                associatedData, ciphertext);
    }

    /**
     * 清除当前客户端持有的 APIv3 密钥副本。
     */
    public void clearSecret() {
        Arrays.fill(apiV3Key, (byte) 0);
    }

    private <T> T send(HttpMethod method, URI uri, @Nullable Object requestBody,
                       Class<T> responseType) {
        byte[] body = requestBody == null ? EMPTY_BODY : encode(requestBody);
        HttpResponse response = execute(method, uri, body);
        byte[] responseBody = response.getBodyAsBytes();

        // 1. 成功和失败响应都必须先验证来源，不能让未验签错误信息进入业务判断。
        verify(response.getHeaders(), responseBody);
        if (!response.isSuccessful()) {
            throw apiException(response.statusCode(), response.getHeaders(), responseBody);
        }

        // 2. 验签通过后再解析 JSON，确保模型只承载可信数据。
        if (responseBody.length == 0) {
            throw new WechatPayProtocolException("微信支付成功响应缺少正文");
        }
        try {
            return jsonCodec.read(responseBody, responseType);
        } catch (JsonException | IllegalArgumentException exception) {
            throw new WechatPayProtocolException("微信支付成功响应不是预期的 JSON 结构");
        }
    }

    private HttpResponse execute(HttpMethod method, URI uri, byte[] body) {
        HttpRequest request = signedRequest(method, uri, body);
        try {
            return httpClient.send(request);
        } catch (HttpException exception) {
            throw transportException(exception);
        }
    }

    private HttpRequest signedRequest(HttpMethod method, URI uri, byte[] body) {
        long timestamp = clock.instant().getEpochSecond();
        String nonce = nonceSupplier.get();
        String authorization = WechatPayCryptoUtils.authorization(mchid, merchantSerialNo,
                merchantPrivateKey, method.getName(), uri, body, timestamp, nonce);

        // 签名正文与发送正文共用同一字节数组，避免 JSON 二次序列化造成签名不一致。
        HttpRequest.Builder builder = HttpRequest.builder(uri, method)
                .header(HttpHeaderNames.ACCEPT, HttpMediaTypes.APPLICATION_JSON)
                .header(WechatPayCryptoUtils.HEADER_SERIAL, wechatPayPublicKeyId)
                .authorization(authorization)
                .userAgent("lava-pay-wechat");
        if (body.length > 0) {
            builder.body(body, HttpMediaTypes.APPLICATION_JSON);
        }
        return builder.build();
    }

    private void requireTrustedDownloadUrl(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getRawFragment() != null) {
            throw new WechatPayProtocolException("微信支付账单下载地址无效");
        }
        boolean sameOrigin = sameOrigin(apiBaseUrl, uri);
        boolean officialOrigin = "https".equalsIgnoreCase(uri.getScheme())
                && OFFICIAL_API_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
                && (uri.getPort() == -1 || uri.getPort() == 443);
        if (!sameOrigin && !officialOrigin) {
            throw new WechatPayProtocolException("微信支付账单下载地址来源不受信任");
        }
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private byte[] encode(Object value) {
        try {
            return jsonCodec.writeBytes(value);
        } catch (JsonException exception) {
            throw new WechatPayProtocolException("无法编码微信支付请求 JSON");
        }
    }

    private WechatPayApiException apiException(HttpResponse response) {
        return apiException(response.statusCode(), response.getHeaders(),
                response.getBodyAsBytes());
    }

    private WechatPayApiException apiException(int statusCode, HttpHeaders headers,
                                               byte[] body) {
        ApiErrorPayload error;
        try {
            error = jsonCodec.read(body, ApiErrorPayload.class);
        } catch (JsonException exception) {
            throw new WechatPayProtocolException("微信支付错误响应不是预期的 JSON 结构");
        }
        if (error.code == null || error.code.isBlank() || error.message == null) {
            throw new WechatPayProtocolException("微信支付错误响应缺少 code 或 message");
        }
        WechatPayApiErrorDetail detail = error.detail == null ? null
                : new WechatPayApiErrorDetail(error.detail.field, error.detail.value,
                error.detail.issue, error.detail.location);
        return new WechatPayApiException(statusCode, error.code, error.message, detail,
                headers.get(WechatPayCryptoUtils.HEADER_REQUEST_ID));
    }

    private static WechatPayTransportException transportException(HttpException exception) {
        return new WechatPayTransportException(exception.getKind(), exception.getMethod(),
                exception.getUrl(), exception.getTransportCauseType());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ApiErrorPayload {
        public @Nullable String code;
        public @Nullable String message;
        public @Nullable ApiErrorDetailPayload detail;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class ApiErrorDetailPayload {
        public @Nullable String field;
        public @Nullable String value;
        public @Nullable String issue;
        public @Nullable String location;
    }
}
