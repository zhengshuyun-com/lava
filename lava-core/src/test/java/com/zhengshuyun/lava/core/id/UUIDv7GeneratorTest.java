/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.core.id;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UUIDv7GeneratorTest {

    @Test
    void producesRfc9562LayoutAndMonotonicValues() {
        MutableClock clock = new MutableClock(1_800_000_000_123L);
        UUIDv7Generator generator = new UUIDv7Generator(clock, zeroRandom());

        UUID first = generator.next();
        UUID second = generator.next();
        clock.setMillis(clock.millis() - 5_000);
        UUID afterRollback = generator.next();

        assertEquals(7, first.version());
        assertEquals(2, first.variant());
        assertEquals(1_800_000_000_123L, first.getMostSignificantBits() >>> 16);
        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(afterRollback) < 0);
        assertEquals(first.getMostSignificantBits() >>> 16,
                afterRollback.getMostSignificantBits() >>> 16);
    }

    @Test
    void isUniqueUnderConcurrentVirtualThreads() throws Exception {
        int count = 20_000;
        UUIDv7Generator generator = new UUIDv7Generator(
                Clock.fixed(Instant.ofEpochMilli(1_800_000_000_000L), ZoneOffset.UTC),
                zeroRandom());
        Set<UUID> values = ConcurrentHashMap.newKeySet();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Set<Future<?>> futures = new HashSet<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> values.add(generator.next())));
            }
            for (Future<?> future : futures) {
                assertTrue((Boolean) future.get());
            }
        }

        assertEquals(count, values.size());
    }

    @Test
    void distinguishesJdkUUIDAndUUIDv7() {
        UUID value = IdUtils.nextUUID();
        assertEquals(4, value.version());
        assertEquals(36, IdUtils.nextUUIDString().length());
        assertEquals(32, IdUtils.nextUUIDStringWithoutHyphens().length());

        UUID valueV7 = IdUtils.nextUUIDv7();
        assertEquals(7, valueV7.version());
        assertEquals(36, IdUtils.nextUUIDv7String().length());
        assertEquals(32, IdUtils.nextUUIDv7StringWithoutHyphens().length());
    }

    @Test
    void createsSnowflakeGeneratorWithExplicitWorkerId() {
        SnowflakeIdGenerator generator = IdUtils.newSnowflakeGenerator(37);

        assertEquals(37, generator.workerId());
    }

    private static SecureRandom zeroRandom() {
        return new SecureRandom() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }

            @Override
            public long nextLong() {
                return 0;
            }
        };
    }

    private static final class MutableClock extends Clock {
        private final AtomicLong millis;

        private MutableClock(long millis) {
            this.millis = new AtomicLong(millis);
        }

        void setMillis(long value) {
            millis.set(value);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public long millis() {
            return millis.get();
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }
    }
}
