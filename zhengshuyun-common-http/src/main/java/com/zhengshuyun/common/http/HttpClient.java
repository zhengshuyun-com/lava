/*
 * Copyright 2025 Toint (599818663@qq.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.common.http;

import com.zhengshuyun.common.core.id.IdUtil;
import com.zhengshuyun.common.core.io.IoUtil;
import com.zhengshuyun.common.core.lang.Validate;
import okhttp3.*;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 客户端封装类
 * <p>
 * 基于 OkHttp 实现的同步 HTTP 客户端, 提供统一的请求执行接口和灵活的配置能力. 
 *
 * @author Toint
 * @since 2026/1/8
 */
public final class HttpClient {

    /**
     * 底层 OkHttp 客户端实例
     */
    private final OkHttpClient okHttpClient;

    /**
     * 私有无参构造函数, 防止直接实例化
     */
    private HttpClient() {
        throw new RuntimeException();
    }

    /**
     * 使用已有的 OkHttpClient 实例构造 HttpClient
     *
     * @param client 非 null 的 OkHttpClient 实例
     */
    public HttpClient(OkHttpClient client) {
        Validate.notNull(client, "OkHttpClient must not be null");
        this.okHttpClient = client;
    }

    /**
     * 创建 Builder 实例用于构建 HttpClient
     *
     * @return Builder 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @see HttpClient#execute(HttpRequest, Builder)
     */
    public HttpResponse execute(HttpRequest request) {
        return execute(request, null);
    }

