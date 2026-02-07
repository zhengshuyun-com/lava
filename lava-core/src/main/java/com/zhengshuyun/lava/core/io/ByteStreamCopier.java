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

package com.zhengshuyun.lava.core.io;

import com.google.common.io.*;
import com.zhengshuyun.lava.core.lang.Validate;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 字节流复制器
 * <p>
 * 优雅地处理字节流复制, 支持多种数据源和目标, 可选进度监听.
 * 所有由框架打开的流都会自动关闭, 用户无需手动管理资源.
 *
 * @author Toint
 */
public final class ByteStreamCopier {

    /**
     * 未知内容长度的标识值
     */
    private static final long UNKNOWN_LENGTH = -1L;

    /**
     * 字节源 (Guava 统一抽象, 支持多次打开流)
     */
    private final ByteSource byteSource;

    /**
     * 总字节数 (-1 表示未知, 用于进度计算)
     */
    private final long contentLength;

    /**
     * 进度监听器 (可选)
     */
    private final @Nullable ProgressListener progressListener;

    private ByteStreamCopier(Builder builder) {
        this.byteSource = Validate.notNull(builder.byteSource, "byteSource must not be null");
        this.contentLength = builder.contentLength;
        this.progressListener = builder.progressListener;
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 写入为字符串 (UTF-8 编码)
     *
     * @return 字符串内容
     * @throws UncheckedIOException 如果发生 I/O 错误
     */
    public String writeString() {
        return writeString(StandardCharsets.UTF_8);
    }

    /**
     * 写入为字符串 (指定编码)
     *
     * @param charset 字符集
     * @return 字符串内容
     * @throws UncheckedIOException 如果发生 I/O 错误
     */
    public String writeString(Charset charset) {
        Validate.notNull(charset, "charset must not be null");
        return new String(writeBytes(), charset);
    }

    /**
     * 写入为字节数组
     *
     * @return 字节数组
     * @throws UncheckedIOException 如果发生 I/O 错误
     */
    public byte[] writeBytes() {
        return IoUtil.wrapIOException(() -> {
            // 无进度监听：使用 Guava
            if (progressListener == null) {
                return byteSource.read();
            }

            // 有进度监听：手动处理流, 触发进度回调
            try (InputStream input = byteSource.openStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                copyWithProgress(input, output);
                return output.toByteArray();
            }
        });
    }

    /**
     * 写入到路径
     *
     * @param path    目标路径
     * @param options 打开选项 (如 StandardOpenOption.CREATE, APPEND 等)
     * @return 复制的字节数
     * @throws UncheckedIOException 如果发生 I/O 错误
     */
    public long write(Path path, OpenOption... options) {
        Validate.notNull(path, "path must not be null");
        return write(MoreFiles.asByteSink(path, options));
    }

    /**
     * 写入到文件
     *
     * @param file  目标文件
     * @param modes 写入模式 (如 FileWriteMode.APPEND)
     * @return 复制的字节数
     * @throws UncheckedIOException 如果发生 I/O 错误
     */
    public long write(File file, FileWriteMode... modes) {
        Validate.notNull(file, "file must not be null");
        return write(Files.asByteSink(file, modes));
    }

    /**
     * 写入到字节接收器 (Guava ByteSink 抽象)
     *
     * @param byteSink 字节接收器
     * @return 复制的字节数
     * @throws UncheckedIOException 如果发生 I/O 错误
     */
    public long write(ByteSink byteSink) {
        Validate.notNull(byteSink, "byteSink must not be null");

        return IoUtil.wrapIOException(() -> {
            // 无进度监听：使用 Guava 的高效实现
            if (progressListener == null) {
                return byteSource.copyTo(byteSink);
            }

            // 有进度监听：手动管理流
            try (InputStream input = byteSource.openStream();
                 OutputStream output = byteSink.openStream()) {
                return copyWithProgress(input, output);
            }
        });
    }

    /**
     * 写入到输出流
     *
     * @param outputStream          输出流
     * @param autoCloseOutputStream 是否自动关闭输出流
     * @return 复制的字节数
     * @throws UncheckedIOException 如果发生 I/O 错误
     */
    public long write(OutputStream outputStream, boolean autoCloseOutputStream) {
        Validate.notNull(outputStream, "outputStream must not be null");

        return IoUtil.wrapIOException(() -> {
            // Guava Closer：确保多个流按逆序安全关闭, 即使发生异常也能正确处理
            try (Closer closer = Closer.create()) {
                InputStream input = closer.register(byteSource.openStream());

                // 根据参数决定是否注册输出流到 Closer
                if (autoCloseOutputStream) {
                    closer.register(outputStream);
                }

                // 根据是否有进度监听选择实现
                if (progressListener == null) {
                    // Guava 高效复制
                    return ByteStreams.copy(input, outputStream);
                } else {
                    return copyWithProgress(input, outputStream);
                }
            }
        });
    }

    // 内部方法

    /**
     * 带进度监听的流复制
     * <p>
     * 读取数据块时触发进度回调, 适用于大文件传输场景.
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @return 复制的总字节数
     * @throws IOException 如果发生 I/O 错误
     */
    private long copyWithProgress(InputStream inputStream, OutputStream outputStream) throws IOException {
        // 通知监听器开始复制, 没监听器的情况下不应该调用本方法
        Validate.notNull(progressListener, "progressListener must not be null");
        progressListener.onStart(contentLength);

        byte[] buffer = IoUtil.createBuffer();
        long totalBytes = 0;
        while (true) {
            int readBytes = inputStream.read(buffer);
            if (readBytes == -1) {
                break;
            }
            outputStream.write(buffer, 0, readBytes);
            totalBytes += readBytes;

            // 通知监听器复制进度
            progressListener.onProgress(totalBytes, contentLength);
        }

        // 通知监听器复制完成
        progressListener.onComplete(totalBytes, contentLength);
        return totalBytes;
    }

    // Builder

    /**
     * 字节流复制器构建器
     * <p>
     * 支持多种数据源, 自动推断内容长度 (如果可能) .
     */
    public static final class Builder {

        private @Nullable ByteSource byteSource;
        private long contentLength = UNKNOWN_LENGTH;
        private @Nullable ProgressListener progressListener;

        private Builder() {
        }

        /**
         * 设置输入流源 (支持多次写入)
         * <p>
         * <b>注意：</b>
         * <ul>
         * <li>框架会在每次写入操作完成后自动关闭流</li>
         * <li>Supplier 应每次返回新的流实例以支持多次写入</li>
         * <li>如果 Supplier 每次返回同一个流实例, 则只能写入一次</li>
         * <li>内容长度未知, 进度监听器的百分比计算可能不准确</li>
         * </ul>
         *
         * @param supplier 输入流提供者 (将被自动关闭)
         * @return Builder 实例
         */
        public Builder setSource(Supplier<InputStream> supplier) {
            return setSource(supplier, UNKNOWN_LENGTH);
        }

        /**
         * 设置输入流源 (支持多次写入, 指定内容长度)
         * <p>
         * <b>注意：</b>
         * <ul>
         * <li>框架会在每次写入操作完成后自动关闭流</li>
         * <li>Supplier 应每次返回新的流实例以支持多次写入</li>
         * <li>如果 Supplier 每次返回同一个流实例, 则只能写入一次</li>
         * </ul>
         *
         * @param supplier      输入流提供者 (将被自动关闭)
         * @param contentLength 内容长度 (字节) , -1 表示未知
         * @return Builder 实例
         */
        public Builder setSource(Supplier<InputStream> supplier, long contentLength) {
            Validate.notNull(supplier, "supplier must not be null");

            this.byteSource = new ByteSource() {
                @Override
                public InputStream openStream() {
                    return Validate.notNull(supplier.get(), "inputStream must not be null");
                }
            };
            this.contentLength = contentLength;

            return this;
        }

        /**
         * 设置输入流源 (仅支持单次写入)
         * <p>
         * <b>注意：</b>
         * <ul>
         * <li>框架会在写入操作完成后自动关闭此输入流</li>
         * <li>InputStream 是一次性资源, 构建的 ByteStreamCopier <b>只能调用一次</b> write 方法</li>
         * <li>如需多次写入, 请使用 File/Path 作为源, 或使用 {@link #setSource(Supplier)} 提供新流</li>
         * <li>内容长度未知, 进度监听器的百分比计算可能不准确</li>
         * </ul>
         *
         * @param inputStream 输入流 (将被自动关闭, 仅支持单次写入)
         * @return Builder 实例
         */
        public Builder setSource(InputStream inputStream) {
            return setSource(inputStream, UNKNOWN_LENGTH);
        }

        /**
         * 设置输入流源 (仅支持单次写入, 指定内容长度)
         * <p>
         * <b>注意：</b>
         * <ul>
         * <li>框架会在写入操作完成后自动关闭此输入流</li>
         * <li>InputStream 是一次性资源, 构建的 ByteStreamCopier <b>只能调用一次</b> write 方法</li>
         * <li>如需多次写入, 请使用 File/Path 作为源, 或使用 {@link #setSource(Supplier)} 提供新流</li>
         * </ul>
         *
         * @param inputStream   输入流 (将被自动关闭, 仅支持单次写入)
         * @param contentLength 内容长度 (字节) , -1 表示未知
         * @return Builder 实例
         */
        public Builder setSource(InputStream inputStream, long contentLength) {
            Validate.notNull(inputStream, "inputStream must not be null");

            // 添加一次性使用保护, 提供友好的错误提示
            AtomicBoolean used = new AtomicBoolean(false);
            this.byteSource = new ByteSource() {
                @Override
                public InputStream openStream() {
                    if (!used.compareAndSet(false, true)) {
                        throw new IllegalStateException(
                                "InputStream source can only be used once. " +
                                        "Use File/Path source for multiple writes, or use setSource(Supplier) to provide fresh streams."
                        );
                    }
                    return inputStream;
                }
            };
            this.contentLength = contentLength;

            return this;
        }

        /**
         * 设置字符串源 (UTF-8 编码)
         *
         * @param string 字符串
         * @return Builder 实例
         */
        public Builder setSource(String string) {
            return setSource(string, StandardCharsets.UTF_8);
        }

        /**
         * 设置字符串源 (指定编码)
         *
         * @param string  字符串
         * @param charset 字符集
         * @return Builder 实例
         */
        public Builder setSource(String string, Charset charset) {
            Validate.notNull(string, "string must not be null");
            Validate.notNull(charset, "charset must not be null");
            return setSource(string.getBytes(charset));
        }

        /**
         * 设置字节数组源
         *
         * @param bytes 字节数组
         * @return Builder 实例
         */
        public Builder setSource(byte[] bytes) {
            Validate.notNull(bytes, "bytes must not be null");
            // Guava ByteSource.wrap：高效包装字节数组, 无需复制
            this.byteSource = ByteSource.wrap(bytes);
            this.contentLength = bytes.length;
            return this;
        }

        /**
         * 设置路径源
         *
         * @param path    路径
         * @param options 打开选项
         * @return Builder 实例
         */
        public Builder setSource(Path path, OpenOption... options) {
            Validate.notNull(path, "path must not be null");
            // Guava MoreFiles：支持 NIO.2 Path API
            this.byteSource = MoreFiles.asByteSource(path, options);
            // 尝试预获取文件大小, 失败则标记为未知
            this.contentLength = tryGetSize(this.byteSource);
            return this;
        }

        /**
         * 设置文件源 (推荐用于本地文件)
         * <p>
         * 使用文件作为源可以多次调用 write 方法, 每次都会重新打开文件.
         *
         * @param file 文件
         * @return Builder 实例
         */
        public Builder setSource(File file) {
            Validate.notNull(file, "file must not be null");
            // Guava Files：传统 File API 的 ByteSource 包装
            this.byteSource = Files.asByteSource(file);
            this.contentLength = file.length();
            return this;
        }

        /**
         * 设置字节源 (高级用法)
         * <p>
         * ByteSource 是 Guava 对字节源的抽象, 可多次调用 openStream(),
         * 适合需要重复读取或自定义数据源的场景.
         *
         * @param byteSource 字节源
         * @return Builder 实例
         */
        public Builder setSource(ByteSource byteSource) {
            this.byteSource = Validate.notNull(byteSource, "byteSource must not be null");
            this.contentLength = tryGetSize(byteSource);
            return this;
        }


        /**
         * 设置进度监听器
         *
         * @param progressListener 进度监听器 (可为 null 表示不监听)
         * @return Builder 实例
         */
        public Builder setProgressListener(@Nullable ProgressListener progressListener) {
            this.progressListener = progressListener;
            return this;
        }

        /**
         * 构建 ByteStreamCopier 实例
         *
         * @return ByteStreamCopier 实例
         * @throws NullPointerException 如果未设置数据源
         */
        public ByteStreamCopier build() {
            return new ByteStreamCopier(this);
        }

        /**
         * 尝试获取 ByteSource 的大小
         * <p>
         * Guava sizeIfKnown()：返回 Optional, 如果无法确定大小则为空
         *
         * @param source 字节源
         * @return 大小 (字节) , 未知则返回 -1
         */
        private static long tryGetSize(ByteSource source) {
            // Guava Optional.or()：提供默认值, 避免 null 判断
            return source.sizeIfKnown().or(UNKNOWN_LENGTH);
        }
    }
}