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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * IO 工具类
 *
 * @author Toint
 * @since 2026/1/10
 */
public final class IoUtil {

    /**
     * 默认缓冲区大小 (8KB)
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private IoUtil() {
    }

    /**
     * 创建默认大小的缓冲区
     */
    public static byte[] createBuffer() {
        return new byte[DEFAULT_BUFFER_SIZE];
    }

    /**
     * 创建字节流复制器
     *
     * @return 字节流复制器
     */
    public static ByteStreamCopier.Builder copier() {
        return ByteStreamCopier.builder();
    }

    /**
     * 静默关闭资源 (逆序关闭, 忽略异常)
     *
     * @param closeables 资源数组
     */
    public static void closeQuietly(@Nullable AutoCloseable @Nullable ... closeables) {
        if (closeables == null) return;

        // 逆序关闭
        for (int i = closeables.length - 1; i >= 0; i--) {
            AutoCloseable closeable = closeables[i];
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 包装 IO 异常为 {@link UncheckedIOException}
     *
     * @param supplier 可能抛出 IOException 的操作
     * @param <T>      返回类型
     * @return 操作结果
     * @throws UncheckedIOException 包装后的异常
     */
    public static <T> T wrapIOException(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    /**
     * 包装 IO 异常为 {@link UncheckedIOException}
     *
     * @param runnable 可能抛出 IOException 的操作
     * @throws UncheckedIOException 包装后的异常
     */
    public static void wrapIOException(IoRunnable runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    /**
     * 可抛出 {@link IOException} 的 Supplier
     * <p>
     * 类似于 {@link java.util.function.Supplier}, 但允许抛出 {@link IOException}
     */
    @FunctionalInterface
    public interface IoSupplier<T> {
        /**
         * 获取结果
         *
         * @return 结果
         * @throws IOException 如果发生 I/O 错误
         */
        T get() throws IOException;
    }

    /**
     * 可抛出 {@link IOException} 的 Runnable
     * <p>
     * 类似于 {@link java.lang.Runnable}, 但允许抛出 {@link IOException}
     */
    @FunctionalInterface
    public interface IoRunnable {
        /**
         * 执行操作
         *
         * @throws IOException 如果发生 I/O 错误
         */
        void run() throws IOException;
    }
}