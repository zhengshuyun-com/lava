/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.bill;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * 已验签的账单下载信息。
 *
 * @param hashType 文件摘要类型，当前固定为 SHA1
 * @param hashValue 期望文件摘要
 * @param downloadUrl 五分钟内有效的下载地址
 * @param tarType 申请时使用的压缩类型
 */
public record BillDownloadInfo(String hashType, String hashValue, URI downloadUrl,
                               @Nullable BillTarType tarType) {
    /**
     * 校验账单下载信息。
     */
    public BillDownloadInfo {
        ValidationUtils.requireNotBlank(hashType, "hashType must not be blank");
        ValidationUtils.requireNotBlank(hashValue, "hashValue must not be blank");
        ValidationUtils.requireNonNull(downloadUrl, "downloadUrl must not be null");
        ValidationUtils.requireTrue(downloadUrl.isAbsolute()
                        && ("https".equalsIgnoreCase(downloadUrl.getScheme())
                        || "http".equalsIgnoreCase(downloadUrl.getScheme())),
                "downloadUrl must be an absolute HTTP or HTTPS URI");
    }
}
