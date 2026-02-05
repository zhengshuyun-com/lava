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

/**
 * HTTP 工具类
 * <p>
 * 提供全局单例 HttpClient 管理和便捷的静态方法执行 HTTP 请求. 
 *
 * <h2>职责</h2>
 * <ul>
 *   <li>管理全局单例 HttpClient (懒加载、线程安全) </li>
 *   <li>提供创建独立 HttpClient 的便捷方法</li>
 *   <li>执行 HTTP 请求</li>
 * </ul>
 *
 * <h2>使用方式</h2>
 * <pre>{@code
 * // 方式1：使用默认配置的单例 (推荐, 适用于大多数场景) 
 * HttpResponse response = HttpUtil.execute(
 *     HttpRequest.get("https://example.com").build()
 * );
 *
 * // 方式2：自定义单例配置 (应用启动时调用一次) 
 * HttpUtil.init(
 *     HttpClient.builder()
 *         .setConnectTimeout(Duration.ofSeconds(10))
 *         .setReadTimeout(Duration.ofSeconds(30))
 *         .build()
 * );
 *
 * // 方式3：创建独立的自定义 HttpClient (用于特殊需求) 
 * HttpClient customClient = HttpUtil.createHttpClientBuilder()
 *         .setConnectTimeout(Duration.ofSeconds(5))
 *         .setReadTimeout(Duration.ofSeconds(15))
 *         .build();
 * HttpResponse response = customClient.execute(request);
 * }</pre>
 *
 * <h2>线程安全</h2>
 * 使用双重检查锁 (Double-Checked Locking) 实现懒加载单例, 保证线程安全. 
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li><b>职责分离</b>：HttpUtil 管理客户端, HttpRequest 构建请求</li>
 *   <li><b>单例模式</b>：全局共享一个 HttpClient, 避免资源浪费</li>
 *   <li><b>灵活性</b>：支持创建独立实例, 满足特殊需求</li>
 * </ul>
 *
 * @author Toint
 * @since 2026/1/7
 */
public final class HttpUtil {

    /**
     * 全局单例 HttpClient
     * <p>
     * 使用 volatile 关键字确保：
     * <ul>
     *   <li>多线程环境下的可见性</li>
     *   <li>禁止指令重排序 (防止双重检查锁失效) </li>
     * </ul>
     */
    private static volatile HttpClient httpClient;

    /**
     * 私有构造函数, 防止实例化
     */
    private HttpUtil() {}

    /**
     * 初始化全局单例 HttpClient
     * <p>
     * 通常在应用启动时调用一次, 用于自定义 HttpClient 配置 (如超时时间、拦截器等) . 
     * 如果不调用此方法, 首次使用时会自动创建默认配置的单例. 
     * <p>
     * <b>注意：</b>只能初始化一次, 重复调用会抛出 IllegalArgumentException 异常. 
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 在应用启动时初始化
     * public class Application {
     *     public static void main(String[] args) {
     *         HttpUtil.init(
     *             HttpClient.builder()
     *                 .setConnectTimeout(Duration.ofSeconds(10))
     *                 .setReadTimeout(Duration.ofSeconds(30))
     *                 .build()
     *         );
     *
     *         // 后续使用
     *         HttpResponse response = HttpUtil.execute(request);
     *     }
     * }
     * }</pre>
     *
     * @param httpClient HttpClient 实例, 不能为 null
     * @throws IllegalArgumentException 如果 httpClient 为 null 或已经初始化过
     */
    public static void init(HttpClient httpClient) {
        synchronized (HttpUtil.class) {
            Validate.notNull(httpClient, "HttpClient must not be null");
            Validate.isNull(HttpUtil.httpClient, "HttpUtil is already initialized");
            HttpUtil.httpClient = httpClient;
        }
    }

