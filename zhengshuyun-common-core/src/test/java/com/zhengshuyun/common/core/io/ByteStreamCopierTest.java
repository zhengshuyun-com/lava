/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.common.core.io;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ByteStreamCopier单元测试
 * 测试字节流复制功能, 包括字符串、字节数组、文件、进度监听等
 *
 * @author Toint
 * @since 2026/1/11
 */
class ByteStreamCopierTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ByteStreamCopierTest.class);

    /**
     * 测试字符串复制 (UTF-8编码) 
     */
    @Test
    void testCopyString() {
        String input = "zhengshuyun-common";
        String result = ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .writeString();
        assertEquals(input, result);
    }

    /**
     * 测试字符串复制 (指定字符集) 
     */
    @Test
    void testCopyStringWithCharset() {
        String input = "zhengshuyun-common";
        String result = ByteStreamCopier.builder()
                .setSource(input, StandardCharsets.UTF_8)
                .build()
                .writeString(StandardCharsets.UTF_8);
        assertEquals(input, result);
    }

    /**
     * 测试字节数组复制
     */
    @Test
    void testCopyBytes() {
        byte[] input = {1, 2, 3, 4, 5};
        byte[] result = ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .writeBytes();
        assertArrayEquals(input, result);
    }

    /**
     * 测试复制到输出流
     */
    @Test
    void testCopyToOutputStream() {
        String input = "test data";
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long bytesWritten = ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .write(output, false);

        assertEquals(input.length(), bytesWritten);
        assertEquals(input, output.toString(StandardCharsets.UTF_8));
    }

    /**
     * 测试从输入流复制
     */
    @Test
    void testCopyFromInputStream() {
        String input = "stream test";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        String result = ByteStreamCopier.builder()
                .setSource(inputStream)
                .build()
                .writeString();

        assertEquals(input, result);
    }

    /**
     * 测试复制到文件
     */
    @Test
    void testCopyToFile(@TempDir Path tempDir) throws IOException {
        String input = "file content";
        Path outputFile = tempDir.resolve("output.txt");

        long bytesWritten = ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .write(outputFile.toFile());

        assertEquals(input.length(), bytesWritten);
        String fileContent = Files.readString(outputFile);
        assertEquals(input, fileContent);
    }

    /**
     * 测试从文件复制
     */
    @Test
    void testCopyFromFile(@TempDir Path tempDir) throws IOException {
        String input = "file input content";
        Path inputFile = tempDir.resolve("input.txt");
        Files.writeString(inputFile, input);

        String result = ByteStreamCopier.builder()
                .setSource(inputFile.toFile())
                .build()
                .writeString();

        assertEquals(input, result);
    }

    /**
     * 测试进度监听器
     */
    @Test
    void testProgressListener() {
        String input = "progress test data";
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicLong completedBytes = new AtomicLong(0);

        ProgressListener listener = new ProgressListener() {
            @Override
            public void onStart(long totalBytes) {
                assertTrue(totalBytes > 0);
            }

            @Override
            public void onProgress(long current, long total) {
                totalBytes.set(current);
            }

            @Override
            public void onComplete(long currentBytes, long totalBytes) {
                completedBytes.set(currentBytes);
            }
        };

        // 字符串源会自动设置 contentLength
        ByteStreamCopier.builder()
                .setSource(input)
                .setProgressListener(listener)
                .build()
                .writeBytes();

        assertEquals(input.length(), totalBytes.get());
        assertEquals(input.length(), completedBytes.get());
    }

    /**
     * 测试自动关闭输入流
     */
    @Test
    void testAutoCloseInputStream() {
        CloseTrackingInputStream inputStream = new CloseTrackingInputStream(
                new ByteArrayInputStream("test".getBytes())
        );

        ByteStreamCopier.builder()
                .setSource(inputStream)
                .build()
                .writeBytes();

        assertTrue(inputStream.isClosed(), "InputStream should be closed automatically");
    }

    /**
     * 测试自动关闭输出流
     */
    @Test
    void testAutoCloseOutputStream() {
        String input = "output test";
        CloseTrackingOutputStream outputStream = new CloseTrackingOutputStream(
                new ByteArrayOutputStream()
        );

        ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .write(outputStream, true);

        assertTrue(outputStream.isClosed(), "OutputStream should be closed");
    }

    /**
     * 测试空流复制
     */
    @Test
    void testEmptyStream() {
        String result = ByteStreamCopier.builder()
                .setSource("")
                .build()
                .writeString();
        assertEquals("", result);
    }

    /**
     * 测试大数据复制 (1MB) 
     */
    @Test
    void testLargeData() {
        byte[] largeData = new byte[1024 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        byte[] result = ByteStreamCopier.builder()
                .setSource(largeData)
                .build()
                .writeBytes();

        assertArrayEquals(largeData, result);
    }

    /**
     * 测试空值验证
     */
    @Test
    void testNullValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder()
                        .setSource((InputStream) null)
                        .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder()
                        .setSource("test")
                        .build()
                        .write((OutputStream) null, false)
        );
    }

    /**
     * 测试复制到Path
     */
    @Test
    void testCopyToPath(@TempDir Path tempDir) throws IOException {
        String input = "path content";
        Path outputFile = tempDir.resolve("path-output.txt");

        long bytesWritten = ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .write(outputFile);

        assertEquals(input.length(), bytesWritten);
        String fileContent = Files.readString(outputFile);
        assertEquals(input, fileContent);
    }

    /**
     * 测试从Path复制
     */
    @Test
    void testCopyFromPath(@TempDir Path tempDir) throws IOException {
        String input = "path input content";
        Path inputFile = tempDir.resolve("path-input.txt");
        Files.writeString(inputFile, input);

        String result = ByteStreamCopier.builder()
                .setSource(inputFile)
                .build()
                .writeString();

        assertEquals(input, result);
    }

    /**
     * 测试使用Supplier作为输入源
     */
    @Test
    void testCopyFromSupplier() {
        String input = "supplier test";
        String result = ByteStreamCopier.builder()
                .setSource(() -> new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
                .build()
                .writeString();
        assertEquals(input, result);
    }

    /**
     * 测试进度监听器不设置内容长度
     * 使用InputStream作为源, 长度未知
     */
    @Test
    void testProgressListenerWithoutLength() {
        String input = "unknown length data";
        AtomicLong totalBytes = new AtomicLong(0);

        ProgressListener listener = new ProgressListener() {
            @Override
            public void onStart(long totalBytes) {
                assertEquals(-1L, totalBytes, "总长度应该是-1 (未知) ");
            }

            @Override
            public void onProgress(long current, long total) {
                totalBytes.set(current);
            }

            @Override
            public void onComplete(long currentBytes, long totalBytes) {
                assertEquals(input.length(), currentBytes, "复制完成时的字节数应该匹配");
            }
        };

        ByteStreamCopier.builder()
                .setSource(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
                .setProgressListener(listener)
                .build()
                .writeBytes();

        assertEquals(input.length(), totalBytes.get(), "复制的总字节数应该匹配");
    }

    /**
     * 测试不自动关闭输出流
     */
    @Test
    void testDoNotAutoCloseOutputStream() {
        String input = "no close test";
        CloseTrackingOutputStream outputStream = new CloseTrackingOutputStream(
                new ByteArrayOutputStream()
        );

        ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .write(outputStream, false);

        assertFalse(outputStream.isClosed(), "OutputStream should not be closed");
    }

    /**
     * 测试复制到ByteSink
     */
    @Test
    void testCopyToByteSink() {
        String input = "bytesink test";
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long bytesWritten = ByteStreamCopier.builder()
                .setSource(input)
                .build()
                .write(new com.google.common.io.ByteSink() {
                    @Override
                    public OutputStream openStream() {
                        return output;
                    }
                });

        assertEquals(input.length(), bytesWritten);
        assertEquals(input, output.toString(StandardCharsets.UTF_8));
    }

    /**
     * 测试从ByteSource复制
     */
    @Test
    void testCopyFromByteSource() {
        byte[] input = {1, 2, 3, 4, 5};
        com.google.common.io.ByteSource byteSource = com.google.common.io.ByteSource.wrap(input);

        byte[] result = ByteStreamCopier.builder()
                .setSource(byteSource)
                .build()
                .writeBytes();

        assertArrayEquals(input, result);
    }

    /**
     * 测试追加模式写入文件
     */
    @Test
    void testAppendToFile(@TempDir Path tempDir) throws IOException {
        String input1 = "First content\n";
        String input2 = "Second content\n";
        Path outputFile = tempDir.resolve("append.txt");

        ByteStreamCopier.builder()
                .setSource(input1)
                .build()
                .write(outputFile.toFile());

        ByteStreamCopier.builder()
                .setSource(input2)
                .build()
                .write(outputFile.toFile(), com.google.common.io.FileWriteMode.APPEND);

        String fileContent = Files.readString(outputFile);
        assertEquals(input1 + input2, fileContent);
    }

    /**
     * 测试指定字符集复制字符串
     */
    @Test
    void testCopyStringWithDifferentCharset() {
        String input = "测试中文";
        String result = ByteStreamCopier.builder()
                .setSource(input, StandardCharsets.UTF_8)
                .build()
                .writeString(StandardCharsets.UTF_8);
        assertEquals(input, result);
    }

    /**
     * 内部类：用于跟踪输入流是否被关闭
     */
    private static class CloseTrackingInputStream extends FilterInputStream {
        private boolean closed = false;

        public CloseTrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    /**
     * 内部类：用于跟踪输出流是否被关闭
     */
    private static class CloseTrackingOutputStream extends FilterOutputStream {
        private boolean closed = false;

        public CloseTrackingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    /**
     * 测试 InputStream 单次使用保护
     * InputStream 源只能调用一次 write 方法
     */
    @Test
    void testInputStreamSingleUseProtection() {
        String input = "single use test";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(inputStream)
                .build();

        // 第一次写入成功
        String result1 = copier.writeString();
        assertEquals(input, result1);

        // 第二次写入应该抛出 IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, copier::writeString);
        assertTrue(exception.getMessage().contains("InputStream source can only be used once"));
    }

    /**
     * 测试 InputStream 带长度的单次使用保护
     */
    @Test
    void testInputStreamWithLengthSingleUseProtection() {
        String input = "single use with length";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(inputStream, input.getBytes(StandardCharsets.UTF_8).length)
                .build();

        // 第一次写入成功
        copier.writeBytes();

        // 第二次写入应该抛出 IllegalStateException
        assertThrows(IllegalStateException.class, copier::writeBytes);
    }

    /**
     * 测试 Supplier 支持多次写入
     */
    @Test
    void testSupplierMultipleWrites(@TempDir Path tempDir) throws IOException {
        String input = "supplier multiple writes";
        
        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(() -> new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
                .build();

        // 多次写入都应该成功
        Path file1 = tempDir.resolve("output1.txt");
        Path file2 = tempDir.resolve("output2.txt");
        
        copier.write(file1);
        copier.write(file2);

        assertEquals(input, Files.readString(file1));
        assertEquals(input, Files.readString(file2));
    }

    /**
     * 测试 Supplier 带长度支持多次写入
     */
    @Test
    void testSupplierWithLengthMultipleWrites() {
        String input = "supplier with length";
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        
        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(() -> new ByteArrayInputStream(bytes), bytes.length)
                .build();

        // 多次写入都应该成功
        String result1 = copier.writeString();
        String result2 = copier.writeString();

        assertEquals(input, result1);
        assertEquals(input, result2);
    }

    /**
     * 测试 File 源支持多次写入
     */
    @Test
    void testFileSourceMultipleWrites(@TempDir Path tempDir) throws IOException {
        String input = "file multiple writes";
        Path inputFile = tempDir.resolve("input-multi.txt");
        Files.writeString(inputFile, input);

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(inputFile.toFile())
                .build();

        // 多次读取都应该成功
        String result1 = copier.writeString();
        String result2 = copier.writeString();
        String result3 = copier.writeString();

        assertEquals(input, result1);
        assertEquals(input, result2);
        assertEquals(input, result3);
    }

    /**
     * 测试 Path 源支持多次写入
     */
    @Test
    void testPathSourceMultipleWrites(@TempDir Path tempDir) throws IOException {
        String input = "path multiple writes";
        Path inputFile = tempDir.resolve("path-input-multi.txt");
        Files.writeString(inputFile, input);

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(inputFile)
                .build();

        // 多次读取都应该成功
        byte[] result1 = copier.writeBytes();
        byte[] result2 = copier.writeBytes();

        assertArrayEquals(input.getBytes(StandardCharsets.UTF_8), result1);
        assertArrayEquals(input.getBytes(StandardCharsets.UTF_8), result2);
    }

    /**
     * 测试不同字符集
     */
    @Test
    void testDifferentCharsets() {
        String input = "Hello 世界 🎉";
        
        // ISO-8859-1 (会丢失中文和emoji)
        ByteStreamCopier copier1 = ByteStreamCopier.builder()
                .setSource(input, StandardCharsets.UTF_8)
                .build();
        String utf8Result = copier1.writeString(StandardCharsets.UTF_8);
        assertEquals(input, utf8Result);

        // UTF-16
        ByteStreamCopier copier2 = ByteStreamCopier.builder()
                .setSource(input, StandardCharsets.UTF_16)
                .build();
        String utf16Result = copier2.writeString(StandardCharsets.UTF_16);
        assertEquals(input, utf16Result);
    }

    /**
     * 测试 writeString 的 null charset 校验
     */
    @Test
    void testWriteStringNullCharset() {
        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource("test")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                copier.writeString(null)
        );
    }

    /**
     * 测试 Builder.setSource 的各种 null 校验
     */
    @Test
    void testBuilderNullValidations() {
        // setSource(String) null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource((String) null)
        );

        // setSource(byte[]) null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource((byte[]) null)
        );

        // setSource(File) null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource((File) null)
        );

        // setSource(Path) null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource((Path) null)
        );

        // setSource(Supplier) null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource((Supplier<InputStream>) null)
        );

        // setSource(ByteSource) null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource((com.google.common.io.ByteSource) null)
        );

        // setSource(String, Charset) - string null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource(null, StandardCharsets.UTF_8)
        );

        // setSource(String, Charset) - charset null
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().setSource("test", null)
        );
    }

    /**
     * 测试 write 方法的 null 校验
     */
    @Test
    void testWriteMethodsNullValidation() {
        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource("test")
                .build();

        // write(Path) null
        assertThrows(IllegalArgumentException.class, () ->
                copier.write((Path) null)
        );

        // write(File) null
        assertThrows(IllegalArgumentException.class, () ->
                copier.write((File) null)
        );

        // write(ByteSink) null
        assertThrows(IllegalArgumentException.class, () ->
                copier.write((com.google.common.io.ByteSink) null)
        );
    }

    /**
     * 测试 build 时未设置 source 抛异常
     */
    @Test
    void testBuildWithoutSource() {
        assertThrows(IllegalArgumentException.class, () ->
                ByteStreamCopier.builder().build()
        );
    }

    /**
     * 测试零字节文件
     */
    @Test
    void testZeroByteFile(@TempDir Path tempDir) throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "");

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(emptyFile.toFile())
                .build();

        String result = copier.writeString();
        assertEquals("", result);
    }

    /**
     * 测试零字节数组
     */
    @Test
    void testZeroByteArray() {
        byte[] emptyArray = new byte[0];
        
        byte[] result = ByteStreamCopier.builder()
                .setSource(emptyArray)
                .build()
                .writeBytes();

        assertEquals(0, result.length);
    }

    /**
     * 测试 ByteSink 带进度监听
     */
    @Test
    void testByteSinkWithProgressListener() {
        String input = "bytesink with progress";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AtomicLong progressCalls = new AtomicLong(0);

        long bytesWritten = ByteStreamCopier.builder()
                .setSource(input)
                .setProgressListener((current, total) -> progressCalls.incrementAndGet())
                .build()
                .write(new com.google.common.io.ByteSink() {
                    @Override
                    public OutputStream openStream() {
                        return output;
                    }
                });

        assertEquals(input.length(), bytesWritten);
        assertEquals(input, output.toString(StandardCharsets.UTF_8));
        assertTrue(progressCalls.get() > 0, "Progress listener should be called");
    }

    /**
     * 测试 OutputStream 带进度监听
     */
    @Test
    void testOutputStreamWithProgressListener() {
        String input = "outputstream with progress";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AtomicLong progressCalls = new AtomicLong(0);

        long bytesWritten = ByteStreamCopier.builder()
                .setSource(input)
                .setProgressListener((current, total) -> progressCalls.incrementAndGet())
                .build()
                .write(output, false);

        assertEquals(input.length(), bytesWritten);
        assertTrue(progressCalls.get() > 0, "Progress listener should be called");
    }

    /**
     * 测试进度监听器抛异常会中断写入
     */
    @Test
    void testProgressListenerException() {
        String input = "progress exception test";

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(input)
                .setProgressListener((current, total) -> {
                    throw new RuntimeException("Test exception in progress listener");
                })
                .build();

        assertThrows(RuntimeException.class, copier::writeBytes);
    }

    /**
     * 测试 Path 使用 OpenOption
     */
    @Test
    void testPathWithOpenOptions(@TempDir Path tempDir) throws IOException {
        String input1 = "First line\n";
        String input2 = "Second line\n";
        Path outputFile = tempDir.resolve("open-options.txt");

        // 创建并写入
        ByteStreamCopier.builder()
                .setSource(input1)
                .build()
                .write(outputFile, java.nio.file.StandardOpenOption.CREATE, 
                       java.nio.file.StandardOpenOption.WRITE);

        // 追加写入
        ByteStreamCopier.builder()
                .setSource(input2)
                .build()
                .write(outputFile, java.nio.file.StandardOpenOption.APPEND);

        String content = Files.readString(outputFile);
        assertEquals(input1 + input2, content);
    }

    /**
     * 测试 Path 作为源使用 OpenOption
     */
    @Test
    void testPathSourceWithOpenOptions(@TempDir Path tempDir) throws IOException {
        String input = "read with options";
        Path inputFile = tempDir.resolve("read-options.txt");
        Files.writeString(inputFile, input);

        String result = ByteStreamCopier.builder()
                .setSource(inputFile, java.nio.file.StandardOpenOption.READ)
                .build()
                .writeString();

        assertEquals(input, result);
    }

    /**
     * 测试 ByteSource 未知大小
     */
    @Test
    void testByteSourceUnknownSize() {
        String input = "unknown size source";
        com.google.common.io.ByteSource customSource = new com.google.common.io.ByteSource() {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            }
            // 不重写 sizeIfKnown(), 默认返回 Optional.absent()
        };

        String result = ByteStreamCopier.builder()
                .setSource(customSource)
                .build()
                .writeString();

        assertEquals(input, result);
    }

    /**
     * 测试大文件进度监听
     */
    @Test
    void testLargeFileWithProgress() {
        byte[] largeData = new byte[100 * 1024]; // 100 KB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        AtomicLong progressCalls = new AtomicLong(0);
        AtomicLong lastProgress = new AtomicLong(0);

        byte[] result = ByteStreamCopier.builder()
                .setSource(largeData)
                .setProgressListener((current, total) -> {
                    progressCalls.incrementAndGet();
                    lastProgress.set(current);
                })
                .build()
                .writeBytes();

        assertArrayEquals(largeData, result);
        assertTrue(progressCalls.get() > 1, "Should have multiple progress updates for large file");
        assertEquals(largeData.length, lastProgress.get(), "Last progress should equal total size");
    }

    /**
     * 测试 InputStream 返回 null (Supplier)
     */
    @Test
    void testSupplierReturnsNull() {
        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(() -> null)
                .build();

        // 在实际使用时才会抛异常
        assertThrows(IllegalArgumentException.class, copier::writeBytes);
    }

    /**
     * 测试文件不存在时的异常
     */
    @Test
    void testNonExistentFile(@TempDir Path tempDir) {
        File nonExistent = tempDir.resolve("non-existent.txt").toFile();

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(nonExistent)
                .build();

        assertThrows(UncheckedIOException.class, copier::writeBytes);
    }

    /**
     * 测试 writeBytes 与 writeString 结果一致性
     */
    @Test
    void testWriteBytesAndWriteStringConsistency() {
        String input = "consistency test 测试";
        
        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(input)
                .build();

        byte[] bytes = copier.writeBytes();
        
        ByteStreamCopier copier2 = ByteStreamCopier.builder()
                .setSource(input)
                .build();
        String string = copier2.writeString();

        assertEquals(input, string);
        assertArrayEquals(input.getBytes(StandardCharsets.UTF_8), bytes);
    }

    /**
     * 测试 Supplier 返回的 InputStream 被正确调用
     */
    @Test
    void testSupplierStreamCreation() {
        AtomicLong supplierCallCount = new AtomicLong(0);
        String input = "supplier call count";

        ByteStreamCopier copier = ByteStreamCopier.builder()
                .setSource(() -> {
                    supplierCallCount.incrementAndGet();
                    return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
                })
                .build();

        copier.writeString();
        copier.writeString();
        copier.writeString();

        assertEquals(3, supplierCallCount.get(), "Supplier should be called once per write");
    }

    /**
     * 测试进度条监听器 (禁用, 需要手动观察输出) 
     * 使用DataTransferUtil进行格式化输出
     */
    @Test
    @Disabled
    void testProgressBarListener() throws InterruptedException {
        byte[] largeData = new byte[10 * 1024 * 1024];

        DataTransferUtil.Tracker tracker = DataTransferUtil.tracker(largeData.length);

        // 字节数组源会自动设置 contentLength
        ByteStreamCopier.builder()
                .setSource(largeData)
                .setProgressListener(new ProgressListener() {
                    @Override
                    public void onStart(long totalBytes) {
                        if (totalBytes > 0) {
                            LOGGER.info("开始写入, 总大小: {}", DataTransferUtil.formatBytes(totalBytes));
                        }
                    }

                    @Override
                    public void onProgress(long bytesRead, long contentLength) {
                        LOGGER.info("{}", tracker.format(bytesRead));
                        try {
                            TimeUnit.MILLISECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onComplete(long currentBytes, long totalBytes) {
                        LOGGER.info("复制完成. 总计: {}", DataTransferUtil.formatBytes(currentBytes));
                    }
                })
                .build()
                .writeBytes();
    }

}
