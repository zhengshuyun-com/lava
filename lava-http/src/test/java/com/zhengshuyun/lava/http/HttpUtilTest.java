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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpUtil 单元测试
 * <p>
 * 测试 HttpUtil 的静态方法, 包括单例管理、实例创建、请求执行等功能.
 *
 * @author Toint
 * @since 2026/1/8
 */
@DisplayName("HttpUtil 单元测试")
class HttpUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtilTest.class);

    // URL 常量

    /**
     * 基础测试 URL - Postman Echo 服务 (用于实际HTTP请求测试)
     */
    private static final String BASE_URL = "https://postman-echo.com";

    /**
     * 示例 API URL (用于单元测试, 不发送实际请求)
     */
    private static final String EXAMPLE_API_URL = "https://example.com/api";

    /**
     * GET 请求测试 URL
     */
    private static final String GET_URL = BASE_URL + "/get";

    /**
     * POST 请求测试 URL
     */
    private static final String POST_URL = BASE_URL + "/post";

    /**
     * 每个测试方法执行前重置单例
     * <p>
     * 使用反射清空 HttpUtil 的单例实例, 确保每个测试独立运行
     */
    @BeforeEach
    void setUp() throws Exception {
        // 重置单例 (使用反射) 
        resetHttpUtilSingleton();
    }

    /**
     * 使用反射重置 HttpUtil 的单例实例
     */
    private void resetHttpUtilSingleton() throws Exception {
        Field field = HttpUtil.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(null, null);
    }

    /**
     * 使用反射获取当前单例实例
     */
    private HttpClient getSingletonInstance() throws Exception {
        Field field = HttpUtil.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        return (HttpClient) field.get(null);
    }

    // 单例管理测试

    @Nested
    @DisplayName("单例初始化测试")
    class SingletonInitializationTest {

        @Test
        @DisplayName("getHttpClient() - 首次调用应该创建单例")
        void testLazyInitialization() throws Exception {
            // 验证初始状态为 null
            assertNull(getSingletonInstance(), "初始状态单例应该为 null");

            // 首次调用应该创建单例
            HttpClient client1 = HttpUtil.getHttpClient();
            assertNotNull(client1, "首次调用应该创建单例");

            // 验证单例已被设置
            assertNotNull(getSingletonInstance(), "调用后单例应该不为 null");
        }

        @Test
        @DisplayName("getHttpClient() - 多次调用返回同一实例")
        void testMultipleCallsReturnSameInstance() {
            // 多次调用
            HttpClient client1 = HttpUtil.getHttpClient();
            HttpClient client2 = HttpUtil.getHttpClient();
            HttpClient client3 = HttpUtil.getHttpClient();

            // 验证都是同一个实例
            assertSame(client1, client2, "第二次调用应该返回同一个实例");
            assertSame(client1, client3, "第三次调用应该返回同一个实例");
        }

        @Test
        @DisplayName("initHttpClient() - 手动初始化成功")
        void testManualInitialization() throws Exception {
            // 验证初始状态
            assertNull(getSingletonInstance(), "初始状态单例应该为 null");

            // 手动初始化
            HttpClient customClient = HttpClient.builder()
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpUtil.initHttpClient(customClient);

            // 验证单例已被设置
            assertNotNull(getSingletonInstance(), "初始化后单例应该不为 null");

            // 验证获取的是初始化的实例
            HttpClient retrieved = HttpUtil.getHttpClient();
            assertSame(customClient, retrieved, "应该返回手动初始化的实例");
        }

        @Test
        @DisplayName("initHttpClient() - 传入 null 应该抛出异常")
        void testInitWithNull() {
            // 传入 null 应该抛出 IllegalArgumentException
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> HttpUtil.initHttpClient(null),
                    "传入 null 应该抛出 IllegalArgumentException"
            );

            // 验证异常消息
            assertTrue(exception.getMessage().contains("must not be null"),
                    "异常消息应该包含 'must not be null'");
        }

        @Test
        @DisplayName("initHttpClient() - 重复初始化应该抛出异常")
        void testDuplicateInitialization() {
            // 第一次初始化
            HttpClient client1 = HttpClient.builder().build();
            HttpUtil.initHttpClient(client1);

            // 第二次初始化应该抛出异常
            HttpClient client2 = HttpClient.builder().build();
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> HttpUtil.initHttpClient(client2),
                    "重复初始化应该抛出 IllegalArgumentException"
            );

            // 验证异常消息
            assertTrue(exception.getMessage().contains("already initialized"),
                    "异常消息应该包含 'already initialized'");
        }

        @Test
        @DisplayName("initHttpClient() - 在 getHttpClient() 之后不能再初始化")
        void testInitAfterGet() {
            // 先调用 getHttpClient() 触发懒加载
            HttpUtil.getHttpClient();

            // 再尝试初始化应该失败
            HttpClient client = HttpClient.builder().build();
            assertThrows(
                    IllegalArgumentException.class,
                    () -> HttpUtil.initHttpClient(client),
                    "懒加载后再初始化应该抛出异常"
            );
        }

        @Test
        @DisplayName("废弃方法 init() 兼容性测试")
        @SuppressWarnings("deprecation")
        void testDeprecatedInit() {
            // 测试废弃方法仍然可用
            HttpClient client = HttpClient.builder().build();
            HttpUtil.initHttpClient(client);
            assertNotNull(HttpUtil.getHttpClient());
        }

        @Test
        @DisplayName("线程安全 - 多线程同时获取单例")
        void testThreadSafeLazyInitialization() throws InterruptedException {
            int threadCount = 50;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<HttpClient> clients = Lists.newCopyOnWriteArrayList();

            // 创建多个线程同时获取单例
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        // 等待开始信号, 确保所有线程同时启动
                        startLatch.await();
                        HttpClient client = HttpUtil.getHttpClient();
                        clients.add(client);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            // 发出开始信号
            startLatch.countDown();

            // 等待所有线程完成
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "所有线程应该在 5 秒内完成");

            // 验证收集到的实例数量
            assertEquals(threadCount, clients.size(), "应该收集到所有线程的结果");

            // 验证所有实例都是同一个
            HttpClient first = clients.getFirst();
            for (HttpClient client : clients) {
                assertSame(first, client, "所有线程获取的应该是同一个实例");
            }
        }

        @Test
        @DisplayName("线程安全 - 多线程同时初始化 (只有一个成功) ")
        void testThreadSafeInit() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<Boolean> results = Lists.newCopyOnWriteArrayList();

            // 创建多个线程同时尝试初始化
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        HttpClient client = HttpClient.builder().build();
                        HttpUtil.initHttpClient(client);
                        results.add(true); // 初始化成功
                    } catch (IllegalArgumentException e) {
                        results.add(false); // 初始化失败
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

            // 验证结果：应该只有一个线程初始化成功
            long successCount = results.stream().filter(success -> success).count();
            assertEquals(1, successCount, "应该只有一个线程初始化成功");
            assertEquals(threadCount - 1, results.stream().filter(success -> !success).count(),
                    "其他线程应该失败");
        }
    }

    // 静态工厂方法测试

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryHttpMethodTest {

        @Test
        @DisplayName("httpClientBuilder() - 创建 Builder 实例")
        void testCreateDefaultInstance() {
            HttpClient.Builder builder = HttpUtil.httpClientBuilder();
            assertNotNull(builder, "应该成功创建 Builder");

            // 验证可以构建出 HttpClient
            HttpClient client = builder.build();
            assertNotNull(client, "Builder 应该能够构建出 HttpClient");
        }

        @Test
        @DisplayName("httpClientBuilder() - 每次创建不同的 HttpClient 实例")
        void testCreateMultipleDifferentInstances() {
            HttpClient client1 = HttpUtil.httpClientBuilder().build();
            HttpClient client2 = HttpUtil.httpClientBuilder().build();
            HttpClient client3 = HttpUtil.httpClientBuilder().build();

            // 验证都不是同一个实例
            assertNotSame(client1, client2, "不同调用应该创建不同实例");
            assertNotSame(client1, client3, "不同调用应该创建不同实例");
            assertNotSame(client2, client3, "不同调用应该创建不同实例");
        }

        @Test
        @DisplayName("httpClientBuilder() - 创建的实例与单例无关")
        void testCreateInstanceIsIndependent() {
            // 先获取单例
            HttpClient singleton = HttpUtil.getHttpClient();

            // 创建独立实例
            HttpClient independent = HttpUtil.httpClientBuilder().build();

            // 验证不是同一个实例
            assertNotSame(singleton, independent, "创建的实例应该独立于单例");
        }

        @Test
        @DisplayName("httpClientBuilder() - 多线程创建不同实例")
        void testConcurrentCreateDifferentInstances() throws InterruptedException {
            int threadCount = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<HttpClient> clients = Lists.newCopyOnWriteArrayList();

            // 多线程同时创建实例
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        HttpClient client = HttpUtil.httpClientBuilder().build();
                        clients.add(client);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

            // 验证收集到所有实例
            assertEquals(threadCount, clients.size(), "应该收集到所有线程创建的实例");

            // 验证所有实例都不相同
            for (int i = 0; i < clients.size(); i++) {
                for (int j = i + 1; j < clients.size(); j++) {
                    assertNotSame(clients.get(i), clients.get(j),
                            "每次创建的实例应该不同");
                }
            }
        }

        @Test
        @DisplayName("httpClientBuilder() - 支持自定义配置")
        void testCreateWithCustomConfig() {
            // 创建自定义配置的客户端
            HttpClient client = HttpUtil.httpClientBuilder()
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(10))
                    .build();

            assertNotNull(client, "应该成功创建自定义配置的客户端");

            // 验证与单例不同
            HttpClient singleton = HttpUtil.getHttpClient();
            assertNotSame(client, singleton, "自定义配置的客户端应该独立于单例");
        }
    }

    // HTTP 方法测试

    @Nested
    @DisplayName("HTTP 方法测试")
    class HttpHttpMethodTest {

        @Test
        @DisplayName("get() - 创建 GET 请求")
        void testGetRequest() {
            HttpRequest request = HttpRequest.get(GET_URL).build();
            assertNotNull(request, "应该成功创建 GET 请求");
            assertEquals(HttpMethod.GET, request.getMethod(), "请求方法应该是 GET");
            assertEquals(GET_URL, request.getUrl(), "URL 应该正确");
        }

        @Test
        @DisplayName("post() - 创建 POST 请求")
        void testPostRequest() {
            HttpRequest request = HttpRequest.post(POST_URL).build();
            assertNotNull(request, "应该成功创建 POST 请求");
            assertEquals(HttpMethod.POST, request.getMethod(), "请求方法应该是 POST");
            assertEquals(POST_URL, request.getUrl(), "URL 应该正确");
        }

        @Test
        @DisplayName("put() - 创建 PUT 请求")
        void testPutRequest() {
            HttpRequest request = HttpRequest.put(EXAMPLE_API_URL).build();
            assertNotNull(request, "应该成功创建 PUT 请求");
            assertEquals(HttpMethod.PUT, request.getMethod(), "请求方法应该是 PUT");
            assertEquals(EXAMPLE_API_URL, request.getUrl(), "URL 应该正确");
        }

        @Test
        @DisplayName("delete() - 创建 DELETE 请求")
        void testDeleteRequest() {
            HttpRequest request = HttpRequest.delete(EXAMPLE_API_URL).build();
            assertNotNull(request, "应该成功创建 DELETE 请求");
            assertEquals(HttpMethod.DELETE, request.getMethod(), "请求方法应该是 DELETE");
            assertEquals(EXAMPLE_API_URL, request.getUrl(), "URL 应该正确");
        }

        @Test
        @DisplayName("patch() - 创建 PATCH 请求")
        void testPatchRequest() {
            HttpRequest request = HttpRequest.patch(EXAMPLE_API_URL).build();
            assertNotNull(request, "应该成功创建 PATCH 请求");
            assertEquals(HttpMethod.PATCH, request.getMethod(), "请求方法应该是 PATCH");
            assertEquals(EXAMPLE_API_URL, request.getUrl(), "URL 应该正确");
        }

        @Test
        @DisplayName("head() - 创建 HEAD 请求")
        void testHeadRequest() {
            HttpRequest request = HttpRequest.head(EXAMPLE_API_URL).build();
            assertNotNull(request, "应该成功创建 HEAD 请求");
            assertEquals(HttpMethod.HEAD, request.getMethod(), "请求方法应该是 HEAD");
            assertEquals(EXAMPLE_API_URL, request.getUrl(), "URL 应该正确");
        }

        @Test
        @DisplayName("各种 HTTP 方法应该使用单例客户端")
        void testMethodsUseSingletonClient() throws Exception {
            // 先触发懒加载
            HttpUtil.getHttpClient();
            HttpClient singleton = getSingletonInstance();

            // 创建各种请求
            HttpRequest.get(GET_URL).build();
            HttpRequest.post(POST_URL).build();
            HttpRequest.put(EXAMPLE_API_URL).build();
            HttpRequest.delete(EXAMPLE_API_URL).build();

            // 验证单例没有变化
            assertSame(singleton, getSingletonInstance(),
                    "使用各种 HTTP 方法不应该改变单例");
        }

        @Test
        @DisplayName("传入空 URL 应该抛出异常")
        void testNullUrlThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.get(null).build());
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.post(null).build());
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.put(null).build());
            assertThrows(IllegalArgumentException.class, () -> HttpRequest.delete(null).build());
        }

        @Test
        @DisplayName("单例、独立实例和请求创建的组合使用")
        void testCombinedUsage() throws Exception {
            // 1. 创建请求 (不会触发单例初始化, 因为只是构建请求对象) 
            HttpRequest request1 = HttpRequest.get(GET_URL).build();

            // 2. 获取单例 (这会触发懒加载) 
            HttpClient singleton = HttpUtil.getHttpClient();
            assertNotNull(getSingletonInstance(), "获取单例应该触发单例初始化");

            // 3. 创建独立实例
            HttpClient independent1 = HttpUtil.httpClientBuilder().build();
            HttpClient independent2 = HttpUtil.httpClientBuilder().build();

            // 4. 再次创建请求
            HttpRequest request2 = HttpRequest.post(POST_URL).build();

            // 验证
            assertNotSame(independent1, singleton, "独立实例不应该等于单例");
            assertNotSame(independent2, singleton, "独立实例不应该等于单例");
            assertNotSame(independent1, independent2, "独立实例之间应该不同");
            assertSame(singleton, getSingletonInstance(), "单例应该保持不变");
        }

        @Test
        @DisplayName("不同顺序的方法调用")
        void testDifferentOrderOfCalls() throws Exception {
            // 顺序1: 先创建独立实例, 再获取单例
            resetHttpUtilSingleton();
            HttpClient independent1 = HttpUtil.httpClientBuilder().build();
            HttpClient singleton1 = HttpUtil.getHttpClient();
            assertNotSame(independent1, singleton1);

            // 顺序2: 先获取单例, 再创建独立实例
            resetHttpUtilSingleton();
            HttpClient singleton2 = HttpUtil.getHttpClient();
            HttpClient independent2 = HttpUtil.httpClientBuilder().build();
            assertNotSame(singleton2, independent2);

            // 顺序3: 先创建请求, 再获取单例
            resetHttpUtilSingleton();
            HttpRequest.get(GET_URL).build();
            HttpClient singleton3 = HttpUtil.getHttpClient();
            HttpClient independent3 = HttpUtil.httpClientBuilder().build();
            assertNotSame(singleton3, independent3);
        }

        @Test
        @DisplayName("并发获取单例和创建独立实例")
        void testConcurrentGetAndCreate() throws InterruptedException {
            int threadCount = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<HttpClient> singletons = Lists.newCopyOnWriteArrayList();
            List<HttpClient> independents = Lists.newCopyOnWriteArrayList();

            // 一半线程获取单例, 一半创建独立实例
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        if (index % 2 == 0) {
                            singletons.add(HttpUtil.getHttpClient());
                        } else {
                            independents.add(HttpUtil.httpClientBuilder().build());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

            // 验证所有单例都相同
            HttpClient firstSingleton = singletons.get(0);
            for (HttpClient singleton : singletons) {
                assertSame(firstSingleton, singleton);
            }

            // 验证所有独立实例都不同
            for (int i = 0; i < independents.size(); i++) {
                for (int j = i + 1; j < independents.size(); j++) {
                    assertNotSame(independents.get(i), independents.get(j));
                }
                // 独立实例也不应该等于单例
                assertNotSame(firstSingleton, independents.get(i));
            }
        }

        @Test
        @DisplayName("使用自定义配置的边界值")
        void testCustomConfigBoundaryValues() {
            // 测试极小超时值
            assertDoesNotThrow(() -> {
                HttpClient.builder()
                        .setConnectTimeout(Duration.ofMillis(1))
                        .setReadTimeout(Duration.ofMillis(1))
                        .setWriteTimeout(Duration.ofMillis(1))
                        .setCallTimeout(Duration.ofMillis(1))
                        .build();
            }, "最小超时值应该可以创建");

            // 测试极大超时值
            assertDoesNotThrow(() -> {
                HttpClient.builder()
                        .setConnectTimeout(Duration.ofHours(1))
                        .setReadTimeout(Duration.ofHours(1))
                        .setWriteTimeout(Duration.ofHours(1))
                        .setCallTimeout(Duration.ofHours(1))
                        .build();
            }, "最大超时值应该可以创建");
        }

        @Test
        @DisplayName("测试不同 HTTP 方法创建请求")
        void testDifferentHttpMethods() {
            // 使用预定义的URL常量, 避免硬编码
            List<HttpRequest> requests = ImmutableList.of(
                    HttpRequest.get(EXAMPLE_API_URL).build(),
                    HttpRequest.post(EXAMPLE_API_URL).build(),
                    HttpRequest.put(EXAMPLE_API_URL).build(),
                    HttpRequest.delete(EXAMPLE_API_URL).build(),
                    HttpRequest.patch(EXAMPLE_API_URL).build(),
                    HttpRequest.head(EXAMPLE_API_URL).build()
            );

            // 验证所有请求都创建成功
            for (HttpRequest request : requests) {
                assertNotNull(request, "请求应该创建成功");
                assertEquals(EXAMPLE_API_URL, request.getUrl(), "URL应该正确");
            }

            // 验证方法正确
            assertEquals(HttpMethod.GET, requests.get(0).getMethod());
            assertEquals(HttpMethod.POST, requests.get(1).getMethod());
            assertEquals(HttpMethod.PUT, requests.get(2).getMethod());
            assertEquals(HttpMethod.DELETE, requests.get(3).getMethod());
            assertEquals(HttpMethod.PATCH, requests.get(4).getMethod());
            assertEquals(HttpMethod.HEAD, requests.get(5).getMethod());
        }
    }

    // 执行请求测试

    @Nested
    @DisplayName("执行请求测试")
    class ExecuteRequestTest {

        @Test
        @DisplayName("execute() - 传入 null 应该抛出异常")
        void testExecuteWithNullRequest() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> HttpUtil.execute(null),
                    "传入 null request 应该抛出 IllegalArgumentException"
            );
        }

        @Test
        @DisplayName("execute() - 实际发送 GET 请求")
        void testExecuteGetRequest() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertTrue(response.isSuccessful(), "请求应该成功");
                String body = response.getBodyAsString();
                assertNotNull(body, "响应体不应该为 null");
                assertTrue(body.contains("\"url\""), "响应应该包含 URL 信息");
            }
        }

        @Test
        @DisplayName("execute() - 实际发送 POST 请求")
        void testExecutePostRequest() {
            HttpRequest request = HttpRequest.post(POST_URL)
                    .setJsonBody("{\"key\":\"value\"}")
                    .build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertTrue(response.isSuccessful(), "请求应该成功");
                String body = response.getBodyAsString();
                assertNotNull(body, "响应体不应该为 null");
                assertTrue(body.contains("\"json\""), "响应应该包含 JSON 数据");
            }
        }

        @Test
        @DisplayName("execute() - execute() 应该使用单例 HttpClient")
        void testExecuteUsesSingleton() throws Exception {
            resetHttpUtilSingleton();

            HttpUtil.getHttpClient();
            HttpClient singleton = getSingletonInstance();

            HttpRequest request = HttpRequest.get(GET_URL).build();
            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
            }

            assertSame(singleton, getSingletonInstance(),
                    "execute() 应该使用单例 HttpClient");
        }

        @Test
        @DisplayName("execute() - 多次调用使用同一个单例")
        void testMultipleExecuteCallsUseSameSingleton() throws Exception {
            resetHttpUtilSingleton();

            HttpUtil.getHttpClient();
            HttpClient singleton = getSingletonInstance();

            HttpUtil.execute(HttpRequest.get(GET_URL).build());
            HttpUtil.execute(HttpRequest.get(GET_URL).build());
            HttpUtil.execute(HttpRequest.get(GET_URL).build());

            assertSame(singleton, getSingletonInstance(),
                    "多次 execute() 应该使用同一个单例");
        }

        @Test
        @DisplayName("execute() - 响应状态码和消息正确")
        void testResponseCodeAndMessage() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertEquals(200, response.getCode(), "状态码应该是 200");
                assertNotNull(response.getMessage(), "状态消息不应该为 null");
            }
        }

        @Test
        @DisplayName("execute() - 响应头正确")
        void testResponseHeaders() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertNotNull(response.getHeaders(), "响应头不应该为 null");

                String contentType = response.getContentType();
                assertNotNull(contentType, "Content-Type 不应该为 null");
                assertTrue(contentType.contains("application/json"),
                        "Content-Type 应该包含 application/json");
            }
        }

        @Test
        @DisplayName("execute() - 请求头正确发送")
        void testRequestHeaders() {
            HttpRequest request = HttpRequest.get(GET_URL)
                    .setHeader("X-Custom-Header", "test-value")
                    .setUserAgentBrowser()
                    .build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                String body = response.getBodyAsString();
                assertTrue(body.contains("\"x-custom-header\""),
                        "服务器应该接收到自定义请求头");
                assertTrue(body.contains("\"user-agent\""),
                        "服务器应该接收到 User-Agent");
            }
        }

        @Test
        @DisplayName("execute() - POST 请求体正确发送")
        void testPostRequestBody() {
            String jsonData = "{\"name\":\"John\",\"age\":30}";
            HttpRequest request = HttpRequest.post(POST_URL)
                    .setJsonBody(jsonData)
                    .build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                String body = response.getBodyAsString();
                assertTrue(body.contains("\"name\""), "响应应该包含 name 字段");
                assertTrue(body.contains("John"), "响应应该包含 name 值");
                assertTrue(body.contains("\"age\""), "响应应该包含 age 字段");
                assertTrue(body.contains("30"), "响应应该包含 age 值");
            }
        }

        @Test
        @DisplayName("execute() - 响应体可以多次读取")
        void testResponseBodyMultipleReads() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");

                byte[] bytes1 = response.getBodyAsBytes();
                String string1 = response.getBodyAsString();
                byte[] bytes2 = response.getBodyAsBytes();
                String string2 = response.getBodyAsString();

                assertNotNull(bytes1, "第一次读取字节数组不应该为 null");
                assertNotNull(string1, "第一次读取字符串不应该为 null");
                assertNotNull(bytes2, "第二次读取字节数组不应该为 null");
                assertNotNull(string2, "第二次读取字符串不应该为 null");

                assertArrayEquals(bytes1, bytes2, "多次读取字节数组应该相同");
                assertEquals(string1, string2, "多次读取字符串应该相同");
            }
        }

        @Test
        @DisplayName("execute() - 响应元数据正确")
        void testResponseMetadata() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertNotNull(response.getMetadata(), "元数据不应该为 null");

                HttpCallMetadata metadata = response.getMetadata();
                assertNotNull(metadata.getRequestId(), "请求 ID 不应该为 null");
                assertNotNull(metadata.getRequestTime(), "请求时间不应该为 null");
                assertNotNull(metadata.getResponseTime(), "响应时间不应该为 null");
                assertEquals("GET", metadata.getMethod(), "方法应该是 GET");
                assertEquals(GET_URL, metadata.getUrl(), "URL 应该正确");
            }
        }

        @Test
        @DisplayName("HttpRequest.execute() - 链式调用使用全局单例")
        void testHttpRequestExecuteUsesGlobalSingleton() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response = request.execute()) {
                assertNotNull(response, "响应不应该为 null");
                assertTrue(response.isSuccessful(), "请求应该成功");
                String body = response.getBodyAsString();
                assertNotNull(body, "响应体不应该为 null");
            }
        }

        @Test
        @DisplayName("HttpRequest.execute() - POST 请求链式调用")
        void testHttpRequestExecutePost() {
            String jsonData = "{\"test\":\"data\"}";

            try (HttpResponse response = HttpRequest.post(POST_URL)
                    .setJsonBody(jsonData)
                    .build()
                    .execute()) {
                assertNotNull(response, "响应不应该为 null");
                assertTrue(response.isSuccessful(), "请求应该成功");
                String body = response.getBodyAsString();
                assertTrue(body.contains("\"test\""), "响应应该包含发送的数据");
            }
        }

        @Test
        @DisplayName("HttpRequest.execute(HttpClient) - 使用自定义 HttpClient")
        void testHttpRequestExecuteWithCustomClient() {
            HttpClient customClient = HttpUtil.httpClientBuilder()
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .build();

            try (HttpResponse response = HttpRequest.get(GET_URL)
                    .build()
                    .execute(customClient)) {
                assertNotNull(response, "响应不应该为 null");
                assertTrue(response.isSuccessful(), "请求应该成功");
            }
        }

        @Test
        @DisplayName("HttpRequest.execute(HttpClient) - 传入 null 应该抛出异常")
        void testHttpRequestExecuteWithNullClient() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> request.execute(null),
                    "传入 null HttpClient 应该抛出 IllegalArgumentException"
            );
        }

        @Test
        @DisplayName("HttpRequest.execute() - 多次调用同一请求")
        void testHttpRequestExecuteMultipleTimes() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response1 = request.execute()) {
                assertNotNull(response1, "第一次响应不应该为 null");
                assertTrue(response1.isSuccessful(), "第一次请求应该成功");
            }

            try (HttpResponse response2 = request.execute()) {
                assertNotNull(response2, "第二次响应不应该为 null");
                assertTrue(response2.isSuccessful(), "第二次请求应该成功");
            }
        }

        @Test
        @DisplayName("HttpRequest.execute() - 完整链式调用示例")
        void testHttpRequestExecuteFullChain() {
            try (HttpResponse response = HttpRequest.get(GET_URL)
                    .setHeader("X-Custom-Header", "test-value")
                    .setUserAgentBrowser()
                    .build()
                    .execute()) {

                assertNotNull(response, "响应不应该为 null");
                assertTrue(response.isSuccessful(), "请求应该成功");
                assertEquals(200, response.getCode(), "状态码应该是 200");

                String body = response.getBodyAsString();
                assertTrue(body.contains("\"x-custom-header\""), "服务器应该接收到自定义请求头");
            }
        }

        @Test
        @DisplayName("HttpRequest.execute(HttpClient) - 不同客户端配置对比")
        void testHttpRequestExecuteWithDifferentClients() {
            HttpRequest request = HttpRequest.get(GET_URL).build();

            HttpClient client1 = HttpUtil.httpClientBuilder()
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpClient client2 = HttpUtil.httpClientBuilder()
                    .setConnectTimeout(Duration.ofSeconds(10))
                    .build();

            try (HttpResponse response1 = request.execute(client1)) {
                assertNotNull(response1, "使用 client1 的响应不应该为 null");
                assertTrue(response1.isSuccessful(), "使用 client1 的请求应该成功");
            }

            try (HttpResponse response2 = request.execute(client2)) {
                assertNotNull(response2, "使用 client2 的响应不应该为 null");
                assertTrue(response2.isSuccessful(), "使用 client2 的请求应该成功");
            }
        }
    }

    // 配置验证测试

    @Nested
    @DisplayName("配置验证测试")
    class ConfigurationTest {

        @Test
        @DisplayName("自定义超时配置的客户端")
        void testCustomTimeoutConfiguration() throws Exception {
            resetHttpUtilSingleton();

            Duration customConnectTimeout = Duration.ofSeconds(5);
            Duration customReadTimeout = Duration.ofSeconds(10);

            HttpClient customClient = HttpClient.builder()
                    .setConnectTimeout(customConnectTimeout)
                    .setReadTimeout(customReadTimeout)
                    .build();

            HttpUtil.initHttpClient(customClient);

            HttpClient singleton = getSingletonInstance();
            assertNotNull(singleton, "单例不应该为 null");
            assertSame(customClient, singleton, "应该返回自定义配置的客户端");

            HttpRequest request = HttpRequest.get(GET_URL).build();
            try (HttpResponse response = singleton.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertTrue(response.isSuccessful(), "请求应该成功");
            }
        }

        @Test
        @DisplayName("极小超时配置的客户端")
        void testVerySmallTimeoutConfiguration() throws Exception {
            resetHttpUtilSingleton();

            assertDoesNotThrow(() -> {
                HttpClient client = HttpClient.builder()
                        .setConnectTimeout(Duration.ofMillis(1))
                        .setReadTimeout(Duration.ofMillis(1))
                        .setWriteTimeout(Duration.ofMillis(1))
                        .setCallTimeout(Duration.ofMillis(1))
                        .build();
                HttpUtil.initHttpClient(client);
            }, "极小超时配置应该可以创建");

            HttpClient singleton = getSingletonInstance();
            assertNotNull(singleton, "单例不应该为 null");
        }

        @Test
        @DisplayName("极大超时配置的客户端")
        void testVeryLargeTimeoutConfiguration() throws Exception {
            resetHttpUtilSingleton();

            assertDoesNotThrow(() -> {
                HttpClient client = HttpClient.builder()
                        .setConnectTimeout(Duration.ofHours(1))
                        .setReadTimeout(Duration.ofHours(1))
                        .setWriteTimeout(Duration.ofHours(1))
                        .setCallTimeout(Duration.ofHours(1))
                        .build();
                HttpUtil.initHttpClient(client);
            }, "极大超时配置应该可以创建");

            HttpClient singleton = getSingletonInstance();
            assertNotNull(singleton, "单例不应该为 null");
        }

        @Test
        @DisplayName("重定向配置验证")
        void testRedirectConfiguration() throws Exception {
            resetHttpUtilSingleton();

            HttpClient client = HttpClient.builder()
                    .setFollowRedirects(true)
                    .setFollowSslRedirects(false)
                    .build();
            HttpUtil.initHttpClient(client);

            assertNotNull(getSingletonInstance(), "单例不应该为 null");

            HttpRequest request = HttpRequest.get("https://postman-echo.com/redirect-to?url=https://postman-echo.com/get").build();
            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertEquals(200, response.getCode(), "重定向后状态码应该是 200");
            }
        }

        @Test
        @DisplayName("独立实例的配置不影响单例")
        void testIndependentInstanceConfigurationDoesNotAffectSingleton() throws Exception {
            resetHttpUtilSingleton();

            HttpClient singleton = HttpUtil.getHttpClient();

            HttpClient independent = HttpUtil.httpClientBuilder()
                    .setConnectTimeout(Duration.ofSeconds(1))
                    .setReadTimeout(Duration.ofSeconds(1))
                    .build();

            assertNotSame(singleton, independent, "独立实例不应该等于单例");

            HttpRequest request = HttpRequest.get(GET_URL).build();
            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "使用单例执行请求应该成功");
            }
        }
    }

    // 拦截器测试

    @Nested
    @DisplayName("拦截器测试")
    class InterceptorTest {

        @Test
        @DisplayName("添加拦截器后正确执行")
        void testAddInterceptor() throws Exception {
            resetHttpUtilSingleton();

            final boolean[] interceptorCalled = {false};

            HttpClient client = HttpClient.builder()
                    .addInterceptor(chain -> {
                        interceptorCalled[0] = true;
                        return chain.proceed(chain.request());
                    })
                    .build();
            HttpUtil.initHttpClient(client);

            HttpRequest request = HttpRequest.get(GET_URL).build();
            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertTrue(interceptorCalled[0], "拦截器应该被调用");
            }
        }

        @Test
        @DisplayName("添加多个拦截器按顺序执行")
        void testMultipleInterceptorsExecutionOrder() throws Exception {
            resetHttpUtilSingleton();

            final java.util.List<String> executionOrder = Lists.newArrayList();

            HttpClient client = HttpClient.builder()
                    .addInterceptor(chain -> {
                        executionOrder.add("interceptor1");
                        return chain.proceed(chain.request());
                    })
                    .addInterceptor(chain -> {
                        executionOrder.add("interceptor2");
                        return chain.proceed(chain.request());
                    })
                    .addInterceptor(chain -> {
                        executionOrder.add("interceptor3");
                        return chain.proceed(chain.request());
                    })
                    .build();
            HttpUtil.initHttpClient(client);

            HttpRequest request = HttpRequest.get(GET_URL).build();
            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                assertEquals(3, executionOrder.size(), "所有拦截器都应该被调用");
                assertEquals("interceptor1", executionOrder.get(0), "拦截器应该按添加顺序执行");
                assertEquals("interceptor2", executionOrder.get(1), "拦截器应该按添加顺序执行");
                assertEquals("interceptor3", executionOrder.get(2), "拦截器应该按添加顺序执行");
            }
        }

        @Test
        @DisplayName("拦截器可以修改请求")
        void testInterceptorCanModifyRequest() throws Exception {
            resetHttpUtilSingleton();

            HttpClient client = HttpClient.builder()
                    .addInterceptor(chain -> {
                        okhttp3.Request originalRequest = chain.request();
                        okhttp3.Request modifiedRequest = originalRequest.newBuilder()
                                .header("X-Modified-Header", "modified-value")
                                .build();
                        return chain.proceed(modifiedRequest);
                    })
                    .build();
            HttpUtil.initHttpClient(client);

            HttpRequest request = HttpRequest.get(GET_URL).build();
            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                String body = response.getBodyAsString();
                assertTrue(body.contains("\"x-modified-header\""),
                        "服务器应该接收到修改后的请求头");
            }
        }

        @Test
        @DisplayName("独立实例的拦截器不影响单例")
        void testIndependentInstanceInterceptorDoesNotAffectSingleton() throws Exception {
            resetHttpUtilSingleton();

            final boolean[] singletonInterceptorCalled = {false};
            final boolean[] independentInterceptorCalled = {false};

            HttpClient singletonClient = HttpClient.builder()
                    .addInterceptor(chain -> {
                        singletonInterceptorCalled[0] = true;
                        return chain.proceed(chain.request());
                    })
                    .build();
            HttpUtil.initHttpClient(singletonClient);

            HttpClient independentClient = HttpUtil.httpClientBuilder()
                    .addInterceptor(chain -> {
                        independentInterceptorCalled[0] = true;
                        return chain.proceed(chain.request());
                    })
                    .build();

            HttpRequest request = HttpRequest.get(GET_URL).build();

            try (HttpResponse response = independentClient.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
            }

            assertFalse(singletonInterceptorCalled[0],
                    "单例的拦截器不应该被独立实例调用");
            assertTrue(independentInterceptorCalled[0],
                    "独立实例的拦截器应该被调用");
        }

        @Test
        @DisplayName("拦截器修改请求头")
        void testInterceptorModifyHeaders() throws Exception {
            resetHttpUtilSingleton();

            HttpClient client = HttpClient.builder()
                    .addInterceptor(chain -> {
                        okhttp3.Request originalRequest = chain.request();
                        okhttp3.Request modifiedRequest = originalRequest.newBuilder()
                                .removeHeader("User-Agent")
                                .header("User-Agent", "Custom-Agent/1.0")
                                .header("X-Auth-Token", "secret-token")
                                .build();
                        return chain.proceed(modifiedRequest);
                    })
                    .build();
            HttpUtil.initHttpClient(client);

            HttpRequest request = HttpRequest.get(GET_URL).build();
            try (HttpResponse response = HttpUtil.execute(request)) {
                assertNotNull(response, "响应不应该为 null");
                String body = response.getBodyAsString();
                assertTrue(body.contains("Custom-Agent/1.0"),
                        "服务器应该接收到修改后的 User-Agent");
                assertTrue(body.contains("secret-token"),
                        "服务器应该接收到添加的认证令牌");
            }
        }
    }

    // 性能测试 (可选)

    @Nested
    @DisplayName("性能测试")
    @Tag("performance")
    class PerformanceTest {

        @Test
        @DisplayName("单例获取性能")
        @Disabled("性能测试, 按需执行")
        void testSingletonPerformance() {
            // 预热
            for (int i = 0; i < 1000; i++) {
                HttpUtil.getHttpClient();
            }

            int iterations = 1_000_000;
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                HttpUtil.getHttpClient();
            }

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // 转换为毫秒

            LOGGER.info("单例获取性能测试");
            LOGGER.info("总次数: {}", iterations);
            LOGGER.info("总耗时: {} ms", durationMs);
            LOGGER.info("平均耗时: {} ns", (double) (endTime - startTime) / iterations);
            LOGGER.info("每秒操作数: {}", (iterations * 1000L / durationMs));

            // 性能断言 (应该非常快) 
            assertTrue(durationMs < 1000, "100万次获取应该在1秒内完成");
        }

        @Test
        @DisplayName("创建独立实例性能 (Builder方式) ")
        @Disabled("性能测试, 按需执行")
        void testCreateInstancePerformance() {
            // 预热
            for (int i = 0; i < 100; i++) {
                HttpUtil.httpClientBuilder().build();
            }

            int iterations = 10_000;
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                HttpUtil.httpClientBuilder().build();
            }

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            LOGGER.info("创建实例性能测试");
            LOGGER.info("总次数: {}", iterations);
            LOGGER.info("总耗时: {} ms", durationMs);
            LOGGER.info("平均耗时: {} ms", (double) durationMs / iterations);
            LOGGER.info("每秒操作数: {}", (iterations * 1000L / durationMs));
        }

        @Test
        @DisplayName("并发获取单例性能")
        @Disabled("性能测试, 按需执行")
        void testConcurrentSingletonPerformance() throws InterruptedException {
            int threadCount = 10;
            int iterationsPerThread = 100_000;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            long startTime = System.nanoTime();

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            HttpUtil.getHttpClient();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS));

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            int totalOperations = threadCount * iterationsPerThread;

            LOGGER.info("并发获取单例性能测试");
            LOGGER.info("线程数: {}", threadCount);
            LOGGER.info("每线程操作数: {}", iterationsPerThread);
            LOGGER.info("总操作数: {}", totalOperations);
            LOGGER.info("总耗时: {} ms", durationMs);
            LOGGER.info("吞吐量: {} ops/s", (totalOperations * 1000L / durationMs));

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("对比单例与创建实例的性能 (Builder方式) ")
        @Disabled("性能测试, 按需执行")
        void testSingletonVsCreatePerformance() {
            int iterations = 10_000;

            // 测试单例性能
            long singletonStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                HttpUtil.getHttpClient();
            }
            long singletonEnd = System.nanoTime();
            long singletonDuration = (singletonEnd - singletonStart) / 1_000_000;

            // 测试创建实例性能 (使用 Builder 方式) 
            long createStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                HttpUtil.httpClientBuilder().build();
            }
            long createEnd = System.nanoTime();
            long createDuration = (createEnd - createStart) / 1_000_000;

            LOGGER.info("单例 vs 创建实例性能对比");
            LOGGER.info("操作次数: {}", iterations);
            LOGGER.info("单例总耗时: {} ms", singletonDuration);
            LOGGER.info("创建总耗时: {} ms", createDuration);
            LOGGER.info("性能差异: {}x", (createDuration / (double) singletonDuration));
            LOGGER.info("单例更快: {} ms", (createDuration - singletonDuration));

            // 单例应该显著更快
            assertTrue(singletonDuration < createDuration,
                    "单例应该比每次创建快");
        }
    }
}
