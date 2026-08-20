/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.http;

import okhttp3.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class HttpClientTest {
    private LocalEchoServer server;

    @BeforeEach
    void startServer() {
        server = LocalEchoServer.start();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void executesBufferedRequestsAndBuildsCredentialSafeMetadata() {
        try (HttpClient client = HttpClient.builder().build()) {
            HttpRequest request = HttpRequest.post(server.baseUrl()
                            + "/echo?existing=yes&access_token=url-secret")
                    .addQueryParam("q", "中文 value")
                    .addQueryParam("tag", "a")
                    .addQueryParam("tag", "b")
                    .header("X-Test", "works")
                    .bearerToken("header-secret")
                    .textBody("payload")
                    .build();

            HttpResponse response = client.send(request);
            assertEquals(200, response.getCode());
            assertTrue(response.isSuccessful());
            assertFalse(response.isRedirect());
            assertTrue(response.getBodyAsString().startsWith("POST|"));
            assertTrue(response.getBodyAsString().contains("q=%E4%B8%AD%E6%96%87%20value"));
            assertTrue(response.getBodyAsString().contains("tag=a&tag=b"));
            assertTrue(response.getBodyAsString().endsWith("|works|payload"));
            assertArrayEquals(response.getBodyAsString().getBytes(StandardCharsets.UTF_8),
                    response.getBodyAsBytes());
            assertEquals("text/plain; charset=utf-8", response.getContentType());
            assertNull(response.getLocation());
            assertEquals("missing", response.getHeaderOrDefault("No-Such", "missing"));
            assertEquals("http/1.1", response.getProtocol());

            HttpCallMetadata metadata = response.getMetadata();
            assertNotNull(metadata.getRequestId());
            assertTrue(metadata.getDuration().compareTo(Duration.ZERO) >= 0);
            assertEquals(metadata.getDurationMillis(), metadata.getDuration().toMillis());
            assertEquals("[REDACTED]", metadata.getRequestHeaders().get("Authorization"));
            assertFalse(metadata.getUrl().contains("url-secret"));
            assertFalse(metadata.toString().contains("header-secret"));
            assertEquals(200, metadata.getStatusCode());
            assertTrue(metadata.isSuccessful());
            assertEquals("POST", metadata.getMethod());
            assertNotNull(metadata.getRequestTime());
            assertNotNull(metadata.getResponseTime());
            assertEquals("http/1.1", metadata.getProtocol());
        }
    }

    @Test
    void returnsHttpErrorsAndHonorsCharsetRedirectAndCookies() {
        try (HttpClient client = HttpClient.builder().followRedirects(false).build()) {
            HttpResponse error = client.send(HttpRequest.get(server.baseUrl() + "/error").build());
            assertEquals(503, error.getCode());
            assertFalse(error.isSuccessful());
            assertEquals("unavailable", error.getBodyAsString());

            HttpResponse charset = client.send(HttpRequest.get(server.baseUrl() + "/charset").build());
            assertEquals(Charset.forName("GBK"), charset.getCharset());
            assertEquals("中文", charset.getBodyAsString());
            assertEquals("����", charset.getBodyAsString(StandardCharsets.UTF_8));

            HttpResponse redirect = client.send(HttpRequest.get(server.baseUrl() + "/redirect").build());
            assertTrue(redirect.isRedirect());
            assertEquals(server.baseUrl() + "/ok", redirect.getLocation());

            HttpResponse cookies = client.send(HttpRequest.get(server.baseUrl() + "/cookies").build());
            assertEquals(Map.of("a", "1", "quoted", "two"), cookies.getCookies());
            assertEquals("1", cookies.getCookie("a"));
            assertNull(cookies.getCookie("none"));
            assertThrows(IllegalArgumentException.class, () -> cookies.getCookie(" "));
        }
    }

    @Test
    void enforcesDeclaredAndChunkedBufferLimitsButAllowsExplicitStreaming() throws IOException {
        try (HttpClient client = HttpClient.builder().maxBufferedResponseBytes(32).build()) {
            HttpException declared = assertThrows(HttpException.class,
                    () -> client.send(HttpRequest.get(server.baseUrl() + "/large").build()));
            assertEquals(HttpFailureKind.RESPONSE_TOO_LARGE, declared.getKind());
            HttpException chunked = assertThrows(HttpException.class,
                    () -> client.send(HttpRequest.get(server.baseUrl() + "/large-chunked").build()));
            assertEquals(HttpFailureKind.RESPONSE_TOO_LARGE, chunked.getKind());

            try (HttpStream response = client.openStream(
                    HttpRequest.get(server.baseUrl() + "/large-chunked").build())) {
                assertEquals(-1, response.contentLength());
                assertEquals(128, response.body().readAllBytes().length);
                assertEquals(200, response.statusCode());
                assertTrue(response.isSuccessful());
                assertFalse(response.isRedirect());
                assertNotNull(response.headers());
                assertEquals("application/octet-stream", response.header("Content-Type"));
                assertEquals(StandardCharsets.UTF_8, response.charset());
                assertNotNull(response.metadata());
                assertEquals("http/1.1", response.protocol());
                assertTrue(response.toString().contains("HttpCallMetadata"));
            }
        }
    }

    @Test
    void largeBufferLimitDoesNotPreallocateTheWholeLimit() throws IOException {
        try (HttpClient client = HttpClient.builder()
                .maxBufferedResponseBytes(Integer.MAX_VALUE - 1)
                .build()) {
            HttpResponse response = client.send(HttpRequest.get(server.baseUrl() + "/ok").build());
            assertEquals("hello", response.bodyString());
        }
    }

    @Test
    void classifiesTimeoutConnectionDnsAndTlsFailures() throws IOException {
        try (HttpClient client = HttpClient.builder().build()) {
            RequestOptions timeout = RequestOptions.builder()
                    .callTimeout(Duration.ofMillis(40))
                    .connectTimeout(Duration.ofSeconds(1))
                    .readTimeout(Duration.ofSeconds(1))
                    .writeTimeout(Duration.ofSeconds(1))
                    .build();
            HttpException timedOut = assertThrows(HttpException.class,
                    () -> client.send(HttpRequest.get(server.baseUrl() + "/slow").build(), timeout));
            assertEquals(HttpFailureKind.TIMEOUT, timedOut.getKind());
        }

        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        try (HttpClient client = HttpClient.builder().build()) {
            HttpException refused = assertThrows(HttpException.class,
                    () -> client.send(HttpRequest.get("http://127.0.0.1:" + unusedPort).build()));
            assertEquals(HttpFailureKind.CONNECTION, refused.getKind());
        }

        HttpClient.Builder dnsBuilder = HttpClient.builder();
        OkHttpInterop.customize(dnsBuilder, nativeBuilder -> nativeBuilder
                .proxy(java.net.Proxy.NO_PROXY)
                .dns(hostname -> {
                    throw new UnknownHostException("cause-must-not-enter-message");
                }));
        try (HttpClient client = dnsBuilder.build()) {
            HttpException dns = assertThrows(HttpException.class,
                    () -> client.send(HttpRequest.get(
                            "http://unresolvable.invalid/path?token=do-not-leak").build()));
            assertEquals(HttpFailureKind.DNS, dns.getKind());
            assertEquals("GET", dns.getMethod());
            assertFalse(dns.getMessage().contains("do-not-leak"));
            assertFalse(dns.toString().contains("cause-must-not-enter-message"));
            assertFalse(dns.getUrl().contains("do-not-leak"));
        }

        HttpClient.Builder tlsBuilder = HttpClient.builder();
        OkHttpInterop.addInterceptor(tlsBuilder, chain -> {
            throw new SSLHandshakeException("deterministic-local-tls-failure");
        });
        try (HttpClient client = tlsBuilder.build()) {
            HttpException tls = assertThrows(HttpException.class,
                    () -> client.send(HttpRequest.get(server.baseUrl() + "/ok").build()));
            assertEquals(HttpFailureKind.TLS, tls.getKind());
        }
    }

    @Test
    void transportFailuresDiscardUnsafeCausesButRetainTheirType() {
        String causeSecret = "cause-secret-value";
        HttpClient.Builder builder = HttpClient.builder();
        OkHttpInterop.addInterceptor(builder, chain -> {
            IOException failure = new IOException(
                    "failed at https://example.test/?accessToken=" + causeSecret);
            failure.addSuppressed(new IllegalStateException("suppressed-" + causeSecret));
            failure.setStackTrace(new StackTraceElement[]{new StackTraceElement(
                    "UnsafeCause", "call", "clientSecret=" + causeSecret, 1)});
            throw failure;
        });

        try (HttpClient client = builder.build()) {
            HttpException failure = assertThrows(HttpException.class, () -> client.send(
                    HttpRequest.get(server.baseUrl() + "/ok?clientSecret=request-secret"
                            + "#accessToken=fragment-request-secret").build()));

            assertEquals(HttpFailureKind.IO, failure.getKind());
            assertEquals(IOException.class.getName(), failure.getTransportCauseType());
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("request-secret"));

            StringWriter rendered = new StringWriter();
            failure.printStackTrace(new PrintWriter(rendered));
            assertFalse(rendered.toString().contains(causeSecret));
            assertFalse(rendered.toString().contains("request-secret"));
            assertFalse(rendered.toString().contains("fragment-request-secret"));
        }
    }

    @Test
    void borrowedCloseLeavesSharedResourcesAndUnrelatedCallsAlive() throws Exception {
        OkHttpClient shared = new OkHttpClient.Builder().build();
        HttpClient borrowed = OkHttpInterop.borrowed(shared);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<String> unrelated = CompletableFuture.supplyAsync(
                    () -> nativeGet(shared, server.baseUrl() + "/slow"), executor);
            CompletableFuture<HttpFailureKind> wrapped = CompletableFuture.supplyAsync(() -> {
                try {
                    borrowed.send(HttpRequest.get(server.baseUrl() + "/slow").build());
                    throw new AssertionError("wrapped call unexpectedly completed");
                } catch (HttpException exception) {
                    return exception.getKind();
                }
            }, executor);

            assertTrue(server.awaitSlowRequest(Duration.ofSeconds(2)));
            borrowed.close();

            assertEquals(HttpFailureKind.CANCELLED, wrapped.get(2, TimeUnit.SECONDS));
            assertEquals("done", unrelated.get(2, TimeUnit.SECONDS));
            assertFalse(shared.dispatcher().executorService().isShutdown());
            assertEquals("hello", nativeGet(shared, server.baseUrl() + "/ok"));
            assertThrows(IllegalStateException.class,
                    () -> borrowed.send(HttpRequest.get(server.baseUrl() + "/ok").build()));
        } finally {
            shared.dispatcher().executorService().shutdown();
            shared.connectionPool().evictAll();
        }
    }

    @Test
    void ownedClientsCloseResourcesIdempotently() {
        OkHttpClient nativeClient = new OkHttpClient.Builder().build();
        HttpClient owned = OkHttpInterop.owned(nativeClient, 64);
        assertSame(nativeClient, OkHttpInterop.unwrap(owned));
        assertEquals(64, owned.maxBufferedResponseBytes());
        owned.close();
        owned.close();
        assertTrue(owned.isClosed());
        assertTrue(nativeClient.dispatcher().executorService().isShutdown());

        HttpClient built = HttpClient.builder().build();
        OkHttpClient builtNative = OkHttpInterop.unwrap(built);
        built.close();
        assertTrue(builtNative.dispatcher().executorService().isShutdown());
    }

    @Test
    void interopConcentratesInterceptorCookieAndNativeBodyExtensions() {
        HttpClient.Builder builder = HttpClient.builder();
        assertSame(builder, OkHttpInterop.addInterceptor(builder,
                chain -> chain.proceed(chain.request().newBuilder().header("X-Test", "interop").build())));
        assertSame(builder, OkHttpInterop.cookieJar(builder, CookieJar.NO_COOKIES));
        try (HttpClient client = builder.build()) {
            RequestBody nativeBody = RequestBody.create("native".getBytes(StandardCharsets.UTF_8),
                    MediaType.parse("text/plain"));
            HttpRequest.Builder requestBuilder = HttpRequest.post(server.baseUrl() + "/echo");
            assertSame(requestBuilder, OkHttpInterop.requestBody(requestBuilder, nativeBody));
            HttpResponse response = client.send(requestBuilder.build());
            assertTrue(response.getBodyAsString().endsWith("|interop|native"));
        }
    }

    private static String nativeGet(OkHttpClient client, String url) {
        try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
            return response.body().string();
        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
    }
}
