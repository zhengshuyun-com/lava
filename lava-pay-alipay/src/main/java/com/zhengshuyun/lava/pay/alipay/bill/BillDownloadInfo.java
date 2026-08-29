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
 * @param downloadUrl 下载地址；未返回时为 {@code null}，地址通常在获取后短时间内失效
 * @param fileCode    账单文件状态；未返回时为 {@code null}
 */
public record BillDownloadInfo(@Nullable URI downloadUrl, @Nullable String fileCode) {
}
