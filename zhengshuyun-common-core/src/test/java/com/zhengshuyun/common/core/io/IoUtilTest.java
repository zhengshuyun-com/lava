/*
 * Copyright 2025 Toint (599818663@qq.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.common.core.io;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IoUtil 单元测试
 * 测试 IO 工具类的各项功能
 *
 * @author Toint
 * @since 2026/01/18
 */
class IoUtilTest {

    /**
     * 测试创建默认大小的缓冲区
     */
    @Test
    void testCreateBuffer() {
        byte[] buffer = IoUtil.createBuffer();
        assertNotNull(buffer);
        assertEquals(IoUtil.DEFAULT_BUFFER_SIZE, buffer.length);
        assertEquals(8192, buffer.length);
    }

    /**
     * 测试创建字节流复制器
     */
    @Test
    void testCopier() {
        ByteStreamCopier.Builder copier = IoUtil.copier();
        assertNotNull(copier);
        assertInstanceOf(ByteStreamCopier.Builder.class, copier);
    }

    /**
     * 测试 copier 可以正常使用
     */
    @Test
    void testCopierFunctional() {
        String input = "test copier";
        String result = IoUtil.copier()
                .setSource(input)
                .build()
                .writeString();
        assertEquals(input, result);
    }

    /**
     * 测试 closeQuietly - 正常关闭单个资源
     */
    @Test
    void testCloseQuietlySingleResource() {
        CloseTrackingResource resource = new CloseTrackingResource(false);
        IoUtil.closeQuietly(resource);
        assertTrue(resource.isClosed());
    }

    /**
     * 测试 closeQuietly - 逆序关闭多个资源
     */
    @Test
    void testCloseQuietlyMultipleResources() {
        CloseOrderTracker tracker = new CloseOrderTracker();
        CloseTrackingResource resource1 = new CloseTrackingResource(false, tracker, 1);
        CloseTrackingResource resource2 = new CloseTrackingResource(false, tracker, 2);
        CloseTrackingResource resource3 = new CloseTrackingResource(false, tracker, 3);

        IoUtil.closeQuietly(resource1, resource2, resource3);

        assertTrue(resource1.isClosed());
        assertTrue(resource2.isClosed());
        assertTrue(resource3.isClosed());
        // 验证逆序关闭 (3 -> 2 -> 1)
        assertEquals("3,2,1", tracker.getCloseOrder());
    }

    /**
     * 测试 closeQuietly - 忽略关闭异常
     */
    @Test
    void testCloseQuietlyIgnoresException() {
        CloseTrackingResource goodResource = new CloseTrackingResource(false);
        CloseTrackingResource badResource = new CloseTrackingResource(true); // 抛出异常
        CloseTrackingResource anotherGoodResource = new CloseTrackingResource(false);

        // 不应该抛出异常
        assertDoesNotThrow(() -> IoUtil.closeQuietly(goodResource, badResource, anotherGoodResource));

        // 所有资源都应该尝试关闭
        assertTrue(goodResource.isClosed());
        assertTrue(badResource.isClosed());
        assertTrue(anotherGoodResource.isClosed());
    }

    /**
     * 测试 closeQuietly - 处理 null 数组
     */
    @Test
    void testCloseQuietlyNullArray() {
        assertDoesNotThrow(() -> IoUtil.closeQuietly((AutoCloseable[]) null));
    }

    /**
     * 测试 closeQuietly - 处理数组中的 null 元素
     */
    @Test
    void testCloseQuietlyNullElements() {
        CloseTrackingResource resource1 = new CloseTrackingResource(false);
        CloseTrackingResource resource2 = new CloseTrackingResource(false);

        assertDoesNotThrow(() -> IoUtil.closeQuietly(resource1, null, resource2, null));

        assertTrue(resource1.isClosed());
        assertTrue(resource2.isClosed());
    }

    /**
     * 测试 closeQuietly - 空数组
     */
    @Test
    void testCloseQuietlyEmptyArray() {
        assertDoesNotThrow(() -> IoUtil.closeQuietly());
    }

    /**
     * 测试 wrapIOException(IoSupplier) - 正常返回
     */
    @Test
    void testWrapIOExceptionSupplierSuccess() {
        String result = IoUtil.wrapIOException(() -> "success");
        assertEquals("success", result);
    }