    /**
     * 同步执行请求
     *
     * @param request HTTP 请求
     * @param config  方法级自定义配置 (仅支持部分配置) 
     * @return HTTP 响应
     * @throws HttpException 请求失败时抛出
     */
    public HttpResponse execute(HttpRequest request, @Nullable Builder config) {
        Validate.notNull(request, "HttpRequest must not be null");

        final OkHttpClient client = getClient(config);

        Response response = null;
        try {
            // 记录开始时间
            Instant requestTime = Instant.now();
            // 执行请求
            response = client.newCall(request.toOkHttpRequest()).execute();
            // 记录结束时间
            Instant responseTime = Instant.now();

            // 构建元数据
            HttpCallMetadata metadata = HttpCallMetadata.builder()
                    .setRequestId(IdUtil.randomUUIDWithoutDash())
                    .setUrl(request.getUrl())
                    .setMethod(request.getMethod().getName())
                    .setRequestTime(requestTime)
                    .setResponseTime(responseTime)
                    .setRequestHeaders(request.getHeaders())
                    .setResponseHeaders(response.headers())
                    .setProtocol(response.protocol().name())
                    .setStatusCode(response.code())
                    .setStatusMessage(response.message())
                    .build();

            // 创建并返回 HttpResponse (response 的生命周期由 HttpResponse 管理, 此处无需 close) 
            return HttpResponse.of(response, metadata);
        } catch (IOException e) {
            // 确保 response 被关闭以避免资源泄漏
            IoUtil.closeQuietly(response);
            throw new HttpException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取客户端实例 (支持方法级配置覆盖) 
     * <p>
     * 当传入 config 时, 基于基础客户端创建一个新实例并应用配置覆盖；
     * 否则直接返回基础客户端. 
     *
     * @param config 方法级自定义配置, 可能为 null
     * @return OkHttpClient 实例
     */
    private OkHttpClient getClient(@Nullable Builder config) {
        if (config == null) {
            return okHttpClient;
        }
        OkHttpClient.Builder builder = okHttpClient.newBuilder();
        return config.applyTo(builder).build();
    }

    /**
     * HTTP 客户端构建器
     * <p>
     * 用于构建 HttpClient 实例, 支持配置超时、连接池、重试、重定向、拦截器、Cookie、代理等. 
     * 也可作为方法级配置传递给 execute() 方法, 覆盖默认配置. 
     */
    public final static class Builder {

        /**
         * 连接超时时间
         * <p>
         * 建立TCP连接的最长等待时间
         */
        private Duration connectTimeout = Duration.ofSeconds(10);

        /**
         * 读取超时时间
         * <p>
         * 从服务器读取响应数据的最长等待时间.
         * <p>
         * 注意: 不是"整个读取过程的总时间", 而是"两次读取数据之间的最大间隔"
         */
        private Duration readTimeout = Duration.ofSeconds(30);

        /**
         * 写入超时时间
         * <p>
         * 向服务器写入请求数据的最长等待时间
         * <p>
         * 注意: 大文件上传需单独配置更长的时间
         */
        private Duration writeTimeout = Duration.ofSeconds(10);

        /**
         * 总调用超时时间
         * <p>
         * 整个请求的最长时间 (连接+写入+读取) 
         * <p>
         * 为什么必须修改：
         * - OkHttp默认0 (不限制) , 可能导致请求永久挂起
         * - 防止重试、重定向等导致的累积延迟
         * - 给用户一个明确的最长等待时间
         */
        private Duration callTimeout = Duration.ofSeconds(60);

        /**
         * 连接池最大空闲连接数
         */
        private int maxIdleConnections = 10;

        /**
         * 连接保持存活时间
         * <p>
         * 空闲连接在池中的最长保持时间
         */
        private Duration keepAliveDuration = Duration.ofMinutes(5);

        /**
         * 连接失败时是否自动重试
         * <p>
         * 默认情况下, 只有少数几种[连接/握手层面的失败]会自动重试, 而且有很多硬性限制.
         * 而且对于「非幂等、不能重复执行」的业务 (支付、下单、扣款等) 有影响
         * 最安全的方式: 让业务层控制重试, 底层库只负责发送请求
         */
        private final boolean retryOnConnectionFailure = false;

        /**
         * 是否自动跟随协议内重定向 (如 301, 302, 303 等状态码) 
         * <p>
         * 如果为 true, OkHttp 会自动请求 Location 指定的新地址；
         * 如果为 false, 则将重定向响应当作普通响应直接返回给开发者. 
         */
        private boolean followRedirects = true;

        /**
         * 是否允许跨协议重定向 (例如从 HTTP 重定向到 HTTPS, 或反之) 
         * <p>
         * 仅当 {@link #followRedirects} 为 true 时此配置才生效. 
         * 出于安全考虑, 建议结合业务场景谨慎开启 (通常建议关闭, 防止 HTTPS 被降级到 HTTP) . 
         */
        private boolean followSslRedirects = false;
        /**
         * 自定义应用拦截器
         * <p>
         * 作用：添加通用逻辑 (认证、公共参数、加解密等) 
         * <p>
         * 为什么必须配置：
         * - 几乎所有生产应用都需要添加认证信息
         * - 需要统一处理公共Header (User-Agent、Accept-Language等) 
         * - 需要统一错误处理和重试逻辑
         * <p>
         * 国际主流用途：
         * - 添加Authorization头 (JWT、OAuth2等) 
         * - 添加API Key
         * - 添加请求签名
         * - 添加设备信息
         * - 统一错误码处理
         * - 请求加密/响应解密
         * <p>
         * 示例：
         * builder.addInterceptor(chain -> {
         * Request request = chain.request().newBuilder()
         * .header("Authorization", "Bearer " + token)
         * .header("User-Agent", "MyApp/1.0")
         * .build();
         * return chain.proceed(request);
         * });
         */
        private final List<Interceptor> interceptors = new ArrayList<>();

        /**
         * Cookie管理器
         * <p>
         * 作用：自动保存和发送Cookie
         * <p>
         * 为什么需要配置：
         * - OkHttp默认不管理Cookie (CookieJar.NO_COOKIES) 
         * - Web应用几乎都需要会话管理
         * - 移动端可能需要持久化Cookie
         * <p>
         * 国际主流用途：
         * - Web应用：必须实现 (会话保持) 
         * - 移动应用：看需求 (登录态保持) 
         * - 服务端调用：通常不需要
         * <p>
         * 默认值：NO_COOKIES (不管理Cookie, 适合无状态的API调用) 
         */
        private CookieJar cookieJar = CookieJar.NO_COOKIES;

        /**
         * 代理
         */
        private HttpProxy proxy;

        /**
         * 设置连接超时时间
         */
        public Builder setConnectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        /**
         * 设置读取超时时间
         */
        public Builder setReadTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        /**
         * 设置写入超时时间
         */
        public Builder setWriteTimeout(Duration timeout) {
            this.writeTimeout = timeout;
            return this;
        }

        /**
         * 设置总调用超时时间
         */
        public Builder setCallTimeout(Duration timeout) {
            this.callTimeout = timeout;
            return this;
        }

        /**
         * 设置是否自动跟随协议内重定向
         */
        public Builder setFollowRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        /**
         * 设置是否允许跨协议重定向
         */
        public Builder setFollowSslRedirects(boolean followSslRedirects) {
            this.followSslRedirects = followSslRedirects;
            return this;
        }

        /**
         * 设置连接池配置
         */
        public Builder setConnectionPool(int maxIdleConnections, Duration keepAliveDuration) {
            this.maxIdleConnections = maxIdleConnections;
            this.keepAliveDuration = keepAliveDuration;
            return this;
        }

        /**
         * 添加应用拦截器
         */
        public Builder addInterceptor(Interceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        /**
         * 设置Cookie管理器
         */
        public Builder setCookieJar(CookieJar cookieJar) {
            this.cookieJar = cookieJar;
            return this;
        }

        /**
         * 设置代理配置
         *
         * @param proxy 代理配置 (包含代理选择器和认证器) 
         * @return this
         */
        public Builder setProxy(HttpProxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * 构建 OkHttpClient
         */
        public HttpClient build() {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            // 连接池 (仅在 build 时设置, 方法级配置不支持修改连接池) 
            builder.connectionPool(new ConnectionPool(
                    maxIdleConnections,
                    keepAliveDuration.toMillis(),
                    TimeUnit.MILLISECONDS
            ));

            // 应用其他配置
            OkHttpClient okHttpClient = applyTo(builder).build();
            return new HttpClient(okHttpClient);
        }

        /**
         * 将当前配置应用到 OkHttpClient.Builder
         * <p>
         * 用于 build() 和 execute() 方法级配置覆盖, 避免重复代码
         *
         * @param builder OkHttpClient.Builder 实例
         */
        private OkHttpClient.Builder applyTo(OkHttpClient.Builder builder) {
            // 超时配置
            builder.connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .writeTimeout(writeTimeout)
                    .callTimeout(callTimeout);

            // 重试与重定向配置
            builder.retryOnConnectionFailure(retryOnConnectionFailure)
                    .followRedirects(followRedirects)
                    .followSslRedirects(followSslRedirects);

            // 自定义拦截器
            for (Interceptor interceptor : interceptors) {
                builder.addInterceptor(interceptor);
            }

            // Cookie 管理
            if (cookieJar != null) {
                builder.cookieJar(cookieJar);
            }

            // 代理配置
            if (proxy != null) {
                if (proxy.getProxySelector() != null) {
                    builder.proxySelector(proxy.getProxySelector());
                }
                if (proxy.getAuthenticator() != null) {
                    builder.proxyAuthenticator(proxy.getAuthenticator());
                }
            }

            return builder;
        }
    }
}
