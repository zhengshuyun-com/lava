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
package com.zhengshuyun.lava.core.id;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    void requiresExplicitValidWorkerAndKeepsWorkersIsolated() {
        Clock clock = Clock.fixed(SnowflakeIdGenerator.LAVA_EPOCH.plusSeconds(1), ZoneOffset.UTC);
        SnowflakeIdGenerator first = new SnowflakeIdGenerator(1, clock);
        SnowflakeIdGenerator second = new SnowflakeIdGenerator(2, clock);

        long firstId = first.nextId();
        long secondId = second.nextId();

        assertNotEquals(firstId, secondId);
        assertEquals(1, (firstId >>> 12) & 1023);
        assertEquals(2, (secondId >>> 12) & 1023);
        assertEquals(SnowflakeIdGenerator.LAVA_EPOCH.plusSeconds(1),
                SnowflakeIdGenerator.timestamp(firstId));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1024));
    }

    @Test
    void failsObservablyOnSequenceExhaustionAndRecoversWhenClockAdvances() {
        MutableClock clock = new MutableClock(SnowflakeIdGenerator.LAVA_EPOCH.toEpochMilli());
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7, clock);
        for (int i = 0; i < 4096; i++) {
            assertEquals(i, generator.nextId() & 4095);
        }

        IdGenerationException failure =
                assertThrows(IdGenerationException.class, generator::nextId);
        assertTrue(failure.getMessage().contains(
                "Snowflake sequence exhausted at Unix millisecond " + clock.millis()));

        clock.addMillis(1);
        assertEquals(0, generator.nextId() & 4095);
    }

    @Test
    void rejectsClockRollbackWithoutWaiting() {
        MutableClock clock = new MutableClock(SnowflakeIdGenerator.LAVA_EPOCH.toEpochMilli() + 10);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock);
        generator.nextId();
        clock.addMillis(-3);

        IdGenerationException failure =
                assertThrows(IdGenerationException.class, generator::nextId);
        assertTrue(failure.getMessage().contains("Clock moved backwards by 3 ms"));
    }

    private static final class MutableClock extends Clock {
        private final AtomicLong millis;

        private MutableClock(long millis) {
            this.millis = new AtomicLong(millis);
        }

        void addMillis(long delta) {
            millis.addAndGet(delta);
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
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }

        @Override
        public long millis() {
            return millis.get();
        }
    }
}
