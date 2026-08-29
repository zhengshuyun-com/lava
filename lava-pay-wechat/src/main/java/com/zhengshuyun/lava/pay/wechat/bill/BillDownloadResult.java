/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.bill;

import com.zhengshuyun.lava.core.lang.ValidationUtils;

import java.nio.file.Path;

/**
 * 已完成摘要校验并落盘的账单文件。
 *
 * @param path 最终文件路径
 * @param size 文件字节数
 * @param hashType 摘要类型
 * @param hashValue 实际摘要值
 */
public record BillDownloadResult(Path path, long size, String hashType, String hashValue) {
    /**
     * 校验下载结果。
     */
    public BillDownloadResult {
        ValidationUtils.requireNonNull(path, "path must not be null");
        ValidationUtils.requireTrue(size >= 0, "size must not be negative");
        ValidationUtils.requireNotBlank(hashType, "hashType must not be blank");
        ValidationUtils.requireNotBlank(hashValue, "hashValue must not be blank");
    }
}
