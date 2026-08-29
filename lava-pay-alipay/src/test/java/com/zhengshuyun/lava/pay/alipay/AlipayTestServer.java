/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhengshuyun.lava.crypto.CryptoUtils;
import org.jspecify.annotations.Nullable;

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
    private static final String RESPONSE_TIMESTAMP = "1787976000000";
    private static final String RESPONSE_NONCE = "response-nonce-001";

    private final HttpServer server;
    private final ExecutorService executor;
    private final PrivateKey alipayPrivateKey;
    private final BlockingQueue<PlannedResponse> responses = new LinkedBlockingQueue<>();
    private final BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();

    private AlipayTestServer(
            HttpServer server,
            ExecutorService executor,
            PrivateKey alipayPrivateKey
    ) {
        this.server = server;
        this.executor = executor;
        this.alipayPrivateKey = alipayPrivateKey;
    }

    static AlipayTestServer start(PrivateKey alipayPrivateKey) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            AlipayTestServer result = new AlipayTestServer(
                    server,
                    executor,
                    alipayPrivateKey
            );
            server.createContext("/", result::handle);
            server.setExecutor(executor);
            server.start();
            return result;
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    URI baseUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    void enqueueSigned(String responseBody) {
        enqueueSigned(200, responseBody);
    }

    void enqueueSigned(int status, String responseBody) {
        String body = responseBody.strip();
        String source = RESPONSE_TIMESTAMP + "\n" + RESPONSE_NONCE + "\n" + body + "\n";
        String signature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(
                        alipayPrivateKey,
                        source.getBytes(StandardCharsets.UTF_8)
                )
        );
        responses.add(new PlannedResponse(
                status,
                body.getBytes(StandardCharsets.UTF_8),
                signature,
                false
        ));
    }

    void enqueueRaw(int status, String body) {
        responses.add(new PlannedResponse(
                status,
                body.getBytes(StandardCharsets.UTF_8),
                null,
                false
        ));
    }

    void enqueueRawSigned(int status, String body, String signature) {
        responses.add(new PlannedResponse(
                status,
                body.getBytes(StandardCharsets.UTF_8),
                signature,
                false
        ));
    }

    void enqueueDuplicateSignature(String responseBody) {
        String body = responseBody.strip();
        String source = RESPONSE_TIMESTAMP + "\n" + RESPONSE_NONCE + "\n" + body + "\n";
        String signature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(
                        alipayPrivateKey,
                        source.getBytes(StandardCharsets.UTF_8)
                )
        );
        responses.add(new PlannedResponse(
                200,
                body.getBytes(StandardCharsets.UTF_8),
                signature,
                true
        ));
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
            response = new PlannedResponse(
                    500,
                    "missing response".getBytes(StandardCharsets.UTF_8),
                    null,
                    false
            );
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.getResponseHeaders().set("alipay-trace-id", "trace-id-001");
        if (response.signature != null) {
            exchange.getResponseHeaders().set("alipay-timestamp", RESPONSE_TIMESTAMP);
            exchange.getResponseHeaders().set("alipay-nonce", RESPONSE_NONCE);
            exchange.getResponseHeaders().set("alipay-signature", response.signature);
            if (response.duplicateSignature) {
                exchange.getResponseHeaders().add("alipay-signature", "duplicate");
            }
        }
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

        String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @Nullable String header(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
                    return entry.getValue().getFirst();
                }
            }
            return null;
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

    private record PlannedResponse(
            int status,
            byte[] body,
            @Nullable String signature,
            boolean duplicateSignature
    ) {
    }
}
