/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** 只监听回环地址的确定性测试服务器。 */
final class LocalEchoServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;
    private final String baseUrl;
    private final CountDownLatch releaseSse = new CountDownLatch(1);
    private final CountDownLatch slowStarted = new CountDownLatch(2);

    private LocalEchoServer(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    static LocalEchoServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);
            LocalEchoServer result = new LocalEchoServer(server, executor);
            result.installContexts();
            server.start();
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("could not start test server", exception);
        }
    }

    String baseUrl() {
        return baseUrl;
    }

    boolean awaitSlowRequest(Duration timeout) throws InterruptedException {
        return slowStarted.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    void releaseSse() {
        releaseSse.countDown();
    }

    private void installContexts() {
        server.createContext("/ok", exchange -> send(exchange, 200,
                "text/plain; charset=utf-8", "hello".getBytes(StandardCharsets.UTF_8), true));
        server.createContext("/error", exchange -> send(exchange, 503,
                "text/plain", "unavailable".getBytes(StandardCharsets.UTF_8), true));
        server.createContext("/charset", exchange -> {
            Charset gbk = Charset.forName("GBK");
            send(exchange, 200, "text/plain; charset=GBK", "中文".getBytes(gbk), true);
        });
        server.createContext("/json", exchange -> send(exchange, 200,
                "application/json", "{\"message\":\"ok\"}".getBytes(StandardCharsets.UTF_8), true));
        server.createContext("/echo", this::echo);
        server.createContext("/large", exchange -> send(exchange, 200,
                "application/octet-stream", new byte[128], true));
        server.createContext("/large-chunked", exchange -> send(exchange, 200,
                "application/octet-stream", new byte[128], false));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", baseUrl + "/ok");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/cookies", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "a=1; Path=/");
            exchange.getResponseHeaders().add("Set-Cookie", "quoted=\"two\"; Secure");
            send(exchange, 200, "text/plain", new byte[0], true);
        });
        server.createContext("/slow", exchange -> {
            slowStarted.countDown();
            try {
                Thread.sleep(600);
                send(exchange, 200, "text/plain", "done".getBytes(StandardCharsets.UTF_8), true);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });
        server.createContext("/sse", exchange -> send(exchange, 200,
                "text/event-stream", "data: first\n\nevent: delta\nid: 2\ndata: second\n\n"
                        .getBytes(StandardCharsets.UTF_8), false));
        server.createContext("/sse-headers", exchange -> {
            String accept = exchange.getRequestHeaders().getFirst(HttpHeaderNames.ACCEPT);
            String lastEventId = exchange.getRequestHeaders().getFirst(HttpHeaderNames.LAST_EVENT_ID);
            String event = "data: " + accept + '|' + lastEventId + "\n\n";
            send(exchange, 200, "text/event-stream",
                    event.getBytes(StandardCharsets.UTF_8), false);
        });
        server.createContext("/sse-hold", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
                output.flush();
                releaseSse.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.createContext("/sse-failure", exchange -> send(exchange, 401,
                "application/json", "{\"error\":\"denied\"}".getBytes(StandardCharsets.UTF_8), true));
    }

    private void echo(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream input = exchange.getRequestBody()) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String query = exchange.getRequestURI().getRawQuery();
        String custom = exchange.getRequestHeaders().getFirst("X-Test");
        String response = exchange.getRequestMethod() + '|'
                + (query == null ? "" : query) + '|'
                + (custom == null ? "" : custom) + '|' + body;
        send(exchange, 200, "text/plain; charset=utf-8",
                response.getBytes(StandardCharsets.UTF_8), true);
    }

    private static void send(HttpExchange exchange, int status, String contentType,
                             byte[] body, boolean fixedLength) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, fixedLength ? body.length : 0);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        releaseSse();
        server.stop(0);
        executor.shutdownNow();
    }
}