    /**
     * 测试 wrapIOException(IoSupplier) - 抛出 IOException
     */
    @Test
    void testWrapIOExceptionSupplierThrowsIOException() {
        String errorMessage = "Test IOException";
        UncheckedIOException exception = assertThrows(UncheckedIOException.class, () ->
            IoUtil.wrapIOException(() -> {
                throw new IOException(errorMessage);
            })
        );

        assertEquals(errorMessage, exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
        assertEquals(errorMessage, exception.getCause().getMessage());
    }

    /**
     * 测试 wrapIOException(IoSupplier) - 返回 null
     */
    @Test
    void testWrapIOExceptionSupplierReturnsNull() {
        String result = IoUtil.wrapIOException(() -> null);
        assertNull(result);
    }

    /**
     * 测试 wrapIOException(IoSupplier) - 复杂对象
     */
    @Test
    void testWrapIOExceptionSupplierComplexObject() {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());
        byte[] result = IoUtil.wrapIOException(() -> {
            byte[] buffer = new byte[4];
            input.read(buffer);
            return buffer;
        });

        assertNotNull(result);
        assertArrayEquals("test".getBytes(), result);
    }

    /**
     * 测试 wrapIOException(IoRunnable) - 正常执行
     */
    @Test
    void testWrapIOExceptionRunnableSuccess() {
        AtomicBoolean executed = new AtomicBoolean(false);
        assertDoesNotThrow(() ->
            IoUtil.wrapIOException(() -> executed.set(true))
        );
        assertTrue(executed.get());
    }

    /**
     * 测试 wrapIOException(IoRunnable) - 抛出 IOException
     */
    @Test
    void testWrapIOExceptionRunnableThrowsIOException() {
        String errorMessage = "Test IOException in Runnable";
        UncheckedIOException exception = assertThrows(UncheckedIOException.class, () ->
            IoUtil.wrapIOException(() -> {
                throw new IOException(errorMessage);
            })
        );

        assertEquals(errorMessage, exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    /**
     * 测试 wrapIOException(IoRunnable) - 实际 IO 操作
     */
    @Test
    void testWrapIOExceptionRunnableRealIO() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        assertDoesNotThrow(() ->
            IoUtil.wrapIOException(() -> {
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write("test data".getBytes());
                }
            })
        );

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    /**
     * 测试 IoSupplier 函数式接口
     */
    @Test
    void testIoSupplierFunctionalInterface() {
        IoUtil.IoSupplier<String> supplier = () -> {
            // 模拟可能抛出 IOException 的操作
            if (System.currentTimeMillis() > 0) {
                return "result";
            }
            throw new IOException("unreachable");
        };

        String result = IoUtil.wrapIOException(supplier);
        assertEquals("result", result);
    }

    /**
     * 测试 IoRunnable 函数式接口
     */
    @Test
    void testIoRunnableFunctionalInterface() {
        AtomicBoolean flag = new AtomicBoolean(false);
        IoUtil.IoRunnable runnable = () -> {
            // 模拟可能抛出 IOException 的操作
            if (System.currentTimeMillis() > 0) {
                flag.set(true);
                return;
            }
            throw new IOException("unreachable");
        };

        IoUtil.wrapIOException(runnable);
        assertTrue(flag.get());
    }

    /**
     * 测试 wrapIOException 保留异常堆栈
     */
    @Test
    void testWrapIOExceptionPreservesStackTrace() {
        try {
            IoUtil.wrapIOException(() -> {
                throw new IOException("Original exception");
            });
            fail("Should throw UncheckedIOException");
        } catch (UncheckedIOException e) {
            assertNotNull(e.getCause());
            assertEquals("Original exception", e.getCause().getMessage());
            assertTrue(e.getCause().getStackTrace().length > 0);
        }
    }

    // ==================== 辅助类 ====================

    /**
     * 用于跟踪资源关闭的测试类
     */
    private static class CloseTrackingResource implements AutoCloseable {
        private boolean closed = false;
        private final boolean throwOnClose;
        private final CloseOrderTracker orderTracker;
        private final int id;

        public CloseTrackingResource(boolean throwOnClose) {
            this(throwOnClose, null, 0);
        }

        public CloseTrackingResource(boolean throwOnClose, CloseOrderTracker orderTracker, int id) {
            this.throwOnClose = throwOnClose;
            this.orderTracker = orderTracker;
            this.id = id;
        }

        @Override
        public void close() throws Exception {
            closed = true;
            if (orderTracker != null) {
                orderTracker.recordClose(id);
            }
            if (throwOnClose) {
                throw new IOException("Intentional close exception");
            }
        }

        public boolean isClosed() {
            return closed;
        }
    }

    /**
     * 用于跟踪资源关闭顺序的测试类
     */
    private static class CloseOrderTracker {
        private final StringBuilder order = new StringBuilder();

        public void recordClose(int id) {
            if (order.length() > 0) {
                order.append(",");
            }
            order.append(id);
        }

        public String getCloseOrder() {
            return order.toString();
        }
    }
}
