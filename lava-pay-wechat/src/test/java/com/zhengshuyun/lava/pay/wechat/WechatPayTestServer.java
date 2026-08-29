/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhengshuyun.lava.crypto.CryptoUtils;
import com.zhengshuyun.lava.http.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class WechatPayTestServer implements AutoCloseable {
    static final String PUBLIC_KEY_ID = "PUB_KEY_ID_1234567890";
    static final String RESPONSE_NONCE = "response-nonce";

    private final HttpServer server;
    private final ExecutorService executor;
    private final PrivateKey responsePrivateKey;
    private final Clock clock;
    private final BlockingQueue<PlannedResponse> responses = new LinkedBlockingQueue<>();
    private final BlockingQueue<CapturedRequest> requests = new LinkedBlockingQueue<>();

    private WechatPayTestServer(
            HttpServer server,
            ExecutorService executor,
            PrivateKey responsePrivateKey,
            Clock clock
    ) {
        this.server = server;
        this.executor = executor;
        this.responsePrivateKey = responsePrivateKey;
        this.clock = clock;
    }

    static WechatPayTestServer start(PrivateKey responsePrivateKey, Clock clock) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            WechatPayTestServer result = new WechatPayTestServer(
                    server,
                    executor,
                    responsePrivateKey,
                    clock
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
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + '/');
    }

    void enqueueSigned(int status, String body) {
        enqueueSigned(
                status,
                body.getBytes(StandardCharsets.UTF_8),
                PUBLIC_KEY_ID,
                responsePrivateKey,
                clock.instant().getEpochSecond()
        );
    }

    void enqueueSigned(
            int status,
            byte[] body,
            String serial,
            PrivateKey privateKey,
            long timestamp
    ) {
        responses.add(new PlannedResponse(
                status,
                body.clone(),
                "application/json",
                true,
                serial,
                privateKey,
                timestamp
        ));
    }

    void enqueueUnsigned(int status, byte[] body, String contentType) {
        responses.add(new PlannedResponse(
                status,
                body.clone(),
                contentType,
                false,
                "",
                responsePrivateKey,
                0
        ));
    }

    CapturedRequest takeRequest() {
        try {
            CapturedRequest request = requests.poll(5, TimeUnit.SECONDS);
            if (request == null) {
                throw new AssertionError("server did not receive a request");
            }
            return request;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    static HttpHeaders signedHeaders(
            byte[] body,
            PrivateKey key,
            String serial,
            long timestamp
    ) {
        String signature = Base64.getEncoder().encodeToString(
                CryptoUtils.rsaSha256Sign(key,
                        responseMessage(timestamp, RESPONSE_NONCE, body)));
        return HttpHeaders.of(
                "Wechatpay-Serial",
                serial,
                "Wechatpay-Signature",
                signature,
                "Wechatpay-Timestamp",
                Long.toString(timestamp),
                "Wechatpay-Nonce",
                RESPONSE_NONCE,
                "Wechatpay-Signature-Type",
                "WECHATPAY2-SHA256-RSA2048"
        );
    }

    @Override
    public void close() {
        server.stop(0);
        executor.close();
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
                    "missing test response".getBytes(StandardCharsets.UTF_8),
                    "text/plain",
                    false,
                    "",
                    responsePrivateKey,
                    0
            );
        }
        exchange.getResponseHeaders().set("Content-Type", response.contentType);
        exchange.getResponseHeaders().set("Request-ID", "request-id-001");
        if (response.signed) {
            HttpHeaders signatureHeaders = signedHeaders(
                    response.body,
                    response.privateKey,
                    response.serial,
                    response.timestamp
            );
            for (String name : signatureHeaders.names()) {
                exchange.getResponseHeaders().set(name, signatureHeaders.get(name));
            }
        }
        if (response.status == 204) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            exchange.sendResponseHeaders(response.status, response.body.length);
            exchange.getResponseBody().write(response.body);
        }
        exchange.close();
    }

    private static byte[] responseMessage(long timestamp, String nonce, byte[] body) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(body.length + 64);
        output.writeBytes(Long.toString(timestamp).getBytes(StandardCharsets.UTF_8));
        output.write('\n');
        output.writeBytes(nonce.getBytes(StandardCharsets.UTF_8));
        output.write('\n');
        output.writeBytes(body);
        output.write('\n');
        return output.toByteArray();
    }

    record CapturedRequest(
            String method,
            String target,
            Map<String, List<String>> headers,
            byte[] body
    ) {
        String header(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(entry -> entry.getValue().getFirst())
                    .findFirst()
                    .orElse(null);
        }
    }

    private record PlannedResponse(
            int status,
            byte[] body,
            String contentType,
            boolean signed,
            String serial,
            PrivateKey privateKey,
            long timestamp
    ) {
    }
}
