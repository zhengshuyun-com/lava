/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhengshuyun.lava.crypto.CryptoUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class AlipayTestServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;
    private final PrivateKey alipayPrivateKey;
    private final BlockingQueue<PlannedResponse> responses = new LinkedBlockingQueue<>();
    private final BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();

    private AlipayTestServer(HttpServer server, ExecutorService executor,
                                PrivateKey alipayPrivateKey) {
        this.server = server;
        this.executor = executor;
        this.alipayPrivateKey = alipayPrivateKey;
    }

    static AlipayTestServer start(PrivateKey alipayPrivateKey) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            AlipayTestServer result = new AlipayTestServer(
                    server, executor, alipayPrivateKey);
            server.createContext("/gateway.do", result::handle);
            server.setExecutor(executor);
            server.start();
            return result;
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    URI gatewayUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                + "/gateway.do");
    }

    void enqueueSigned(String method, String responseSource) {
        String root = method.replace('.', '_') + "_response";
        enqueueSignedRoot(root, responseSource);
    }

    void enqueueSignedError(String responseSource) {
        enqueueSignedRoot("error_response", responseSource);
    }

    void enqueueRaw(int status, String body) {
        responses.add(new PlannedResponse(status, body.getBytes(StandardCharsets.UTF_8)));
    }

    CapturedRequest takeRequest() {
        try {
            CapturedRequest result = requests.poll(5, TimeUnit.SECONDS);
            if (result == null) {
                throw new AssertionError("server did not receive a request");
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private void enqueueSignedRoot(String root, String responseSource) {
        responseSource = responseSource.strip();
        String signature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(alipayPrivateKey,
                        responseSource.getBytes(StandardCharsets.UTF_8)));
        String body = "{\"" + root + "\":" + responseSource
                + ",\"sign\":\"" + signature + "\"}";
        enqueueRaw(200, body);
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        requests.add(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                Map.copyOf(exchange.getRequestHeaders()),
                requestBody
        ));
        PlannedResponse response = responses.poll();
        if (response == null) {
            response = new PlannedResponse(500,
                    "missing response".getBytes(StandardCharsets.UTF_8));
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.getResponseHeaders().set("trace_id", "trace-id-001");
        exchange.sendResponseHeaders(response.status, response.body.length);
        exchange.getResponseBody().write(response.body);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.close();
    }

    record CapturedRequest(
            String method,
            String target,
            Map<String, List<String>> headers,
            byte[] body
    ) {
        Map<String, String> queryParams() {
            int question = target.indexOf('?');
            return question < 0 ? Map.of() : parseForm(target.substring(question + 1));
        }

        Map<String, String> formParams() {
            return parseForm(new String(body, StandardCharsets.UTF_8));
        }

        private static Map<String, String> parseForm(String value) {
            Map<String, String> result = new LinkedHashMap<>();
            if (value.isEmpty()) {
                return result;
            }
            for (String item : value.split("&")) {
                String[] pair = item.split("=", 2);
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String fieldValue = pair.length == 1 ? ""
                        : URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                result.put(name, fieldValue);
            }
            return result;
        }
    }

    private record PlannedResponse(int status, byte[] body) {
    }
}
