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

/**
 * 进度监听器(三阶段:开始、进行中、完成)
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>回调方式为同步回调, 回调中避免耗时操作影响写入效率</li>
 *     <li>回调抛出异常会中断写入</li>
 * </ul>
 *
 * @author Toint
 * @see DataTransferUtil 数据传输工具类 - 字节大小格式化、百分比计算、时长格式化等
 * @since 2026/1/8
 */
public interface ProgressListener {

    /**
     * 开始 (在第一次读取数据之前)
     *
     * @param totalBytes 总字节数(-1 表示未知大小)
     * @throws RuntimeException 抛异常会中断执行
     */
    default void onStart(long totalBytes) {
        // 默认空实现
    }

    /**
     * 进行中 (每次读取数据后)
     *
     * <h2>注意事项</h2>
     * <ul>
     *     <li>每次循环读写 ({@link IoUtil#DEFAULT_BUFFER_SIZE}字节) 均会以同步的方式回调本方法, 所以需要自行控制回调内的执行频率, 避免影响写入性能</li>
     * </ul>
     *
     * @param currentBytes 合计已读取字节数
     * @param totalBytes   总字节数(-1 表示未知大小)
     * @throws RuntimeException 抛异常会中断执行
     */
    void onProgress(long currentBytes, long totalBytes);

    /**
     * 完成 (所有数据读取完毕后)
     *
     * @param currentBytes 合计已读取字节数
     * @param totalBytes   总字节数(-1 表示未知大小)
     * @throws RuntimeException 抛异常会中断执行
     */
    default void onComplete(long currentBytes, long totalBytes) {
        // 默认空实现
    }
}