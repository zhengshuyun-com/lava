/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OAuth2AccessTokenProviderTest {
    @Test
    void refreshIsSingleFlightAndHonorsRefreshAhead() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-17T00:00:00Z"));
        AtomicInteger fetches = new AtomicInteger();
        OAuth2TokenClient client = (credential, requestClock) -> {
            int sequence = fetches.incrementAndGet();
            return new OAuth2AccessToken(
                    "token-" + sequence, requestClock.instant().plus(Duration.ofMinutes(5)));
        };
        OAuth2AccessTokenProvider provider = new OAuth2AccessTokenProvider(
                credential(), client, clock, Duration.ofMinutes(1));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<String>> calls = new ArrayList<>();
            for (int index = 0; index < 32; index++) {
                calls.add(provider::accessToken);
            }
            List<Future<String>> results = executor.invokeAll(calls);
            for (Future<String> result : results) {
                assertEquals("token-1", result.get());
            }
        }
        assertEquals(1, fetches.get());

        clock.advance(Duration.ofMinutes(4));
        assertEquals("token-2", provider.accessToken());
        assertEquals(2, fetches.get());
    }

    @Test
    void responseWithoutExpiryIsNeverAssumedCacheable() {
        AtomicInteger fetches = new AtomicInteger();
        OAuth2TokenClient client = (credential, clock) ->
                new OAuth2AccessToken("token-" + fetches.incrementAndGet(), null);
        OAuth2AccessTokenProvider provider = new OAuth2AccessTokenProvider(
                credential(), client, Clock.systemUTC(), Duration.ZERO);

        assertEquals("token-1", provider.accessToken());
        assertEquals("token-2", provider.accessToken());
    }

    @Test
    void closingProviderClosesPrivateClientAndRejectsFurtherUse() {
        AtomicInteger closes = new AtomicInteger();
        OAuth2TokenClient client = new OAuth2TokenClient() {
            @Override
            public OAuth2AccessToken fetchAccessToken(
                    OAuth2RefreshTokenCredential credential, Clock clock) {
                return new OAuth2AccessToken("token", clock.instant().plusSeconds(60));
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };
        OAuth2AccessTokenProvider provider = new OAuth2AccessTokenProvider(
                credential(), client, Clock.systemUTC(), Duration.ZERO);

        provider.close();
        provider.close();

        assertEquals(1, closes.get());
        assertThrows(IllegalStateException.class, provider::accessToken);
    }

    @Test
    void closeWaitsForInFlightSingleFlightRefreshBeforeClosingClient() throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        AtomicBoolean clientClosed = new AtomicBoolean();
        OAuth2TokenClient client = new OAuth2TokenClient() {
            @Override
            public OAuth2AccessToken fetchAccessToken(
                    OAuth2RefreshTokenCredential credential, Clock clock) {
                fetchStarted.countDown();
                try {
                    assertTrue(releaseFetch.await(2, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return new OAuth2AccessToken("token", clock.instant().plusSeconds(60));
            }

            @Override
            public void close() {
                clientClosed.set(true);
            }
        };
        OAuth2AccessTokenProvider provider = new OAuth2AccessTokenProvider(
                credential(), client, Clock.systemUTC(), Duration.ZERO);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> token = executor.submit(provider::accessToken);
            assertTrue(fetchStarted.await(2, TimeUnit.SECONDS));
            Future<?> close = executor.submit(provider::close);
            assertFalse(clientClosed.get());
            releaseFetch.countDown();
            assertEquals("token", token.get(2, TimeUnit.SECONDS));
            close.get(2, TimeUnit.SECONDS);
        }

        assertTrue(clientClosed.get());
        assertThrows(IllegalStateException.class, provider::accessToken);
    }

    @Test
    void tokenReuseHandlesBoundaryAndClockOverflowAsRefreshRequired() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC);
        OAuth2AccessToken token = new OAuth2AccessToken(
                "token", clock.instant().plus(Duration.ofMinutes(1)));
        assertTrue(token.reusable(clock, Duration.ofSeconds(59)));
        assertFalse(token.reusable(clock, Duration.ofMinutes(1)));
        assertFalse(new OAuth2AccessToken("token", null).reusable(clock, Duration.ZERO));

        Clock maximum = Clock.fixed(Instant.MAX, ZoneOffset.UTC);
        assertFalse(new OAuth2AccessToken("token", Instant.MAX)
                .reusable(maximum, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new OAuth2AccessToken("bad\ntoken", null));
        assertThrows(IllegalArgumentException.class,
                () -> new OAuth2AccessToken("bad token", null));
    }

    private static OAuth2RefreshTokenCredential credential() {
        return new OAuth2RefreshTokenCredential(
                "user@example.com", "client", "refresh",
                URI.create("https://login.example.com/token"), List.of("mail"), null);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return zone.equals(ZoneOffset.UTC) ? this : Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
