/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.bill;

import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * 已验签账单下载信息。
 *
 * @param downloadUrl 下载地址；未返回时为 {@code null}，获取后 30 秒内未下载即失效
 * @param fileCode    账单文件状态；未返回时为 {@code null}
 */
public record BillDownloadInfo(@Nullable URI downloadUrl, @Nullable String fileCode) {
    /**
     * 返回不包含账单下载令牌的安全摘要。
     *
     * @return 已脱敏文本
     */
    @Override
    public String toString() {
        return "BillDownloadInfo[downloadUrl="
                + (downloadUrl == null ? "null" : "[redacted]")
                + ", fileCode=" + fileCode + ']';
    }
}