    /**
     * 获取全局单例 HttpClient
     * <p>
     * 使用双重检查锁 (Double-Checked Locking) 实现懒加载：
     * <ul>
     *   <li>首次调用时使用默认配置创建单例</li>
     *   <li>后续调用直接返回已创建的实例, 性能高效</li>
     *   <li>线程安全, 多线程环境下保证只创建一个实例</li>
     * </ul>
     *
     * <h3>使用场景</h3>
     * <ul>
     *   <li>大多数 HTTP 请求场景 (通过 {@link #execute(HttpRequest)} 内部调用) </li>
     *   <li>需要直接访问 HttpClient 的场景 (如添加拦截器、获取连接池信息等) </li>
     * </ul>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 直接获取单例 (不常用) 
     * HttpClient client = HttpUtil.getHttpClient();
     * HttpResponse response = client.execute(request);
     *
     * // 推荐使用方式
     * HttpResponse response = HttpUtil.execute(request);
     * }</pre>
     *
     * @return 全局单例 HttpClient
     */
    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (HttpUtil.class) {
                if (httpClient == null) {
                    httpClient = HttpClient.builder().build();
                }
            }
        }
        return httpClient;
    }

    /**
     * 创建新的 HttpClient.Builder
     * <p>
     * 用于创建独立的、自定义配置的 HttpClient 实例. 
     * 每次调用返回一个新的 Builder, 可以链式配置各种参数. 
     *
     * <h3>使用场景</h3>
     * <ul>
     *   <li>需要不同超时配置的场景 (如快速接口 vs 慢速接口) </li>
     *   <li>需要不同拦截器的场景 (如不同的日志记录策略) </li>
     *   <li>需要不同代理配置的场景</li>
     *   <li>测试场景 (独立的客户端实例便于测试) </li>
     * </ul>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 场景1：创建快速接口的客户端 (短超时) 
     * HttpClient fastClient = HttpUtil.createHttpClientBuilder()
     *         .setConnectTimeout(Duration.ofSeconds(2))
     *         .setReadTimeout(Duration.ofSeconds(5))
     *         .build();
     *
     * // 场景2：创建慢速接口的客户端 (长超时) 
     * HttpClient slowClient = HttpUtil.createHttpClientBuilder()
     *         .setConnectTimeout(Duration.ofSeconds(10))
     *         .setReadTimeout(Duration.ofSeconds(60))
     *         .build();
     *
     * // 场景3：创建带自定义拦截器的客户端
     * HttpClient customClient = HttpUtil.createHttpClientBuilder()
     *         .addInterceptor(new LoggingInterceptor())
     *         .addInterceptor(new RetryInterceptor())
     *         .build();
     * }</pre>
     *
     * <h3>注意事项</h3>
     * <ul>
     *   <li>独立实例不共享全局单例的配置</li>
     *   <li>独立实例需要自行管理生命周期 (如关闭连接池) </li>
     *   <li>创建过多独立实例会消耗资源, 建议优先使用全局单例</li>
     * </ul>
     *
     * @return HttpClient.Builder 实例, 用于链式配置
     */
    public static HttpClient.Builder createHttpClientBuilder() {
        return HttpClient.builder();
    }

    /**
     * 使用全局单例 HttpClient 执行 HTTP 请求
     * <p>
     * 这是最常用的方法, 适用于大多数 HTTP 请求场景.
     * 内部使用 {@link #getHttpClient()} 获取单例执行请求.
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // GET 请求
     * HttpResponse response = HttpUtil.execute(
     *     HttpRequest.get("https://api.example.com/users")
     *         .setUserAgentBrowser()
     *         .setHeader("Accept", "application/json")
     *         .build()
     * );
     *
     * // POST JSON 请求
     * String json = "{\"name\":\"John\",\"age\":30}";
     * HttpResponse response = HttpUtil.execute(
     *     HttpRequest.post("https://api.example.com/users")
     *         .setJsonBody(json)
     *         .setBearerToken("your-token")
     *         .build()
     * );
     *
     * // 链式调用
     * String body = HttpUtil.execute(
     *     HttpRequest.get("https://api.example.com/users").build()
     * ).getBodyAsString();
     * }</pre>
     *
     * @param request HTTP 请求对象, 不能为 null
     * @return HTTP 响应对象
     * @throws HttpException 请求失败时抛出 (网络错误、超时、服务器错误等)
     * @throws IllegalArgumentException 如果 request 为 null
     */
    public static HttpResponse execute(HttpRequest request) {
        return getHttpClient().execute(request);
    }
}