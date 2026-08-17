/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.schedule;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TriggerTest {

    @Test
    void oneShotAndFixedRateUseInstantAndDuration() {
        Instant first = Instant.parse("2026-08-17T00:00:00Z");
        Trigger oneShot = Trigger.at(first);
        Trigger fixed = Trigger.fixedRate(first, Duration.ofMinutes(5));

        assertEquals(first, oneShot.firstFireTime(first.minusSeconds(1)));
        assertNull(oneShot.nextExecutionAfter(first));
        assertEquals(first, oneShot.nextExecutionAfter(first.minusNanos(1)));
        assertEquals(first.plus(Duration.ofMinutes(5)), fixed.nextExecutionAfter(first));
    }

    @Test
    void relativeTriggersCalculateFromTheSuppliedClockInstant() {
        Instant now = Instant.parse("2026-08-17T00:00:00Z");

        assertEquals(now.plusSeconds(4), Trigger.after(Duration.ofSeconds(4)).firstFireTime(now));
        assertEquals(now.plusSeconds(2),
                Trigger.fixedRate(Duration.ofSeconds(2), Duration.ofSeconds(3)).firstFireTime(now));
    }

    @Test
    void cronDefaultsToUtcAndHonorsExplicitZone() {
        Instant after = Instant.parse("2026-08-17T00:00:00Z");

        assertEquals(
                Instant.parse("2026-08-17T02:00:00Z"),
                Trigger.cron("0 0 2 * * ?").nextExecutionAfter(after));
        assertEquals(
                Instant.parse("2026-08-17T18:00:00Z"),
                Trigger.cron("0 0 2 * * ?", ZoneId.of("Asia/Shanghai"))
                        .nextExecutionAfter(after));
    }

    @Test
    void cronUsesZoneRulesAcrossDstGap() {
        Trigger berlin = Trigger.cron("0 30 2 * * ?", ZoneId.of("Europe/Berlin"));

        assertEquals(
                Instant.parse("2026-03-30T00:30:00Z"),
                berlin.nextExecutionAfter(Instant.parse("2026-03-28T02:00:00Z")));
    }

    @Test
    void invalidTriggerValuesFailImmediately() {
        assertThrows(IllegalArgumentException.class, () -> Trigger.cron("bad cron"));
        assertThrows(IllegalArgumentException.class, () -> Trigger.cron(" "));
        assertThrows(IllegalArgumentException.class, () -> Trigger.after(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> Trigger.fixedRate(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> Trigger.at(null));
    }

    @Test
    void relativeTriggerOverflowIsReportedAsScheduleFailure() {
        assertThrows(
                ScheduleException.class,
                () -> Trigger.after(Duration.ofSeconds(Long.MAX_VALUE))
                        .firstFireTime(Instant.EPOCH));
    }
}
