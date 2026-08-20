/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.core.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationFormatterTest {

    @Test
    void formatsOnlyExactDurationUnits() {
        DurationFormatter formatter = DurationFormatter.builder()
                .range(ChronoUnit.DAYS, ChronoUnit.MILLIS)
                .build();

        assertEquals("2d 3h 4min 5s 6ms", formatter.format(
                Duration.ofDays(2).plusHours(3).plusMinutes(4).plusSeconds(5).plusMillis(6)));
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.builder().largestUnit(ChronoUnit.MONTHS).build());
        assertThrows(IllegalArgumentException.class, () ->
                DurationFormatter.builder().largestUnit(ChronoUnit.YEARS).build());
    }

    @Test
    void supportsPrecisionLocaleAndZeroValuesWithoutCalendarApproximations() {
        DurationFormatter nanos = DurationFormatter.builder()
                .range(ChronoUnit.SECONDS, ChronoUnit.NANOS)
                .locale(Locale.SIMPLIFIED_CHINESE)
                .showZeroValues(true)
                .build();

        assertEquals("1秒 0毫秒 2微秒 3纳秒", nanos.format(Duration.ofSeconds(1).plusNanos(2003)));
        assertEquals("0s", DurationFormatter.builder().build().format(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> DurationFormatter.builder().build().format(Duration.ofSeconds(-1)));
    }

    @Test
    void publicFormattersUseStrictResolution() {
        assertEquals(LocalDate.of(2028, 2, 29), LocalDate.parse("2028-02-29", DateTimeFormatterUtils.DATE));
        assertThrows(DateTimeParseException.class,
                () -> LocalDate.parse("2027-02-29", DateTimeFormatterUtils.DATE));
    }

    @Test
    void rejectsNanosecondAdditionOverflowInsteadOfWrapping() {
        long seconds = Long.MAX_VALUE / 1_000_000_000L;
        int nanos = (int) (Long.MAX_VALUE % 1_000_000_000L) + 1;
        Duration duration = Duration.ofSeconds(seconds, nanos);
        DurationFormatter formatter = DurationFormatter.builder()
                .range(ChronoUnit.MICROS, ChronoUnit.NANOS)
                .build();

        assertThrows(ArithmeticException.class, () -> formatter.format(duration));
    }
}
