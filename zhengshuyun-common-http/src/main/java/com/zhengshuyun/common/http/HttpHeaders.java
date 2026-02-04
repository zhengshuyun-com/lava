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

/**
 * HTTP 请求头常量
 * <p>
 * 包含常用的 HTTP 请求头和响应头名称
 *
 * @author Toint
 * @since 2026/1/12
 */
public final class HttpHeaders {

    private HttpHeaders() {}

    /**
     * Accept: 客户端能够接收的内容类型
     * <p>
     * 例如: application/json, text/html, *&#47;*
     */
    public static final String ACCEPT = "Accept";

    /**
     * Accept-Charset: 客户端能够接收的字符集
     * <p>
     * 例如: utf-8, iso-8859-1
     */
    public static final String ACCEPT_CHARSET = "Accept-Charset";

    /**
     * Accept-Encoding: 客户端能够接收的编码格式
     * <p>
     * 例如: gzip, deflate, br
     */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * Accept-Language: 客户端能够接收的语言
     * <p>
     * 例如: zh-CN, en-US
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";

    /**
     * Authorization: 身份认证信息
     * <p>
     * 例如: Bearer token, Basic base64credentials
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Cache-Control: 缓存控制
     * <p>
     * 例如: no-cache, max-age=3600
     */
    public static final String CACHE_CONTROL = "Cache-Control";

    /**
     * Connection: 连接选项
     * <p>
     * 例如: keep-alive, close
     */
    public static final String CONNECTION = "Connection";

    /**
     * Content-Length: 请求体长度
     */
    public static final String CONTENT_LENGTH = "Content-Length";

    /**
     * Content-Type: 请求体的MIME类型
     * <p>
     * 例如: application/json, application/x-www-form-urlencoded
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * Cookie: 客户端Cookie
     * <p>
     * 例如: sessionid=abc123; userid=456
     */
    public static final String COOKIE = "Cookie";

    /**
     * Date: 消息发送的日期和时间
     */
    public static final String DATE = "Date";

    /**
     * Host: 服务器域名
     * <p>
     * 例如: www.example.com
     */
    public static final String HOST = "Host";

    /**
     * If-Modified-Since: 条件请求, 仅在资源被修改后才返回
     */
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    /**
     * If-None-Match: 条件请求, 配合ETag使用
     */
    public static final String IF_NONE_MATCH = "If-None-Match";

    /**
     * Origin: 请求的来源 (用于CORS) 
     * <p>
     * 例如: https://example.com
     */
    public static final String ORIGIN = "Origin";

    /**
     * Referer: 引用页面的地址
     * <p>
     * 例如: https://example.com/page1
     */
    public static final String REFERER = "Referer";

    /**
     * User-Agent: 用户代理字符串
     * <p>
     * 例如: Mozilla/5.0 (Windows NT 10.0; Win64; x64)...
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * X-Requested-With: 标识Ajax请求
     * <p>
     * 通常值为: XMLHttpRequest
     */
    public static final String X_REQUESTED_WITH = "X-Requested-With";

    /**
     * X-Forwarded-For: 客户端真实IP (通过代理时) 
     */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * X-Real-IP: 客户端真实IP (Nginx代理) 
     */
    public static final String X_REAL_IP = "X-Real-IP";

    /**
     * Range: 请求资源的部分内容 (用于断点续传) 
     * <p>
     * 例如: bytes=0-1023
     */
    public static final String RANGE = "Range";

    /**
     * Upgrade: 协议升级
     * <p>
     * 例如: websocket
     */
    public static final String UPGRADE = "Upgrade";

    // ==================== 响应头 ====================

    /**
     * Content-Disposition: 内容处理方式
     * <p>
     * 例如: attachment; filename="file.txt"
     */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * Content-Encoding: 响应体的编码格式
     * <p>
     * 例如: gzip, deflate
     */
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /**
     * Set-Cookie: 设置Cookie
     */
    public static final String SET_COOKIE = "Set-Cookie";

    /**
     * Location: 重定向地址
     */
    public static final String LOCATION = "Location";

    /**
     * ETag: 资源的唯一标识
     */
    public static final String ETAG = "ETag";

    /**
     * Last-Modified: 资源的最后修改时间
     */
    public static final String LAST_MODIFIED = "Last-Modified";

    /**
     * Expires: 响应过期时间
     */
    public static final String EXPIRES = "Expires";

    /**
     * Transfer-Encoding: 传输编码方式
     * <p>
     * 例如: chunked
     */
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    /**
     * Access-Control-Allow-Origin: CORS允许的源
     */
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
     * Access-Control-Allow-Methods: CORS允许的HTTP方法
     */
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /**
     * Access-Control-Allow-Headers: CORS允许的请求头
     */
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    /**
     * Access-Control-Allow-Credentials: CORS是否允许携带认证信息
     */
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    /**
     * Access-Control-Max-Age: CORS预检请求的有效期
     */
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
}