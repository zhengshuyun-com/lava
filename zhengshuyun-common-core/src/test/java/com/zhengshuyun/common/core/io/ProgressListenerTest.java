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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProgressListener 单元测试
 * 测试进度监听器的默认实现和回调机制
 *
 * @author Toint
 * @since 2026/01/18
 */
class ProgressListenerTest {

    /**
     * 测试默认实现 - onStart 方法
     */
    @Test
    void testDefaultOnStart() {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                // 必须实现的方法
            }
        };

        // 默认实现不应抛异常
        assertDoesNotThrow(() -> listener.onStart(1024));
        assertDoesNotThrow(() -> listener.onStart(-1));
        assertDoesNotThrow(() -> listener.onStart(0));
    }

    /**
     * 测试默认实现 - onComplete 方法
     */
    @Test
    void testDefaultOnComplete() {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                // 必须实现的方法
            }
        };

        // 默认实现不应抛异常
        assertDoesNotThrow(() -> listener.onComplete(1024, 1024));
        assertDoesNotThrow(() -> listener.onComplete(512, -1));
        assertDoesNotThrow(() -> listener.onComplete(0, 0));
    }

    /**
     * 测试完整生命周期 - onStart -> onProgress -> onComplete
     */
    @Test
    void testFullLifecycle() {
        LifecycleTracker tracker = new LifecycleTracker();

        tracker.onStart(1024);
        tracker.onProgress(256, 1024);
        tracker.onProgress(512, 1024);
        tracker.onProgress(1024, 1024);
        tracker.onComplete(1024, 1024);

        assertTrue(tracker.isStartCalled());
        assertEquals(3, tracker.getProgressCallCount());
        assertTrue(tracker.isCompleteCalled());
    }

    /**
     * 测试 onStart 参数 - 已知大小
     */
    @Test
    void testOnStartKnownSize() {
        AtomicLong capturedTotal = new AtomicLong(-999);
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onStart(long totalBytes) {
                capturedTotal.set(totalBytes);
            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
            }
        };

        listener.onStart(1024 * 1024);
        assertEquals(1024 * 1024, capturedTotal.get());
    }

    /**
     * 测试 onStart 参数 - 未知大小
     */
    @Test
    void testOnStartUnknownSize() {
        AtomicLong capturedTotal = new AtomicLong(999);
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onStart(long totalBytes) {
                capturedTotal.set(totalBytes);
            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
            }
        };

        listener.onStart(-1);
        assertEquals(-1, capturedTotal.get());
    }

    /**
     * 测试 onProgress 参数传递
     */
    @Test
    void testOnProgressParameters() {
        ProgressCapture capture = new ProgressCapture();

        capture.onProgress(256, 1024);
        assertEquals(256, capture.getLastCurrent());
        assertEquals(1024, capture.getLastTotal());

        capture.onProgress(512, 1024);
        assertEquals(512, capture.getLastCurrent());
        assertEquals(1024, capture.getLastTotal());
    }

    /**
     * 测试 onProgress - 未知总大小
     */
    @Test
    void testOnProgressUnknownTotal() {
        ProgressCapture capture = new ProgressCapture();

        capture.onProgress(256, -1);
        assertEquals(256, capture.getLastCurrent());
        assertEquals(-1, capture.getLastTotal());
    }

    /**
     * 测试 onComplete 参数传递
     */
    @Test
    void testOnCompleteParameters() {
        CompleteCapture capture = new CompleteCapture();

        capture.onComplete(1024, 1024);
        assertEquals(1024, capture.getFinalCurrent());
        assertEquals(1024, capture.getFinalTotal());
    }

    /**
     * 测试 onComplete - 未知总大小
     */
    @Test
    void testOnCompleteUnknownTotal() {
        CompleteCapture capture = new CompleteCapture();

        capture.onComplete(1024, -1);
        assertEquals(1024, capture.getFinalCurrent());
        assertEquals(-1, capture.getFinalTotal());
    }

    /**
     * 测试回调顺序
     */
    @Test
    void testCallbackOrder() {
        CallbackOrderTracker tracker = new CallbackOrderTracker();

        tracker.onStart(1000);
        tracker.onProgress(250, 1000);
        tracker.onProgress(500, 1000);
        tracker.onProgress(750, 1000);
        tracker.onProgress(1000, 1000);
        tracker.onComplete(1000, 1000);

        List<String> expectedOrder = List.of("start", "progress", "progress", "progress", "progress", "complete");
        assertEquals(expectedOrder, tracker.getCallbackOrder());
    }

    /**
     * 测试多次进度回调
     */
    @Test
    void testMultipleProgressCallbacks() {
        ProgressCapture capture = new ProgressCapture();

        for (int i = 1; i <= 10; i++) {
            capture.onProgress(i * 100, 1000);
        }

        assertEquals(10, capture.getCallCount());
        assertEquals(1000, capture.getLastCurrent());
    }

    /**
     * 测试异常处理 - onStart 抛异常
     */
    @Test
    void testOnStartThrowsException() {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onStart(long totalBytes) {
                throw new RuntimeException("Test exception in onStart");
            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
            }
        };

        assertThrows(RuntimeException.class, () -> listener.onStart(1024));
    }

    /**
     * 测试异常处理 - onProgress 抛异常
     */
    @Test
    void testOnProgressThrowsException() {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(long currentBytes, long totalBytes) {
                throw new RuntimeException("Test exception in onProgress");
            }
        };

        assertThrows(RuntimeException.class, () -> listener.onProgress(512, 1024));
    }

    /**
     * 测试异常处理 - onComplete 抛异常
     */
    @Test
    void testOnCompleteThrowsException() {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onComplete(long currentBytes, long totalBytes) {
                throw new RuntimeException("Test exception in onComplete");
            }

            @Override
            public void onProgress(long currentBytes, long totalBytes) {
            }
        };

        assertThrows(RuntimeException.class, () -> listener.onComplete(1024, 1024));
    }

    /**
     * 测试边界条件 - 零字节
     */
    @Test
    void testZeroBytes() {
        LifecycleTracker tracker = new LifecycleTracker();

        tracker.onStart(0);
        tracker.onProgress(0, 0);
        tracker.onComplete(0, 0);

        assertTrue(tracker.isStartCalled());
        assertTrue(tracker.isProgressCalled());
        assertTrue(tracker.isCompleteCalled());
    }

    /**
     * 测试边界条件 - 大数值
     */
    @Test
    void testLargeNumbers() {
        ProgressCapture capture = new ProgressCapture();

        long largeTotal = Long.MAX_VALUE;
        long largeCurrent = Long.MAX_VALUE / 2;

        capture.onProgress(largeCurrent, largeTotal);
        assertEquals(largeCurrent, capture.getLastCurrent());
        assertEquals(largeTotal, capture.getLastTotal());
    }

    /**
     * 测试与 ByteStreamCopier 集成
     */
    @Test
    void testIntegrationWithByteStreamCopier() {
        LifecycleTracker tracker = new LifecycleTracker();
        String testData = "Integration test data";

        ByteStreamCopier.builder()
                .setSource(testData)
                .setProgressListener(tracker)
                .build()
                .writeBytes();

        assertTrue(tracker.isStartCalled());
        assertTrue(tracker.isProgressCalled());
        assertTrue(tracker.isCompleteCalled());
    }

    /**
     * 测试只实现 onProgress 的最小监听器
     */
    @Test
    void testMinimalListener() {
        AtomicBoolean progressCalled = new AtomicBoolean(false);

        ProgressListener listener = (currentBytes, totalBytes) -> progressCalled.set(true);

        listener.onStart(1024); // 默认实现,不应抛异常
        listener.onProgress(512, 1024);
        listener.onComplete(1024, 1024); // 默认实现,不应抛异常

        assertTrue(progressCalled.get());
    }

    // 辅助类

    /**
     * 生命周期跟踪器
     */
    private static class LifecycleTracker implements ProgressListener {
        private boolean startCalled = false;
        private boolean progressCalled = false;
        private boolean completeCalled = false;
        private int progressCallCount = 0;

        @Override
        public void onStart(long totalBytes) {
            startCalled = true;
        }

        @Override
        public void onProgress(long currentBytes, long totalBytes) {
            progressCalled = true;
            progressCallCount++;
        }

        @Override
        public void onComplete(long currentBytes, long totalBytes) {
            completeCalled = true;
        }

        public boolean isStartCalled() {
            return startCalled;
        }

        public boolean isProgressCalled() {
            return progressCalled;
        }

        public boolean isCompleteCalled() {
            return completeCalled;
        }

        public int getProgressCallCount() {
            return progressCallCount;
        }
    }

    /**
     * 进度参数捕获器
     */
    private static class ProgressCapture implements ProgressListener {
        private long lastCurrent = -1;
        private long lastTotal = -1;
        private int callCount = 0;

        @Override
        public void onProgress(long currentBytes, long totalBytes) {
            lastCurrent = currentBytes;
            lastTotal = totalBytes;
            callCount++;
        }

        public long getLastCurrent() {
            return lastCurrent;
        }

        public long getLastTotal() {
            return lastTotal;
        }

        public int getCallCount() {
            return callCount;
        }
    }

    /**
     * 完成参数捕获器
     */
    private static class CompleteCapture implements ProgressListener {
        private long finalCurrent = -1;
        private long finalTotal = -1;

        @Override
        public void onProgress(long currentBytes, long totalBytes) {
        }

        @Override
        public void onComplete(long currentBytes, long totalBytes) {
            finalCurrent = currentBytes;
            finalTotal = totalBytes;
        }

        public long getFinalCurrent() {
            return finalCurrent;
        }

        public long getFinalTotal() {
            return finalTotal;
        }
    }

    /**
     * 回调顺序跟踪器
     */
    private static class CallbackOrderTracker implements ProgressListener {
        private final List<String> callbackOrder = new ArrayList<>();

        @Override
        public void onStart(long totalBytes) {
            callbackOrder.add("start");
        }

        @Override
        public void onProgress(long currentBytes, long totalBytes) {
            callbackOrder.add("progress");
        }

        @Override
        public void onComplete(long currentBytes, long totalBytes) {
            callbackOrder.add("complete");
        }

        public List<String> getCallbackOrder() {
            return new ArrayList<>(callbackOrder);
        }
    }
}
