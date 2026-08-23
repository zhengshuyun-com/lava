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

package com.zhengshuyun.lava.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpSseTest {
    private LocalEchoServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        server = LocalEchoServer.start();
        client = HttpClient.builder().callTimeout(Duration.ofSeconds(3)).build();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.close();
    }

    @Test
    void deliversEventsAndOneRemoteCloseTerminal() throws InterruptedException {
        CountDownLatch terminalLatch = new CountDownLatch(1);
        List<HttpSseEvent> events = new CopyOnWriteArrayList<>();
        AtomicInteger opens = new AtomicInteger();
        AtomicInteger terminals = new AtomicInteger();
        AtomicReference<HttpSseTerminal> terminal = new AtomicReference<>();

        HttpSseSession session = client.openSse(
                HttpRequest.get(server.baseUrl() + "/sse").build(), new HttpSseListener() {
                    @Override
                    public void onOpen(HttpSseSession current, HttpSseOpen open) {
                        opens.incrementAndGet();
                        assertEquals(200, open.statusCode());
                        assertEquals("text/event-stream", open.headers().get("Content-Type"));
                    }

                    @Override
                    public void onEvent(HttpSseSession current, HttpSseEvent event) {
                        events.add(event);
                    }

                    @Override
                    public void onTerminal(HttpSseSession current, HttpSseTerminal value) {
                        terminals.incrementAndGet();
                        terminal.set(value);
                        terminalLatch.countDown();
                    }
                });

        assertTrue(terminalLatch.await(2, TimeUnit.SECONDS));
        assertEquals(1, opens.get());
        assertEquals(List.of("first", "second"), events.stream().map(HttpSseEvent::data).toList());
        assertEquals(HttpSseEvent.DEFAULT_TYPE, events.getFirst().type());
        assertTrue(events.getFirst().isDefaultType());
        assertEquals("delta", events.get(1).type());
        assertEquals("2", events.get(1).id());
        assertEquals(1, terminals.get());
        assertEquals(HttpSseTermination.REMOTE_CLOSED, terminal.get().termination());
        assertNull(terminal.get().failure());
        assertEquals(HttpSseSession.State.REMOTE_CLOSED, session.getState());
        assertTrue(session.isClosed());
        assertFalse(session.isCancelled());
        session.close();
        assertEquals(1, terminals.get());
    }

    @Test
    void concurrentCancellationWinsExactlyOnce() throws Exception {
        CountDownLatch opened = new CountDownLatch(1);
        CountDownLatch terminalLatch = new CountDownLatch(1);
        AtomicInteger terminals = new AtomicInteger();
        AtomicReference<HttpSseTerminal> terminal = new AtomicReference<>();
        HttpSseSession session = client.openSse(
                HttpRequest.get(server.baseUrl() + "/sse-hold").build(), new HttpSseListener() {
                    @Override
                    public void onOpen(HttpSseSession current, HttpSseOpen open) {
                        opened.countDown();
                    }

                    @Override
                    public void onTerminal(HttpSseSession current, HttpSseTerminal value) {
                        terminals.incrementAndGet();
                        terminal.set(value);
                        terminalLatch.countDown();
                    }
                });
        assertTrue(opened.await(2, TimeUnit.SECONDS));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < 100; index++) {
                executor.submit(session::cancel);
            }
        }

        assertTrue(terminalLatch.await(2, TimeUnit.SECONDS));
        assertEquals(1, terminals.get());
        assertEquals(HttpSseTermination.CANCELLED, terminal.get().termination());
        assertEquals(HttpSseSession.State.CANCELLED, session.getState());
        assertTrue(session.isCancelled());
        server.releaseSse();
    }

    @Test
    void callbackExceptionCancelsSourceAndBecomesFailure() throws InterruptedException {
        CountDownLatch terminalLatch = new CountDownLatch(1);
        AtomicReference<HttpSseTerminal> terminal = new AtomicReference<>();
        HttpSseSession session = client.openSse(
                HttpRequest.get(server.baseUrl() + "/sse").build(), new HttpSseListener() {
                    @Override
                    public void onEvent(HttpSseSession current, HttpSseEvent event) {
                        throw new IllegalStateException("listener bug");
                    }

                    @Override
                    public void onTerminal(HttpSseSession current, HttpSseTerminal value) {
                        terminal.set(value);
                        terminalLatch.countDown();
                    }
                });

        assertTrue(terminalLatch.await(2, TimeUnit.SECONDS));
        assertEquals(HttpSseSession.State.FAILED, session.getState());
        assertEquals(HttpSseTermination.FAILED, terminal.get().termination());
        HttpSseFailure failure = terminal.get().failure();
        assertNotNull(failure);
        assertEquals(HttpFailureKind.IO, failure.kind());
        assertInstanceOf(IllegalStateException.class, failure.throwable());
        assertFalse(failure.toString().contains("listener bug"));
    }

    @Test
    void handshakeFailureIncludesBoundedContext() throws InterruptedException {
        CountDownLatch terminalLatch = new CountDownLatch(1);
        AtomicReference<HttpSseTerminal> terminal = new AtomicReference<>();
        client.openSse(HttpRequest.get(server.baseUrl() + "/sse-failure").build(),
                new HttpSseListener() {
                    @Override
                    public void onTerminal(HttpSseSession current, HttpSseTerminal value) {
                        terminal.set(value);
                        terminalLatch.countDown();
                    }
                });

        assertTrue(terminalLatch.await(2, TimeUnit.SECONDS));
        assertEquals(HttpSseTermination.FAILED, terminal.get().termination());
        HttpSseFailure failure = terminal.get().failure();
        assertNotNull(failure);
        assertEquals(HttpFailureKind.PROTOCOL, failure.kind());
        assertEquals(401, failure.statusCode());
        assertEquals("{\"error\":\"denied\"}", failure.responseBody());
        assertNotNull(failure.headers());
        assertFalse(failure.toString().contains("denied"));
    }

    @Test
    void clientCloseCancelsSessionAndRejectsEveryNewCall() throws InterruptedException {
        CountDownLatch opened = new CountDownLatch(1);
        CountDownLatch terminalLatch = new CountDownLatch(1);
        AtomicReference<HttpSseTerminal> terminal = new AtomicReference<>();
        HttpSseSession session = client.openSse(
                HttpRequest.get(server.baseUrl() + "/sse-hold").build(), new HttpSseListener() {
                    @Override
                    public void onOpen(HttpSseSession current, HttpSseOpen open) {
                        opened.countDown();
                    }

                    @Override
                    public void onTerminal(HttpSseSession current, HttpSseTerminal value) {
                        terminal.set(value);
                        terminalLatch.countDown();
                        throw new IllegalStateException("terminal callback is contained");
                    }
                });
        assertTrue(opened.await(2, TimeUnit.SECONDS));
        client.close();
        assertTrue(terminalLatch.await(2, TimeUnit.SECONDS));
        assertEquals(HttpSseTermination.CANCELLED, terminal.get().termination());
        assertEquals(HttpSseSession.State.CANCELLED, session.getState());

        HttpRequest request = HttpRequest.get(server.baseUrl() + "/ok").build();
        assertThrows(IllegalStateException.class, () -> client.send(request));
        assertThrows(IllegalStateException.class, () -> client.openStream(request));
        assertThrows(IllegalStateException.class,
                () -> client.openSse(request, new HttpSseListener() {
                }));
    }

    @Test
    void validatesTerminalInvariantsAndNormalizesDirectEvents() {
        HttpSseEvent event = new HttpSseEvent(null, " ", "data");
        assertEquals(HttpSseEvent.DEFAULT_TYPE, event.type());
        assertThrows(IllegalArgumentException.class,
                () -> new HttpSseTerminal(HttpSseTermination.FAILED, null));
        assertThrows(IllegalArgumentException.class,
                () -> new HttpSseTerminal(HttpSseTermination.CANCELLED,
                        new HttpSseFailure(HttpFailureKind.IO, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> new HttpSseTerminal(null, null));
    }
}
