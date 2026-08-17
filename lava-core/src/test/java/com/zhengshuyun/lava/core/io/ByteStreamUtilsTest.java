/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.core.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ByteStreamUtilsTest {

    @Test
    void borrowedStreamsAreNeverClosedOrFlushed() throws Exception {
        TrackingInput input = new TrackingInput("lava".getBytes(StandardCharsets.UTF_8));
        TrackingOutput output = new TrackingOutput();

        assertEquals(4, ByteStreamUtils.copy(input, output));
        assertEquals("lava", output.toString(StandardCharsets.UTF_8));
        assertFalse(input.closed);
        assertFalse(output.closed);
        assertFalse(output.flushed);

        assertArrayEquals(new byte[0], ByteStreamUtils.readAllBytes(input, 4));
        assertFalse(input.closed);
    }

    @Test
    void sourceStreamsAreOwnedAndClosed() throws Exception {
        AtomicReference<TrackingInput> opened = new AtomicReference<>();
        InputStreamSource source = () -> {
            TrackingInput input = new TrackingInput(new byte[]{1, 2, 3});
            opened.set(input);
            return input;
        };
        TrackingOutput output = new TrackingOutput();

        assertEquals(3, ByteStreamUtils.copy(source, output));
        assertTrue(opened.get().closed);
        assertFalse(output.closed);
        assertFalse(output.flushed);
    }

    @Test
    void boundedReadFailsAsSoonAsLimitIsCrossed() {
        TrackingInput input = new TrackingInput(new byte[]{1, 2, 3, 4});
        SizeLimitExceededException failure = assertThrows(
                SizeLimitExceededException.class,
                () -> ByteStreamUtils.readAllBytes(input, 3));

        assertEquals(3, failure.maximumBytes());
        assertTrue(failure.observedBytes() > failure.maximumBytes());
        assertFalse(input.closed);
        assertThrows(IllegalArgumentException.class, () -> ByteStreamUtils.readAllBytes(input, -1));
    }

    @Test
    void sourceFactoriesAreRepeatableAndSnapshotByteArrays(@TempDir Path directory)
            throws Exception {
        byte[] mutable = "one".getBytes(StandardCharsets.UTF_8);
        InputStreamSource bytes = InputStreamSource.fromBytes(mutable);
        mutable[0] = 'X';
        assertEquals("one", new String(ByteStreamUtils.readAllBytes(bytes, 3), StandardCharsets.UTF_8));
        assertEquals("one", new String(ByteStreamUtils.readAllBytes(bytes, 3), StandardCharsets.UTF_8));

        Path target = directory.resolve("copy.bin");
        assertEquals(3, ByteStreamUtils.copy(bytes, target));
        assertArrayEquals("one".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInput(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static final class TrackingOutput extends ByteArrayOutputStream {
        private boolean closed;
        private boolean flushed;

        @Override
        public void flush() throws IOException {
            flushed = true;
            super.flush();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
