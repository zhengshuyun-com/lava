/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.http;

import com.zhengshuyun.lava.core.lang.Validate;
import okhttp3.*;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

/**
 * HTTP 请求封装
 * <p>
 * 提供简洁的 Builder API 构建和执行 HTTP 请求, 支持 GET、POST、PUT、DELETE 等方法
 *
 * @author Toint
 * @since 2026/1/8
 */
public final class HttpRequest {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final String url;
    private final HttpMethod method;
    private final Headers headers;
    private final @Nullable RequestBody body;

    private HttpRequest(Builder builder) {
        Validate.notNull(builder, "builder must not be null");
        this.url = Validate.notBlank(builder.url, "url must not be blank");
        this.method = Validate.notNull(builder.method, "method must not be null");
        this.headers = Validate.notNull(builder.headersBuilder, "headersBuilder must not be null").build();
        this.body = builder.body;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Headers getHeaders() {
        return headers;
    }

    public @Nullable RequestBody getBody() {
        return body;
    }

    /**
     * 使用全局单例 HttpClient 执行请求
     * <p>
     * 返回的 {@link HttpResponse} 使用完后必须关闭, 推荐使用 try-with-resources
     *
     * @return HTTP 响应
     * @throws HttpException 请求失败时抛出
     * @see HttpUtil#execute(HttpRequest)
     */
    public HttpResponse execute() {
        return HttpUtil.execute(this);
    }

    /**
     * 使用指定的 HttpClient 执行请求
     * <p>
     * 返回的 {@link HttpResponse} 使用完后必须关闭, 推荐使用 try-with-resources
     *
     * @param httpClient HTTP 客户端
     * @return HTTP 响应
     * @throws IllegalArgumentException httpClient 为 null
     * @throws HttpException            请求失败时抛出
     */
    public HttpResponse execute(HttpClient httpClient) {
        return Validate.notNull(httpClient, "httpClient must not be null")
                .execute(this);
    }

    /**
     * 转换为 OkHttp Request
     * <p>
     * 框架层面处理请求体的合法性：
     * <ul>
     * <li>不允许携带请求体的方法 (GET/HEAD) 携带了请求体 → 自动忽略</li>
     * <li>必须携带请求体的方法 (POST/PUT/PATCH) 没有请求体 → 自动补充空请求体</li>
     * </ul>
     */
    Request toOkHttpRequest() {
        RequestBody requestBody = body;

        // 不允许有 body 的方法, 自动忽略 body
        if (!getMethod().permitsRequestBody() && requestBody != null) {
            requestBody = null;
        }

        // 必须有 body 的方法, 自动补充空 body
        if (getMethod().requiresRequestBody() && requestBody == null) {
            requestBody = RequestBody.create(new byte[0]);
        }

        return new Request.Builder()
                .url(url)
                .headers(headers)
                .method(method.getName(), requestBody)
                .build();
    }

    public static Builder builder(String url, HttpMethod method) {
        return new Builder(url, method, DEFAULT_CHARSET);
    }

    public static Builder builder(String url, HttpMethod method, Charset charset) {
        return new Builder(url, method, charset);
    }

    /**
     * 创建 GET 请求
     */
    public static Builder get(String url) {
        return new Builder(url, HttpMethod.GET, DEFAULT_CHARSET);
    }

    /**
     * 创建 GET 请求
     */
    public static Builder get(String url, Charset charset) {
        return new Builder(url, HttpMethod.GET, charset);
    }

    /**
     * 创建 POST 请求
     */
    public static Builder post(String url) {
        return new Builder(url, HttpMethod.POST, DEFAULT_CHARSET);
    }

    /**
     * 创建 POST 请求
     */
    public static Builder post(String url, Charset charset) {
        return new Builder(url, HttpMethod.POST, charset);
    }

    /**
     * 创建 PUT 请求
     */
    public static Builder put(String url) {
        return new Builder(url, HttpMethod.PUT, DEFAULT_CHARSET);
    }

    /**
     * 创建 PUT 请求
     */
    public static Builder put(String url, Charset charset) {
        return new Builder(url, HttpMethod.PUT, charset);
    }

    /**
     * 创建 DELETE 请求
     */
    public static Builder delete(String url) {
        return new Builder(url, HttpMethod.DELETE, DEFAULT_CHARSET);
    }

    /**
     * 创建 DELETE 请求
     */
    public static Builder delete(String url, Charset charset) {
        return new Builder(url, HttpMethod.DELETE, charset);
    }

    /**
     * 创建 PATCH 请求
     */
    public static Builder patch(String url) {
        return new Builder(url, HttpMethod.PATCH, DEFAULT_CHARSET);
    }

    /**
     * 创建 PATCH 请求
     */
    public static Builder patch(String url, Charset charset) {
        return new Builder(url, HttpMethod.PATCH, charset);
    }

    /**
     * 创建 HEAD 请求
     */
    public static Builder head(String url) {
        return new Builder(url, HttpMethod.HEAD, DEFAULT_CHARSET);
    }

    /**
     * 创建 HEAD 请求
     */
    public static Builder head(String url, Charset charset) {
        return new Builder(url, HttpMethod.HEAD, charset);
    }

    /**
     * HTTP 请求构建器
     */
    public static final class Builder {
        private final String url;
        private final HttpMethod method;
        private final Charset charset;
        private final Headers.Builder headersBuilder = new Headers.Builder();
        private @Nullable RequestBody body;

        private Builder(String url, HttpMethod method, Charset charset) {
            this.url = Validate.notBlank(url, "url must not be blank");
            this.method = Validate.notNull(method, "method must not be null");
            this.charset = Validate.notNull(charset, "charset must not be null");
        }

        /**
         * 添加请求头, 已存在会替换原有值
         */
        public Builder setHeader(String name, String value) {
            headersBuilder.set(name, value);
            return this;
        }

        /**
         * 添加请求头 (允许重复)
         */
        public Builder addHeader(String name, String value) {
            headersBuilder.add(name, value);
            return this;
        }

        /**
         * 设置 User-Agent (覆盖模式)
         * <p>
         * 注意：OkHttp 默认会使用 "okhttp/x.x.x" 作为 User-Agent,
         * 某些服务器可能会识别并限制非浏览器请求.
         * 如需模拟浏览器, 可使用 {@link #setUserAgentBrowser()} 或 {@link #setUserAgent(String)}
         * 配合 {@link HttpUserAgents} 常量类
         */
        public Builder setUserAgent(String userAgent) {
            return setHeader(HttpHeaders.USER_AGENT, userAgent);
        }

        /**
         * 设置 User-Agent 为浏览器 (默认使用 Chrome on macOS)
         * <p>
         * 使用此方法可以让请求看起来像来自真实浏览器, 避免被某些服务器拒绝或限流.
         * 等同于 {@code setUserAgent(UserAgents.DEFAULT)}
         *
         * @see HttpUserAgents
         */
        public Builder setUserAgentBrowser() {
            return setHeader(HttpHeaders.USER_AGENT, HttpUserAgents.DEFAULT);
        }

        /**
         * 设置 Authorization (覆盖模式)
         */
        public Builder setAuthorization(String authorization) {
            return setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }

        /**
         * 设置 Bearer Token 认证 (覆盖模式)
         *
         * @param token JWT token 或其他 Bearer token
         */
        public Builder setBearerToken(String token) {
            return setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        /**
         * 设置 Basic Auth 认证 (覆盖模式)
         *
         * @param username 用户名
         * @param password 密码
         */
        public Builder setBasicAuth(String username, String password) {
            Validate.notNull(username, "username must not be null");
            Validate.notNull(password, "password must not be null");
            String basic = Credentials.basic(username, password, charset);
            return setHeader(HttpHeaders.AUTHORIZATION, basic);
        }

        /**
         * 设置 Cookie (覆盖模式)
         * <p>
         * 完整的 cookie 字符串, 例如: "name1=value1; name2=value2"
         */
        public Builder setCookie(String cookie) {
            return setHeader(HttpHeaders.COOKIE, cookie);
        }

        /**
         * 设置 JSON 请求体
         */
        public Builder setJsonBody(String json) {
            this.body = RequestBody.create(json, MediaType.parse(HttpMediaTypes.APPLICATION_JSON));
            return this;
        }

        /**
         * 设置 XML 请求体
         */
        public Builder setXmlBody(String xml) {
            this.body = RequestBody.create(xml, MediaType.parse(HttpMediaTypes.APPLICATION_XML));
            return this;
        }

        /**
         * 设置纯文本请求体
         */
        public Builder setTextBody(String text) {
            this.body = RequestBody.create(text, MediaType.parse(HttpMediaTypes.TEXT_PLAIN));
            return this;
        }

        /**
         * 设置表单请求体 (application/x-www-form-urlencoded)
         */
        public Builder setFormBody(Map<String, String> params) {
            Validate.notNull(params, "params must not be null");
            FormBody.Builder formBuilder = new FormBody.Builder(charset);
            params.forEach(formBuilder::add);
            this.body = formBuilder.build();
            return this;
        }

        /**
         * 设置 Multipart 请求体 (文件上传)
         */
        public Builder setMultipartBody(MultipartBuilder multipartBuilder) {
            Validate.notNull(multipartBuilder, "multipartBuilder must not be null");
            this.body = multipartBuilder.build();
            return this;
        }

        /**
         * 设置 Multipart 请求体 (文件上传)
         */
        public Builder setMultipartBody(MultipartBody multipartBody) {
            Validate.notNull(multipartBody, "multipartBody must not be null");
            this.body = multipartBody;
            return this;
        }

        /**
         * 设置原始请求体
         */
        public Builder setBody(@Nullable RequestBody body) {
            this.body = body;
            return this;
        }

        /**
         * 构建请求
         */
        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }

    /**
     * Multipart 构建器 (用于文件上传)
     */
    public static final class MultipartBuilder {

        private final MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        public static MultipartBuilder builder() {
            return new MultipartBuilder();
        }

        /**
         * 添加表单字段
         */
        public MultipartBuilder addFormField(String name, String value) {
            builder.addFormDataPart(name, value);
            return this;
        }

        /**
         * 添加文件
         */
        public MultipartBuilder addFile(String name, Path path) {
            return addFile(name, path.toFile());
        }

        /**
         * 添加文件
         */
        public MultipartBuilder addFile(String name, Path path, String contentType) {
            return addFile(name, path.toFile(), contentType);
        }

        /**
         * 添加文件
         */
        public MultipartBuilder addFile(String name, File file) {
            return addFile(name, file, HttpMediaTypes.APPLICATION_OCTET_STREAM);
        }

        /**
         * 添加文件
         */
        public MultipartBuilder addFile(String name, File file, String contentType) {
            Validate.isTrue(file.exists(), "File does not exist: %s", file.getAbsolutePath());
            MediaType mediaType = MediaType.parse(contentType);
            Validate.notNull(mediaType, "Invalid content type: %s", contentType);
            RequestBody requestBody = RequestBody.create(file, mediaType);
            builder.addFormDataPart(name, file.getName(), requestBody);
            return this;
        }

        /**
         * 添加文件
         */
        public MultipartBuilder addFile(String name, @Nullable String filename, byte[] data) {
            return addFile(name, filename, data, HttpMediaTypes.APPLICATION_OCTET_STREAM);
        }

        /**
         * 添加文件
         */
        public MultipartBuilder addFile(String name, @Nullable String filename, byte[] data, String contentType) {
            Validate.notNull(data, "data must not be null");
            Validate.notNull(contentType, "contentType must not be null");
            MediaType mediaType = MediaType.parse(contentType);
            Validate.notNull(mediaType, "Invalid content type: {}", contentType);
            RequestBody requestBody = RequestBody.create(data, mediaType);
            builder.addFormDataPart(name, filename, requestBody);
            return this;
        }

        public MultipartBody build() {
            return builder.build();
        }
    }
}
