/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.wechat.bill;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * 已验签的账单下载信息。
 *
 * @param hashType 文件摘要类型，当前固定为 {@code SHA1}
 * @param hashValue 账单原文的 40 位十六进制 SHA-1 摘要
 * @param downloadUrl 带敏感下载令牌且通常五分钟内有效的绝对地址
 * @param tarType 申请时使用的压缩类型；未指定时为 {@code null}
 */
public record BillDownloadInfo(
        String hashType,
        String hashValue,
        URI downloadUrl,
        @Nullable BillTarType tarType
) {
    /** 微信支付账单摘要的 40 位十六进制格式。 */
    private static final Pattern SHA1_VALUE = Pattern.compile("[0-9A-Fa-f]{40}");

    /**
     * 校验账单摘要和下载地址；地址允许 HTTP 仅为支持环回测试服务。
     *
     * @throws IllegalArgumentException 摘要不是 SHA-1，或下载地址不是长度不超过 2048
     *                                  字符的 HTTP/HTTPS 绝对地址时抛出
     */
    public BillDownloadInfo {
        ValidationUtils.requireTrue("SHA1".equalsIgnoreCase(hashType),
                "hashType must be SHA1");
        ValidationUtils.requireTrue(hashValue != null
                        && SHA1_VALUE.matcher(hashValue).matches(),
                "hashValue must be a 40-character SHA-1 value");
        ValidationUtils.requireNonNull(downloadUrl, "downloadUrl must not be null");
        ValidationUtils.requireTrue(downloadUrl.isAbsolute()
                        && ("https".equalsIgnoreCase(downloadUrl.getScheme())
                        || "http".equalsIgnoreCase(downloadUrl.getScheme())),
                "downloadUrl must be an absolute HTTP or HTTPS URI");
        ValidationUtils.requireTrue(downloadUrl.getHost() != null
                        && downloadUrl.getUserInfo() == null
                        && downloadUrl.getRawFragment() == null
                        && downloadUrl.toASCIIString().length() <= 2048,
                "downloadUrl must contain a host, omit user information and fragments, "
                        + "and not exceed 2048 characters");
    }

    /**
     * 返回不包含账单下载令牌的安全摘要。
     *
     * @return 已脱敏文本
     */
    @Override
    public String toString() {
        return "BillDownloadInfo[hashType=" + hashType
                + ", hashValue=[redacted], downloadUrl=[redacted], tarType="
                + tarType + ']';
    }
}
