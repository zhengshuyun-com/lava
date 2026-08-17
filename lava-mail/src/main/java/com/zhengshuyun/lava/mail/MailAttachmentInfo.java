/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import org.jspecify.annotations.Nullable;

/**
 * 附件元数据，附件内容需要通过索引另行下载。
 *
 * <p>{@code reportedSizeBytes} 来自邮件服务端，可能表示传输编码后的大小；{@code -1} 表示未知。
 * 下载限制始终按实际解码后的字节流执行，不能用该字段代替安全检查。</p>
 *
 * @param index 附件在邮件 MIME 遍历顺序中的索引
 * @param fileName 服务端声明的文件名，不可直接作为本地路径
 * @param contentType 服务端声明的 MIME 内容类型
 * @param reportedSizeBytes 服务端报告的大小，未知时为 {@code -1}
 * @param contentId 去除首尾尖括号后的 Content-ID，没有时为 {@code null}
 */
public record MailAttachmentInfo(
        int index,
        String fileName,
        String contentType,
        long reportedSizeBytes,
        @Nullable String contentId) {
    /**
     * 校验附件元数据。
     *
     * @param index 附件索引
     * @param fileName 附件文件名
     * @param contentType MIME 内容类型
     * @param reportedSizeBytes 服务端报告的大小
     * @param contentId 可选 Content-ID
     */
    public MailAttachmentInfo {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        fileName = PasswordCredential.requireNonBlank(fileName, "fileName");
        contentType = PasswordCredential.requireNonBlank(contentType, "contentType");
        if (fileName.codePoints().anyMatch(Character::isISOControl)
                || contentType.codePoints().anyMatch(Character::isISOControl)
                || (contentId != null
                && contentId.codePoints().anyMatch(Character::isISOControl))) {
            throw new IllegalArgumentException("attachment metadata must not contain control characters");
        }
        if (reportedSizeBytes < -1) {
            throw new IllegalArgumentException(
                    "reportedSizeBytes must be -1 (unknown) or non-negative");
        }
    }
}
