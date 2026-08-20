/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.core.io;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * 仅依赖 JDK 的字节流操作，明确资源所有权和内存分配上限。
 */
public final class ByteStreamUtils {

    public static final int DEFAULT_BUFFER_SIZE = 8192;
    public static final long DEFAULT_MAX_BYTES = 16L * 1024 * 1024;
    private static final long MAX_BYTE_ARRAY_SIZE = Integer.MAX_VALUE - 8L;

    private ByteStreamUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 从借入的输入流复制到借入的输出流。两个流都不会关闭，输出流也不会刷新。
     *
     * @param input  待读取的输入流
     * @param output 待写入的输出流
     * @return 已复制的字节数
     * @throws IOException 读取或写入失败时抛出
     */
    public static long copy(InputStream input, OutputStream output) throws IOException {
        ValidationUtils.requireNonNull(input, "input");
        ValidationUtils.requireNonNull(output, "output");
        return input.transferTo(output);
    }

    /**
     * 打开并关闭源流；借入的输出流既不会关闭，也不会刷新。
     *
     * @param source 提供输入流的源
     * @param output 待写入的输出流
     * @return 已复制的字节数
     * @throws IOException 打开、读取或写入失败时抛出
     */
    public static long copy(InputStreamSource source, OutputStream output) throws IOException {
        ValidationUtils.requireNonNull(source, "source");
        ValidationUtils.requireNonNull(output, "output");
        try (InputStream input = requireOpened(source)) {
            return copy(input, output);
        }
    }

    /**
     * 打开并关闭由库创建的两个流。
     *
     * @param source  提供输入流的源
     * @param target  写入目标路径
     * @param options 打开目标文件的选项
     * @return 已复制的字节数
     * @throws IOException 打开、读取或写入失败时抛出
     */
    public static long copy(InputStreamSource source, Path target, OpenOption... options)
            throws IOException {
        ValidationUtils.requireNonNull(source, "source");
        ValidationUtils.requireNonNull(target, "target");
        ValidationUtils.requireNonNull(options, "options");
        try (InputStream input = requireOpened(source);
             OutputStream output = Files.newOutputStream(target, options)) {
            return copy(input, output);
        }
    }

    /**
     * 以默认 16 MiB 上限读取借入的流，且不会关闭该流。
     *
     * @param input 待读取的输入流
     * @return 完整字节内容
     * @throws IOException 读取失败或内容超过上限时抛出
     */
    public static byte[] readAllBytes(InputStream input) throws IOException {
        return readAllBytes(input, DEFAULT_MAX_BYTES);
    }

    /**
     * 读取借入的流，最大不超过 {@code maximumBytes}，且不会关闭该流。
     *
     * @param input        待读取的输入流
     * @param maximumBytes 允许读取的最大字节数
     * @return 完整字节内容
     * @throws IOException 读取失败或内容超过上限时抛出
     */
    public static byte[] readAllBytes(InputStream input, long maximumBytes) throws IOException {
        ValidationUtils.requireNonNull(input, "input");
        validateMaximumBytes(maximumBytes);

        int initialCapacity = (int) Math.min(DEFAULT_BUFFER_SIZE, maximumBytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialCapacity);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long total = 0;
        while (true) {
            int read = input.read(buffer);
            if (read < 0) {
                return output.toByteArray();
            }
            if (read == 0) {
                continue;
            }
            long nextTotal = total + read;
            if (nextTotal > maximumBytes) {
                throw new SizeLimitExceededException(maximumBytes, nextTotal);
            }
            output.write(buffer, 0, read);
            total = nextTotal;
        }
    }

    /**
     * 有界读取后打开并关闭源流。
     *
     * @param source       提供输入流的源
     * @param maximumBytes 允许读取的最大字节数
     * @return 完整字节内容
     * @throws IOException 打开、读取失败或内容超过上限时抛出
     */
    public static byte[] readAllBytes(InputStreamSource source, long maximumBytes)
            throws IOException {
        ValidationUtils.requireNonNull(source, "source");
        try (InputStream input = requireOpened(source)) {
            return readAllBytes(input, maximumBytes);
        }
    }

    /**
     * 按默认上限读取后打开并关闭源流。
     *
     * @param source 提供输入流的源
     * @return 完整字节内容
     * @throws IOException 打开、读取失败或内容超过上限时抛出
     */
    public static byte[] readAllBytes(InputStreamSource source) throws IOException {
        return readAllBytes(source, DEFAULT_MAX_BYTES);
    }

    /**
     * 以指定字符集读取借入的输入流，且不会关闭该流。
     *
     * @param input        待读取的输入流
     * @param charset      解码字符集
     * @param maximumBytes 允许读取的最大字节数
     * @return 解码后的文本
     * @throws IOException 读取失败或内容超过上限时抛出
     */
    public static String readString(InputStream input, Charset charset, long maximumBytes)
            throws IOException {
        ValidationUtils.requireNonNull(charset, "charset");
        return new String(readAllBytes(input, maximumBytes), charset);
    }

    /**
     * 打开、读取并关闭源流，再以指定字符集解码。
     *
     * @param source       提供输入流的源
     * @param charset      解码字符集
     * @param maximumBytes 允许读取的最大字节数
     * @return 解码后的文本
     * @throws IOException 打开、读取失败或内容超过上限时抛出
     */
    public static String readString(InputStreamSource source, Charset charset, long maximumBytes)
            throws IOException {
        ValidationUtils.requireNonNull(charset, "charset");
        return new String(readAllBytes(source, maximumBytes), charset);
    }

    /**
     * 以 UTF-8 读取借入的输入流，且不会关闭该流。
     *
     * @param input        待读取的输入流
     * @param maximumBytes 允许读取的最大字节数
     * @return 解码后的 UTF-8 文本
     * @throws IOException 读取失败或内容超过上限时抛出
     */
    public static String readUtf8(InputStream input, long maximumBytes) throws IOException {
        return readString(input, StandardCharsets.UTF_8, maximumBytes);
    }

    private static InputStream requireOpened(InputStreamSource source) throws IOException {
        return ValidationUtils.requireNonNull(source.openStream(), "source.openStream()");
    }

    private static void validateMaximumBytes(long maximumBytes) {
        if (maximumBytes < 0 || maximumBytes > MAX_BYTE_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                    "maximumBytes must be between 0 and " + MAX_BYTE_ARRAY_SIZE + ": " + maximumBytes);
        }
    }
}
