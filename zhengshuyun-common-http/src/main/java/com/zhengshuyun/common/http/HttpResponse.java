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

package com.zhengshuyun.common.http;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.zhengshuyun.common.core.lang.Validate;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * HTTP 响应封装
 * <p>
 * 提供便捷的响应数据访问方法, 自动管理底层网络连接资源.
 *
 * <h2>资源管理</h2>
 * HttpResponse 持有底层网络连接, 使用完毕后必须关闭, 推荐使用 try-with-resources：
 * <pre>{@code
 * try (HttpResponse response = client.get(url)) {
 *     String body = response.getBodyAsString();
 * }
 * }</pre>
 *
 * <p><strong>重要</strong>: {@link #getBodyAsStream()} 返回的流由 HttpResponse 管理生命周期,
 * <strong>不需要单独关闭</strong>. 关闭 HttpResponse 会自动关闭流和释放网络连接.
 *
 * <h2>响应体读取</h2>
 * <ul>
 *     <li>{@link #getBodyAsBytes()} - 读取为字节数组 (自动缓存, 支持重复调用)</li>
 *     <li>{@link #getBodyAsStream()} - 读取为流 (一次性, 重复调用抛异常)</li>
 *     <li>{@link #getBodyAsString()} - 读取为字符串 (委托给 getBodyAsBytes)</li>
 * </ul>
 *
 * <p><strong>重要</strong>:
 * <ul>
 *     <li>响应体只能被消费一次 (getBodyAsStream 直接返回底层流)</li>
 *     <li>如需多次读取, 请先调用 getBodyAsBytes() 缓存, 再调用其他方法</li>
 *     <li>重复调用 getBodyAsStream() 会抛出 IllegalStateException</li>
 * </ul>
 *
 * <h2>线程安全</h2>
 * 不是线程安全的, 不要在多线程间共享实例.
 *
 * @author Toint
 * @since 2026/1/8
 */
public final class HttpResponse implements AutoCloseable {

    /**
     * 原始的 OkHttp Response 对象
     */
    private final Response response;

    /**
     * 响应体字节数组
     */
    private byte[] cacheBodyBytes;

    /**
     * 响应体是否已被消费 (调用过 getBodyAsStream 或 getBodyAsBytes)
     */
    private boolean bodyConsumed = false;

    /**
     * 元数据
     */
    private final HttpCallMetadata metadata;

    private HttpResponse(Response response, HttpCallMetadata metadata) {
        Validate.notNull(response, "response must not be null");
        Validate.notNull(metadata, "metadata must not be null");
        this.response = response;
        this.metadata = metadata;
    }

    /**
     * 从 OkHttp Response 创建 (带元数据)
     *
     * @param response OkHttp 响应
     * @param metadata 元数据
     */
    static HttpResponse of(Response response, HttpCallMetadata metadata) {
        return new HttpResponse(response, metadata);
    }

    /**
     * 获取 HTTP 状态码
     */
    public int getCode() {
        return response.code();
    }

    /**
     * 获取 HTTP 状态消息
     */
    public String getMessage() {
        return response.message();
    }

    /**
     * 是否成功 (2xx)
     */
    public boolean isSuccessful() {
        return response.isSuccessful();
    }

    /**
     * 是否重定向 (3xx)
     */
    public boolean isRedirect() {
        return response.isRedirect();
    }

    /**
     * 获取响应头
     */
    public Headers getHeaders() {
        return response.headers();
    }

    /**
     * 获取指定响应头的所有值
     */
    public List<String> getHeaders(String name) {
        return response.headers(name);
    }

    /**
     * 获取指定响应头的值
     */
    public String getHeader(String name) {
        return response.header(name);
    }

    /**
     * 获取指定响应头的值 (带默认值)
     */
    public String getHeaderOrDefault(String name, String defaultValue) {
        return response.header(name, defaultValue);
    }

    /**
     * 获取 Content-Type
     */
    public String getContentType() {
        return getHeader(HttpHeaders.CONTENT_TYPE);
    }

    /**
     * 获取 Content-Length
     * <p>
     * 从响应头获取, 协议头可能伪造, 请勿过分依赖
     *
     * @return Content-Length, 未知时返回 -1
     */
    public long getContentLength() {
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            return responseBody.contentLength();
        } else {
            return -1;
        }
    }

    /**
     * 获取 Location 响应头 (重定向地址)
     *
     * @return 重定向地址, 不存在时返回 null
     */
    public String getLocation() {
        return getHeader("Location");
    }

    /**
     * 获取所有 Set-Cookie 响应头
     * <p>
     * 返回解析后的 Cookie 名称和值的映射. 如果存在多个同名 Cookie, 后者会覆盖前者.
     * <p>
     * 注意：只返回 Cookie 的名称和值, 不包含其他属性 (如 Path、Domain、Expires 等)
     *
     * @return 不可变的 Cookie 名称到值的映射, 无 Cookie 时返回空 Map
     */
    public Map<String, String> getCookies() {
        List<String> cookieHeaders = getHeaders("Set-Cookie");
        if (cookieHeaders.isEmpty()) {
            return ImmutableMap.of();
        }

        // 用于分割 name=value (只分割第一个 '=') 
        Splitter cookieSplitter = Splitter.on('=').limit(2).trimResults();
        // 用于移除引号
        CharMatcher quoteMatcher = CharMatcher.is('"');

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        for (String cookie : cookieHeaders) {
            if (cookie == null || cookie.isBlank()) {
                continue;
            }

            // 提取 name=value 部分 (分号前) 
            int semicolonIndex = cookie.indexOf(';');
            String nameValue = semicolonIndex > 0
                    ? cookie.substring(0, semicolonIndex)
                    : cookie;

            // 解析 name 和 value
            List<String> parts = cookieSplitter.splitToList(nameValue);
            if (parts.size() == 2 && !parts.get(0).isEmpty()) {
                String name = parts.get(0);
                String value = quoteMatcher.trimFrom(parts.get(1));  // 移除两端引号
                builder.put(name, value);
            }
        }

        return builder.buildKeepingLast();  // 后者覆盖前者
    }

    /**
     * 获取指定名称的 Cookie 值
     *
     * @param name Cookie 名称
     * @return Cookie 值, 不存在时返回 null
     */
    @Nullable
    public String getCookie(String name) {
        Validate.notBlank(name, "Cookie name must not be blank");
        return getCookies().get(name);
    }

    /**
     * 获取响应体字节数组
     * <p>
     * 注意：会将整个响应体加载到内存中, 不适合大文件.
     *
     * @return 响应体字节数组
     * @throws HttpException         读取失败时抛出
     * @throws IllegalStateException 如果响应体已被消费且未缓存
     */
    public byte[] getBodyAsBytes() {
        // 如果已缓存, 直接返回
        if (cacheBodyBytes != null) {
            return cacheBodyBytes;
        }

        // 获取流并读取 (流的生命周期由 HttpResponse 管理, 无需单独关闭)
        try {
            InputStream stream = getBodyAsStream();
            cacheBodyBytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new HttpException("Failed to read response body. " + e.getMessage(), e);
        }
        return cacheBodyBytes;
    }

    /**
     * 获取响应体字符串 (使用 UTF-8 编码)
     * <p>
     * 注意：会将整个响应体加载到内存中, 不适合大文件
     *
     * @return 响应体字符串
     * @throws HttpException 读取失败时抛出
     */
    public String getBodyAsString() {
        return getBodyAsString(StandardCharsets.UTF_8);
    }

    /**
     * 获取响应体字符串 (指定编码)
     * <p>
     * 注意：会将整个响应体加载到内存中, 不适合大文件
     *
     * @param charset 字符编码
     * @return 响应体字符串
     * @throws HttpException 读取失败时抛出
     */
    public String getBodyAsString(Charset charset) {
        return new String(getBodyAsBytes(), charset);
    }

    /**
     * 获取响应体输入流
     * <p>
     * 适合处理大文件, 流式读取不会占用大量内存.
     *
     * <h2>资源管理</h2>
     * <p><strong>重要</strong>: 返回的流的生命周期由 HttpResponse 管理,
     * <strong>不需要</strong>单独关闭流, 只需要关闭 HttpResponse:
     *
     * <pre>{@code
     * // ✅ 正确: 只关闭 HttpResponse
     * try (HttpResponse response = client.get(url)) {
     *     InputStream stream = response.getBodyAsStream();
     *     // 使用 stream...
     *     // response.close() 时会自动关闭流和网络连接
     * }
     *
     * // ❌ 错误: 单独关闭流不会释放网络连接
     * HttpResponse response = client.get(url);
     * InputStream stream = response.getBodyAsStream();
     * stream.close();  // 网络连接仍然打开!
     * }</pre>
     *
     * <h2>多次读取</h2>
     * <p><strong>注意</strong>:
     * <ul>
     *   <li>如果已调用 {@link #getBodyAsBytes()}, 返回缓存的字节流 (可重复调用, 可单独关闭)</li>
     *   <li>否则返回底层流 (一次性, 再次调用抛异常, 生命周期由 Response 管理)</li>
     *   <li>建议: 需要多次读取时, 先调用 getBodyAsBytes() 缓存</li>
     * </ul>
     *
     * @return 响应体输入流, 如果响应体为空返回空流
     * @throws IllegalStateException 如果响应体已被消费且未缓存
     */
    public InputStream getBodyAsStream() {
        // 如果已经缓存了, 返回缓存流 (可重复调用)
        if (cacheBodyBytes != null) {
            return new ByteArrayInputStream(cacheBodyBytes);
        }

        // 检查是否已被消费
        if (bodyConsumed) {
            throw new IllegalStateException(
                    "Response body stream has already been consumed. Use getBodyAsBytes() for caching and multiple reads.");
        }

        // 返回原始流 (生命周期由 Response 管理)
        ResponseBody body = response.body();
        if (body != null) {
            bodyConsumed = true;
            return body.byteStream();
        }

        return InputStream.nullInputStream();
    }

    /**
     * 获取元数据
     */
    public HttpCallMetadata getMetadata() {
        return metadata;
    }

    /**
     * 获取 HTTP 协议版本
     *
     * @return 协议版本字符串, 如 "HTTP/1.1"、"h2"
     */
    public String getProtocol() {
        return response.protocol().name();
    }

    @Override
    public void close() {
        response.close();
    }

    @Override
    public String toString() {
        return getMetadata().toString();
    }
}