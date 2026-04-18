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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSE 单元测试.
 *
 * @author Toint
 * @since 2026/4/18
 */
@DisplayName("HttpSse 单元测试")
class HttpSseTest {
    /** 本地测试服务 */
    private HttpServer httpServer;

    /** 本地测试地址 */
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        resetHttpUtilSingleton();

        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/success", this::handleSuccessSse);
        httpServer.createContext("/failure", this::handleFailureResponse);
        httpServer.createContext("/cancel", this::handleCancelSse);
        httpServer.start();
        baseUrl = "http://127.0.0.1:" + httpServer.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    @DisplayName("HttpClient.executeSse() - 正常接收 SSE 事件")
    void testExecuteSseByClient() throws Exception {
        HttpClient httpClient = HttpClient.builder()
                .setReadTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.get(baseUrl + "/success")
                .setHeader(HttpHeaders.ACCEPT, "text/event-stream")
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        List<HttpSseEvent> events = new CopyOnWriteArrayList<>();
        AtomicReference<Integer> statusCodeRef = new AtomicReference<>();
        AtomicReference<String> contentTypeRef = new AtomicReference<>();

        HttpSseSession session = httpClient.executeSse(request, new HttpSseListener() {
            @Override
            public void onOpen(HttpSseSession session, HttpSseOpen open) {
                statusCodeRef.set(open.statusCode());
                contentTypeRef.set(open.headers().get("Content-Type"));
            }

            @Override
            public void onEvent(HttpSseSession session, HttpSseEvent event) {
                events.add(event);
            }

            @Override
            public void onClosed(HttpSseSession session) {
                latch.countDown();
            }
        });

        assertFalse(session.isClosed(), "SSE 会话初始不应该关闭");
        assertTrue(latch.await(3, TimeUnit.SECONDS), "SSE 应该在 3 秒内完成");
        assertEquals(200, statusCodeRef.get());
        assertEquals("text/event-stream", contentTypeRef.get());
        assertEquals(2, events.size());
        assertEquals("status", events.get(0).type());
        assertEquals("{\"text\":\"start\"}", events.get(0).data());
        assertEquals("result", events.get(1).type());
        assertEquals("{\"ok\":true}", events.get(1).data());
        assertTrue(session.isClosed(), "SSE 结束后会话应该关闭");
    }

    @Test
    @DisplayName("HttpRequest.executeSse() - 使用全局单例执行 SSE 请求")
    void testExecuteSseByRequest() throws Exception {
        HttpRequest request = HttpRequest.get(baseUrl + "/success")
                .setHeader(HttpHeaders.ACCEPT, "text/event-stream")
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpSseEvent> resultEventRef = new AtomicReference<>();

        HttpSseSession session = request.executeSse(new HttpSseListener() {
            @Override
            public void onEvent(HttpSseSession session, HttpSseEvent event) {
                if ("result".equals(event.type())) {
                    resultEventRef.set(event);
                }
            }

            @Override
            public void onClosed(HttpSseSession session) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS), "SSE 应该在 3 秒内完成");
        assertNotNull(resultEventRef.get(), "应该收到 result 事件");
        assertEquals("{\"ok\":true}", resultEventRef.get().data());
        assertTrue(session.isClosed(), "SSE 结束后会话应该关闭");
    }

    @Test
    @DisplayName("HttpClient.executeSse() - HTTP 非 2xx 时触发失败回调")
    void testExecuteSseFailure() throws Exception {
        HttpClient httpClient = HttpClient.builder().build();
        HttpRequest request = HttpRequest.get(baseUrl + "/failure").build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> statusCodeRef = new AtomicReference<>();
        AtomicReference<String> responseBodyRef = new AtomicReference<>();
        AtomicReference<String> contentTypeRef = new AtomicReference<>();

        httpClient.executeSse(request, new HttpSseListener() {
            @Override
            public void onFailure(HttpSseSession session, HttpSseFailure failure) {
                statusCodeRef.set(failure.statusCode());
                responseBodyRef.set(failure.responseBody());
                contentTypeRef.set(failure.headers() == null ? null : failure.headers().get("Content-Type"));
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS), "失败回调应该在 3 秒内触发");
        assertEquals(500, statusCodeRef.get());
        assertEquals("internal-error", responseBodyRef.get());
        assertEquals("text/plain; charset=UTF-8", contentTypeRef.get());
    }

    @Test
    @DisplayName("HttpSseSession.cancel() - 主动取消后不再继续接收后续事件")
    void testCancelSseSession() throws Exception {
        HttpClient httpClient = HttpClient.builder()
                .setReadTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.get(baseUrl + "/cancel")
                .setHeader(HttpHeaders.ACCEPT, "text/event-stream")
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger();

        HttpSseSession session = httpClient.executeSse(request, new HttpSseListener() {
            @Override
            public void onEvent(HttpSseSession session, HttpSseEvent event) {
                if (eventCount.incrementAndGet() == 1) {
                    session.cancel();
                }
            }

            @Override
            public void onClosed(HttpSseSession session) {
                latch.countDown();
            }

            @Override
            public void onFailure(HttpSseSession session, HttpSseFailure failure) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS), "取消后应该尽快结束会话");
        assertEquals(1, eventCount.get(), "取消后不应该再收到后续事件");
        assertTrue(session.isClosed(), "取消后会话应该关闭");
    }

    /**
     * 重置 HttpUtil 单例.
     */
    private void resetHttpUtilSingleton() throws Exception {
        Field field = HttpUtil.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(null, null);
    }

    /**
     * 成功 SSE 响应.
     */
    private void handleSuccessSse(HttpExchange exchange) throws java.io.IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write("event: status\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("data: {\"text\":\"start\"}\n\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            outputStream.write("event: result\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("data: {\"ok\":true}\n\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    /**
     * 失败 HTTP 响应.
     */
    private void handleFailureResponse(HttpExchange exchange) throws java.io.IOException {
        byte[] responseBody = "internal-error".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(500, responseBody.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        }
    }

    /**
     * 可取消 SSE 响应.
     */
    private void handleCancelSse(HttpExchange exchange) throws java.io.IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write("event: status\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("data: first\n\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            try {
                Thread.sleep(500);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            outputStream.write("event: status\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("data: second\n\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (java.io.IOException ignored) {
            // 客户端取消连接后, 服务端写流失败属于预期行为.
        }
    }
}
