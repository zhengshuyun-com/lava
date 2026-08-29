/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.zhengshuyun.lava.pay.alipay.bill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhengshuyun.lava.core.lang.ValidationUtils;
import com.zhengshuyun.lava.pay.alipay.exception.AlipayProtocolException;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayRuntime;
import com.zhengshuyun.lava.pay.alipay.internal.AlipayTransport;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 支付宝对账单下载地址查询客户端。
 */
public final class BillClient {
    private static final String METHOD = "alipay.data.dataservice.bill.downloadurl.query";

    private final AlipayRuntime runtime;

    /**
     * 由根客户端创建账单入口。
     *
     * @param runtime 共享运行时
     */
    public BillClient(AlipayRuntime runtime) {
        this.runtime = ValidationUtils.requireNonNull(runtime, "runtime");
    }

    /**
     * 查询日账单下载地址。
     *
     * @param billType 账单类型
     * @param date     账单日期
     * @return 已验签下载信息
     */
    public BillDownloadInfo queryDaily(String billType, LocalDate date) {
        return query(BillRequest.builder().billType(billType).date(date).build());
    }

    /**
     * 查询月账单下载地址。
     *
     * @param billType 账单类型
     * @param month    账单月份
     * @return 已验签下载信息
     */
    public BillDownloadInfo queryMonthly(String billType, YearMonth month) {
        return query(BillRequest.builder().billType(billType).month(month).build());
    }

    /**
     * 使用完整参数查询账单下载地址。
     *
     * @param request 查询参数
     * @return 已验签下载信息
     */
    public BillDownloadInfo query(BillRequest request) {
        AlipayTransport transport = runtime.transport();
        ValidationUtils.requireNonNull(request, "request must not be null");
        validateDate(transport, request);
        String billDate = request.date() == null
                ? request.month().toString() : request.date().toString();
        Payload response = transport.execute(METHOD,
                new RequestPayload(request.billType(), billDate), Payload.class);
        URI downloadUrl = parseUrl(response.downloadUrl);
        if (downloadUrl == null && response.fileCode == null) {
            throw new AlipayProtocolException(
                    "支付宝账单响应未返回下载地址或文件状态");
        }
        return new BillDownloadInfo(downloadUrl, response.fileCode);
    }

    private static void validateDate(AlipayTransport transport, BillRequest request) {
        LocalDate today = transport.currentDateTime().toLocalDate();
        if (request.date() != null) {
            ValidationUtils.requireTrue(request.date().isBefore(today),
                    "daily bill date must be before today");
            ValidationUtils.requireTrue(!request.date().isBefore(today.minusYears(6)),
                    "daily bill date must be within the last 6 years");
        } else {
            YearMonth current = YearMonth.from(today);
            ValidationUtils.requireTrue(request.month().isBefore(current),
                    "monthly bill month must be before the current month");
            ValidationUtils.requireTrue(!request.month().isBefore(current.minusYears(6)),
                    "monthly bill month must be within the last 6 years");
        }
    }

    private static @Nullable URI parseUrl(@Nullable String value) {
        if (value == null) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new AlipayProtocolException("支付宝账单下载地址格式无效");
        }
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getRawFragment() != null
                || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new AlipayProtocolException("支付宝账单下载地址格式无效");
        }
        return uri;
    }

    private record RequestPayload(
            @JsonProperty("bill_type") String billType,
            @JsonProperty("bill_date") String billDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Payload(
            @JsonProperty("bill_download_url") @Nullable String downloadUrl,
            @JsonProperty("bill_file_code") @Nullable String fileCode) {
    }
}
