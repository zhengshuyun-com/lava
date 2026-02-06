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

import com.zhengshuyun.common.core.lang.Validate;
import okhttp3.*;
import okhttp3.Authenticator;

import java.io.IOException;
import java.net.*;
import java.util.List;

/**
 * @author Toint
 * @since 2026/1/9
 */
public final class HttpProxy {

    private final ProxySelector proxySelector;
    private final Authenticator authenticator;

    private HttpProxy(Builder builder) {
        this.proxySelector = builder.proxySelector;
        this.authenticator = builder.authenticator;
    }

    public ProxySelector getProxySelector() {
        return proxySelector;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * 创建固定代理 (无认证)
     */
    public static HttpProxy of(String host, int port) {
        return builder()
                .setHttp(host, port)
                .build();
    }

    /**
     * 创建固定代理 (带认证)
     */
    public static HttpProxy of(String host, int port, String username, String password) {
        return builder()
                .setHttp(host, port)
                .setAuth(username, password)
                .build();
    }

    /**
     * 创建 SOCKS 代理
     */
    public static HttpProxy socks(String host, int port) {
        return builder()
                .setSocks(host, port)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ProxySelector proxySelector;
        private Authenticator authenticator;

        /**
         * 设置 HTTP 代理
         */
        public Builder setHttp(String host, int port) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            this.proxySelector = new FixedProxySelector(proxy);
            return this;
        }

        /**
         * 设置 SOCKS 代理
         */
        public Builder setSocks(String host, int port) {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
            this.proxySelector = new FixedProxySelector(proxy);
            return this;
        }

        /**
         * 设置代理认证
         */
        public Builder setAuth(String username, String password) {
            this.authenticator = new ProxyAuthenticator(username, password);
            return this;
        }

        /**
         * 自定义 ProxySelector
         */
        public Builder setProxySelector(ProxySelector proxySelector) {
            this.proxySelector = proxySelector;
            return this;
        }

        /**
         * 自定义 Authenticator
         */
        public Builder setAuthenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        public HttpProxy build() {
            return new HttpProxy(this);
        }
    }

    /**
     * 固定代理选择器
     */
    private static class FixedProxySelector extends ProxySelector {

        private final Proxy proxy;

        FixedProxySelector(Proxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public List<Proxy> select(URI uri) {
            return List.of(proxy);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            // 连接失败时的处理 (可以记录日志) 
        }
    }

    /**
     * 代理认证器
     */
    private static class ProxyAuthenticator implements Authenticator {

        private final String username;
        private final String password;

        ProxyAuthenticator(String username, String password) {
            Validate.notNull(username, "username must not be null");
            Validate.notNull(password, "password must not be null");
            this.username = username;
            this.password = password;
        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            // 只处理代理认证 (407) 
            if (response.code() != 407) {
                return null;
            }

            // 避免无限重试
            if (responseCount(response) >= 3) {
                return null;
            }

            String credential = Credentials.basic(username, password);
            return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
        }

        private int responseCount(Response response) {
            int result = 1;
            while ((response = response.priorResponse()) != null) {
                result++;
            }
            return result;
        }
    }
}
