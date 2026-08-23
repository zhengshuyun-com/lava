/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhengshuyun.lava.mail;

/**
 * 在保留或流式传输已解码 MIME 内容前执行的限制。
 *
 * <p>{@code maxDecodedBytesPerOperation} 分别限制一次
 * {@link MailReader#readMessage(MailMessageId)} 解码的全部正文、一次附件下载，或一次发信包含的
 * 正文与全部附件。多次独立的附件下载不共享累计状态，因此每次单独受限。</p>
 *
 * @param maxBodyBytes                单个正文部分允许的最大解码字节数
 * @param maxAttachmentBytes          单个附件允许的最大解码字节数
 * @param maxDecodedBytesPerOperation 单次操作允许的最大累计解码字节数
 * @param maxMimeDepth                允许的最大 MIME 嵌套深度
 */
public record MailLimits(
        long maxBodyBytes,
        long maxAttachmentBytes,
        long maxDecodedBytesPerOperation,
        int maxMimeDepth) {

    /**
     * 一个 MiB 对应的字节数。
     */
    public static final long MEBIBYTE = 1024L * 1024L;

    /**
     * 默认限制：正文 10 MiB、附件 25 MiB、单次累计 50 MiB、MIME 深度 20 层。
     */
    public static final MailLimits DEFAULT = new MailLimits(
            10L * MEBIBYTE,
            25L * MEBIBYTE,
            50L * MEBIBYTE,
            20);

    /**
     * 校验各项 MIME 限制及其组合关系。
     *
     * @param maxBodyBytes                单个正文最大字节数
     * @param maxAttachmentBytes          单个附件最大字节数
     * @param maxDecodedBytesPerOperation 单次操作最大累计解码字节数
     * @param maxMimeDepth                最大 MIME 嵌套深度
     */
    public MailLimits {
        requirePositive(maxBodyBytes, "maxBodyBytes");
        requirePositive(maxAttachmentBytes, "maxAttachmentBytes");
        requirePositive(maxDecodedBytesPerOperation, "maxDecodedBytesPerOperation");
        if (maxMimeDepth < 1) {
            throw new IllegalArgumentException("maxMimeDepth must be positive");
        }
        if (maxBodyBytes > maxDecodedBytesPerOperation) {
            throw new IllegalArgumentException("maxBodyBytes must not exceed maxDecodedBytesPerOperation");
        }
        if (maxAttachmentBytes > maxDecodedBytesPerOperation) {
            throw new IllegalArgumentException(
                    "maxAttachmentBytes must not exceed maxDecodedBytesPerOperation");
        }
    }

    private static void requirePositive(long value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
