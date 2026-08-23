/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zhengshuyun.lava.http.HttpRequest;
import com.zhengshuyun.lava.http.SseSession;
import com.zhengshuyun.lava.http.SseTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiClientTest {
    private HttpServer server;
    private AiClient ai;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/json", this::json);
        server.createContext("/stream", this::stream);
        server.start();
        ai = AiClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/").build();
    }

    @AfterEach
    void tearDown() {
        ai.close();
        server.stop(0);
    }

    @Test
    void sendsJsonAndDecodesResponse() {
        Reply reply = ai.sendJson(HttpRequest.post("json").build(), new Prompt("hello"), Reply.class);
        assertEquals("hello", reply.message());
    }

    @Test
    void decodesProtocolNeutralSseChunks() throws Exception {
        CountDownLatch terminal = new CountDownLatch(1);
        List<String> chunks = new CopyOnWriteArrayList<>();
        SseSession session = ai.openJsonStream(HttpRequest.post("stream").build(),
                new Prompt("hello"), event -> Optional.of(event.data()), new AiStreamListener<>() {
                    @Override
                    public void onChunk(SseSession current, String chunk) {
                        chunks.add(chunk);
                    }

                    @Override
                    public void onTerminal(SseSession current, SseTerminal value) {
                        terminal.countDown();
                    }
                });

        assertTrue(terminal.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("one", "two"), chunks);
        assertTrue(session.isClosed());
    }

    private void json(HttpExchange exchange) throws IOException {
        byte[] request = exchange.getRequestBody().readAllBytes();
        String body = new String(request, StandardCharsets.UTF_8);
        String message = body.contains("hello") ? "hello" : "missing";
        send(exchange, 200, "application/json", ("{\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8));
    }

    private void stream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write("data: one\n\ndata: two\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private record Prompt(String prompt) {
    }

    private record Reply(String message) {
    }
}
