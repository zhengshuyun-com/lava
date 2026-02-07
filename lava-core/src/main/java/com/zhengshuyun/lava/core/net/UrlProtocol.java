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

package com.zhengshuyun.lava.core.net;

import com.google.common.base.Objects;
import com.zhengshuyun.lava.core.lang.Validate;

import java.util.Locale;

/**
 * URL 协议类型
 * <p>
 * 参考 Spring Framework 的 HttpMethod 实现，使用常量 + 实例的方案，
 * 支持扩展自定义协议。
 *
 * <h3>标准协议</h3>
 * <ul>
 *   <li>{@link #HTTP} - HTTP 协议</li>
 *   <li>{@link #HTTPS} - HTTPS 协议</li>
 *   <li>{@link #WS} - WebSocket 协议</li>
 *   <li>{@link #WSS} - Secure WebSocket 协议</li>
 *   <li>{@link #FTP} - FTP 协议</li>
 *   <li>{@link #FILE} - 文件协议</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 使用常量
 * UrlProtocol protocol = UrlProtocol.HTTPS;
 *
 * // 自定义协议
 * UrlProtocol custom = UrlProtocol.of("grpc");
 * }</pre>
 *
 * @author Toint
 * @since 2026/1/18
 */
public final class UrlProtocol {

    /**
     * HTTP 协议
     */
    public static final UrlProtocol HTTP = new UrlProtocol("http");

    /**
     * HTTPS 协议
     */
    public static final UrlProtocol HTTPS = new UrlProtocol("https");

    /**
     * WebSocket 协议
     */
    public static final UrlProtocol WS = new UrlProtocol("ws");

    /**
     * 安全 WebSocket 协议
     */
    public static final UrlProtocol WSS = new UrlProtocol("wss");

    /**
     * FTP 协议
     */
    public static final UrlProtocol FTP = new UrlProtocol("ftp");

    /**
     * 文件协议
     */
    public static final UrlProtocol FILE = new UrlProtocol("file");

    /**
     * 协议名称
     */
    private final String name;

    private UrlProtocol(String name) {
        Validate.notBlank(name, "protocol scheme must not be blank");
        this.name = name;
    }

    /**
     * 创建协议实例
     *
     * @param name 协议字符串
     * @return 协议实例
     */
    public static UrlProtocol of(String name) {
        Validate.notBlank(name, "protocol name must not be blank");
        String normalized = name.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "http" -> HTTP;
            case "https" -> HTTPS;
            case "ws" -> WS;
            case "wss" -> WSS;
            case "ftp" -> FTP;
            case "file" -> FILE;
            default -> new UrlProtocol(normalized);
        };
    }

    /**
     * 获取协议名称
     *
     * @return 协议名称
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        UrlProtocol that = (UrlProtocol) object;
        return Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
